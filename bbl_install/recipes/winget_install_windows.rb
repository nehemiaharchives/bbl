require 'fileutils'
require 'json'
require 'tmpdir'

raise 'winget_install_windows recipe must run on Windows' unless platform_family?('windows')

cookbook_files = ::File.expand_path('../files', __dir__)
zip_source = ::File.join(cookbook_files, 'bbl-winget.zip')
manifest_source = ::File.join(cookbook_files, 'manifests')
version_source = ::File.join(cookbook_files, 'version.txt')

raise "Missing #{zip_source}" unless ::File.file?(zip_source)
raise "Missing #{manifest_source}" unless ::File.directory?(manifest_source)
raise "Missing #{version_source}" unless ::File.file?(version_source)

work_root = ::File.join(ENV['TEMP'] || ENV['TMP'] || ::Dir.tmpdir, 'bbl-winget-install')
zip_work = ::File.join(work_root, 'bbl-winget.zip')
manifest_work = ::File.join(work_root, 'manifests')
pid_file = ::File.join(work_root, 'server.pid')
port_file = ::File.join(work_root, 'port.txt')
attrs_file = ::File.join(work_root, 'bbl-winget-test-attributes.json')

ruby_block 'prepare bbl winget fixture' do
  block do
    ::FileUtils.rm_rf(work_root)
    ::FileUtils.mkdir_p(manifest_work)
    ::FileUtils.cp(zip_source, zip_work)
    ::Dir.children(manifest_source).each { |c| ::FileUtils.cp_r(::File.join(manifest_source, c), ::File.join(manifest_work, c)) }
  end
end

powershell_script 'start local http server and patch manifest' do
  code <<~'PS'
    $ErrorActionPreference = 'Stop'
    $wr = $env:TEMP + '\bbl-winget-install'
    $pf = $wr + '\port.txt'

    $script = @"
    param(`$Root, `$PortFile)
    `$ErrorActionPreference = 'Stop'
    `$l = New-Object System.Net.HttpListener
    `$port = 17851
    while (`$true) {
      try {
        `$l.Prefixes.Add("http://127.0.0.1:`$port/")
        `$l.Start()
        Set-Content `$PortFile `$port -Encoding ascii
        break
      } catch { `$port++; if (`$port -gt 17950) { throw } }
    }
    try {
      while (`$l.IsListening) {
        `$ctx = `$l.GetContext()
        `$p = Join-Path `$Root ([IO.Path]::GetFileName(`$ctx.Request.Url.AbsolutePath))
        if (Test-Path `$p) {
          `$ctx.Response.StatusCode = 200; `$ctx.Response.ContentType = 'application/zip'
          `$bytes = [IO.File]::ReadAllBytes(`$p)
        } else {
          `$ctx.Response.StatusCode = 404
          `$bytes = [Text.Encoding]::UTF8.GetBytes('not found')
        }
        `$ctx.Response.ContentLength64 = `$bytes.Length
        `$ctx.Response.OutputStream.Write(`$bytes, 0, `$bytes.Length)
        `$ctx.Response.OutputStream.Close()
      }
    } finally { `$l.Stop() }
"@

    $scriptPath = $wr + '\server.ps1'
    Set-Content $scriptPath $script -Encoding ascii
    $p = Start-Process powershell -WindowStyle Hidden -PassThru -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $scriptPath, '-Root', $wr, '-PortFile', $pf)
    Set-Content ($wr + '\server.pid') $p.Id -Encoding ascii

    for ($i = 0; $i -lt 100; $i++) { if (Test-Path $pf) { break }; Start-Sleep -Milliseconds 100 }
    if (-not (Test-Path $pf)) { throw 'Server did not start' }

    $port = Get-Content $pf
    $url = "http://127.0.0.1:$port/bbl-winget.zip"
    $installer = Get-ChildItem -Recurse $wr\manifests\Gnit.Bbl.installer.yaml | Select-Object -First 1 -ExpandProperty FullName
    (Get-Content $installer) -replace '__BBL_WINGET_INSTALLER_URL__', $url | Set-Content $installer
    $attrs = @{manifest_dir = $wr + '\manifests'; installer_url = $url; package_identifier = 'Gnit.Bbl'} | ConvertTo-Json
    Set-Content ($wr + '\bbl-winget-test-attributes.json') $attrs -Encoding ascii
  PS
end

powershell_script 'enable local manifests' do
  code 'winget settings --enable LocalManifestFiles'
end

powershell_script 'validate manifest' do
  code "winget validate --manifest '#{work_root.tr('/', '\\')}\\manifests' --disable-interactivity"
end

powershell_script 'uninstall existing' do
  code 'winget uninstall --id Gnit.Bbl --exact --disable-interactivity --silent --force'
  ignore_failure true
end

powershell_script 'winget install' do
  code "winget install --manifest '#{work_root.tr('/', '\\')}\\manifests' --disable-interactivity --accept-package-agreements --accept-source-agreements --force"
end

powershell_script 'smoke test' do
  code <<~PS
    bbl --version
    bbl john 3:16
    bbl search God limit 1
    winget list --id Gnit.Bbl --exact --disable-interactivity
  PS
end

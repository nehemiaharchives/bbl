describe 'bbl pack install and uninstall' do
  it 'downloads the platform-qualified common helper and installs it with its local name' do
    ps_quote = ->(value) { "'#{value.to_s.gsub("'", "''")}'" }
    bbl_cmd = ps_quote.call($bbl_bin)
    source_dir = ps_quote.call($bbl_install_source_dir)

    script = <<~POWERSHELL
      $ErrorActionPreference = 'Stop'
      $testHome = Join-Path ([System.IO.Path]::GetTempPath()) ('bbl-release-name-test.' + [System.Guid]::NewGuid().ToString('N'))
      $testRelease = Join-Path $testHome 'release'
      New-Item -ItemType Directory -Force $testRelease | Out-Null
      Copy-Item (Join-Path #{source_dir} 'webus.zip') (Join-Path $testRelease 'webus.zip')
      Copy-Item (Join-Path #{source_dir} 'bbl-search-common.exe') (Join-Path $testRelease 'bbl-search-common-windows-x64.exe')
      $env:USERPROFILE = $testHome
      $env:BBL_RELEASE_DOWNLOAD_URL = 'file://' + ($testRelease -replace '\\', '/')
      try {
        & #{bbl_cmd} install webus
        if ($LASTEXITCODE -ne 0) { throw "install exited with $LASTEXITCODE" }
        $localBinary = Join-Path $testHome '.bbl\bin\bbl-search-common.exe'
        $releaseBinary = Join-Path $testHome '.bbl\bin\bbl-search-common-windows-x64.exe'
        if (-not (Test-Path -LiteralPath $localBinary)) { throw "missing local helper $localBinary" }
        if (Test-Path -LiteralPath $releaseBinary) { throw "release asset was not renamed: $releaseBinary" }
        & #{bbl_cmd} uninstall webus
        if ($LASTEXITCODE -ne 0) { throw "uninstall exited with $LASTEXITCODE" }
        if (Test-Path -LiteralPath $localBinary) { throw "local helper was not uninstalled: $localBinary" }
      } finally {
        Remove-Item -LiteralPath $testHome -Recurse -Force -ErrorAction SilentlyContinue
      }
    POWERSHELL

    result = powershell(script)
    expect(result.exit_status).to eq(0), result.stderr
    expect(result.stdout).to include("Installed bbl-search-common.exe#{$bbl_eol}")
    expect(result.stdout).to include("Uninstalled bbl-search-common.exe#{$bbl_eol}")
  end

  it 'uninstalls and reinstalls kjv' do
    ps_quote = ->(value) { "'#{value.to_s.gsub("'", "''")}'" }
    bbl_cmd = ps_quote.call($bbl_bin)
    pack_dir = ps_quote.call($bbl_pack_dir)
    release_download_url = ps_quote.call("file://#{$bbl_install_source_dir}")

    # Debug: check environment and packs directory in powershell resource
    env_check = powershell("$env:USERPROFILE = $env:USERPROFILE; Write-Output \"USERPROFILE=$env:USERPROFILE\"; Write-Output \"Packs dir: $(Get-ChildItem -LiteralPath #{pack_dir} -Name)\"; & #{bbl_cmd} list translations")
    puts "ENV CHECK: #{env_check.stdout}"
    puts "ENV CHECK STDERR: #{env_check.stderr}" if env_check.respond_to?(:stderr)

    # Use powershell resource with -Command and explicit USERPROFILE
    uninstall = powershell("$env:USERPROFILE = $env:USERPROFILE; & #{bbl_cmd} uninstall kjv")
    puts "UNINSTALL EXIT STATUS: #{uninstall.exit_status}"
    puts "UNINSTALL STDOUT: #{uninstall.stdout}"
    puts "UNINSTALL STDERR: #{uninstall.stderr}" if uninstall.respond_to?(:stderr)
    expect(uninstall.exit_status).to eq(0)
    expect(uninstall.stdout).to include("Uninstalled kjv#{$bbl_eol}")

    after_uninstall_list = powershell("$env:USERPROFILE = $env:USERPROFILE; & #{bbl_cmd} list translations")
    expect(after_uninstall_list.exit_status).to eq(0)
    kjv_available = after_uninstall_list.stdout.lines.map(&:strip).find { |line| line.start_with?('KJV ') }
    expect(kjv_available).not_to be_nil
    expect(kjv_available).to match(/\|\s+Available\s+\|/)

    # Write install script to temp file and execute with powershell -Command
    script_content = "$env:BBL_RELEASE_DOWNLOAD_URL = #{release_download_url}; $env:USERPROFILE = $env:USERPROFILE; & #{bbl_cmd} install kjv"
    install = powershell(script_content)
    puts "INSTALL EXIT STATUS: #{install.exit_status}"
    puts "INSTALL STDOUT: #{install.stdout}"
    puts "INSTALL STDERR: #{install.stderr}" if install.respond_to?(:stderr)
    expect(install.exit_status).to eq(0)
    expect(install.stdout).to include("Installed kjv#{$bbl_eol}")

    # Wait a moment and run list translations with debug
    sleep 2
    after_install_list = powershell("$env:USERPROFILE = $env:USERPROFILE; Write-Output \"Packs dir after install: $(Get-ChildItem -LiteralPath #{pack_dir} -Name)\"; & #{bbl_cmd} list translations")
    puts "AFTER INSTALL LIST STDOUT: #{after_install_list.stdout}"
    puts "AFTER INSTALL LIST EXIT STATUS: #{after_install_list.exit_status}"
    expect(after_install_list.exit_status).to eq(0)
    kjv_installed = after_install_list.stdout.lines.map(&:strip).find { |line| line.start_with?('KJV ') }
    expect(kjv_installed).not_to be_nil
    expect(kjv_installed).to match(/\|\s+Installed\s+\|/)
  end
end

require 'fileutils'
require 'tmpdir'

raise 'msi_install_windows recipe must run on Windows' unless platform_family?('windows')

cookbook_files = ::File.expand_path('../files', __dir__)
msi_source = ::File.join(cookbook_files, 'bbl.msi')
raise "Missing #{msi_source}" unless ::File.file?(msi_source)

msi_work = ::File.join(ENV['TEMP'] || ENV['TMP'] || ::Dir.tmpdir, 'bbl-msi-install', 'bbl.msi')

ruby_block 'copy bbl msi to temp' do
  block do
    ::FileUtils.mkdir_p(::File.dirname(msi_work))
    ::FileUtils.cp(msi_source, msi_work)
  end
end

bbl_dir = "#{ENV['LOCALAPPDATA']}\\bbl"
bbl_packs_dir = "#{ENV['USERPROFILE']}\\.bbl\\packs"
bbl_bin_dir = "#{ENV['USERPROFILE']}\\.bbl\\bin"

powershell_script 'install bbl via msiexec' do
  code <<~PS
    $ErrorActionPreference = 'Stop'
    $proc = Start-Process msiexec -ArgumentList @('/i', '#{msi_work.tr('/', '\\')}', '/qn', '/norestart', '/log', "$env:TEMP\\bbl-msi-install.log") -Wait -NoNewWindow -PassThru
    if ($proc.ExitCode -ne 0) {
      Get-Content "$env:TEMP\\bbl-msi-install.log" -Tail 20
      throw "msiexec exited with code $($proc.ExitCode)"
    }
  PS
end

ruby_block 'deploy webus pack and search helper to .bll dirs' do
  block do
    webus_src = ::File.join(bbl_dir, 'webus.zip')
    webus_dst = ::File.join(bbl_packs_dir, 'webus.zip')
    helper_src = ::File.join(bbl_dir, 'bbl-search-common.exe')
    helper_dst = ::File.join(bbl_bin_dir, 'bbl-search-common.exe')

    ::FileUtils.mkdir_p(bbl_packs_dir)
    ::FileUtils.mkdir_p(bbl_bin_dir)
    ::FileUtils.cp(webus_src, webus_dst) if ::File.file?(webus_src)
    ::FileUtils.cp(helper_src, helper_dst) if ::File.file?(helper_src)
  end
end

powershell_script 'smoke test bbl msi install' do
  code <<~PS
    $ErrorActionPreference = 'Stop'
    & "$env:LOCALAPPDATA\\bbl\\bbl.exe" --version
    & "$env:LOCALAPPDATA\\bbl\\bbl.exe" john 3:16
    & "$env:LOCALAPPDATA\\bbl\\bbl.exe" search God limit 1
  PS
end

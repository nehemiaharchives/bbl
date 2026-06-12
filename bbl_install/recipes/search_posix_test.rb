require 'etc'

return if platform_family?('windows')

test_script_path = node['bbl_install']['search_posix_test_script_path']
return unless test_script_path

macos = platform_family?('mac_os_x')
system_owner = macos ? (ENV['SUDO_USER'] || ENV['USER'] || Etc.getlogin) : 'root'
install_group = macos ? Etc.getgrgid(Etc.getpwnam(system_owner).gid).name : 'root'

directory ::File.dirname(test_script_path) do
  owner system_owner
  group install_group
  mode '0755'
  recursive true
end

cookbook_file test_script_path do
  source 'search_posix_test.sh'
  owner system_owner
  group install_group
  mode '0755'
end

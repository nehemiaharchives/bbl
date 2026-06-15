require 'etc'

return if platform_family?('windows')

test_script_path = node['bbl_install']['config_posix_test_script_path']
return unless test_script_path

local_owner = ENV['SUDO_USER'] || ENV['USER'] || Etc.getlogin
local_group = Etc.getgrgid(Etc.getpwnam(local_owner).gid).name if local_owner
system_owner = node['bbl_install']['system_user'] || local_owner || 'root'
system_group = node['bbl_install']['system_group'] || local_group || 'root'
test_command_user = node['bbl_install']['test_command_user']
test_command_home = node['bbl_install']['test_command_home']

shell_quote = ->(value) { "'#{value.to_s.gsub("'", "'\"'\"'")}'" }

directory ::File.dirname(test_script_path) do
  owner system_owner
  group system_group
  mode '0755'
  recursive true
end

if test_command_user
  source_script_path = "#{test_script_path}.source"

  cookbook_file source_script_path do
    source 'config_posix_test.sh'
    owner system_owner
    group system_group
    mode '0755'
  end

  file test_script_path do
    content <<~BASH
      #!/usr/bin/env bash
      set -euo pipefail
      exec runuser -u #{shell_quote.call(test_command_user)} -- env HOME=#{shell_quote.call(test_command_home)} #{shell_quote.call(source_script_path)} "$@"
    BASH
    owner system_owner
    group system_group
    mode '0755'
  end
else
  cookbook_file test_script_path do
    source 'config_posix_test.sh'
    owner system_owner
    group system_group
    mode '0755'
  end
end

require 'etc'
require 'fileutils'

windows = platform_family?('windows')
macos = platform_family?('mac_os_x')

# Include Windows-specific recipe for Windows
include_recipe 'bbl_install::install_windows' if windows
return if windows

# POSIX (Linux/macOS) specific installation
install_root = node['bbl_install']['install_root']
bin_dir = node['bbl_install']['bin_dir']
helper_bin_dir = node['bbl_install']['helper_bin_dir'] || bin_dir
pack_dir = node['bbl_install']['pack_dir']
version_file_path = node['bbl_install']['version_file_path']
install_source_dir = node['bbl_install']['install_source_dir']
bbl_bin_path = node['bbl_install']['bbl_binary_path']

posix_owner = macos ? (ENV['SUDO_USER'] || ENV['USER'] || Etc.getlogin) : 'root'
posix_group = macos ? Etc.getgrgid(Etc.getpwnam(posix_owner).gid).name : 'root'
system_group = macos ? 'wheel' : 'root'
system_owner = macos ? posix_owner : 'root'
install_group = macos ? posix_group : system_group

directory install_root do
  owner posix_owner
  group posix_group
  mode '0755'
end

directory bin_dir do
  owner posix_owner
  group posix_group
  mode '0755'
end

if helper_bin_dir != bin_dir
  directory helper_bin_dir do
    owner posix_owner
    group posix_group
    mode '0755'
  end
end

directory pack_dir do
  owner posix_owner
  group posix_group
  mode '0755'
end

cookbook_file version_file_path do
  source 'version.txt'
  owner posix_owner
  group posix_group
  mode '0644'
end

if install_source_dir
  directory install_source_dir do
    owner system_owner
    group install_group
    mode '0755'
  end
end

cookbook_file bbl_bin_path do
  source node['bbl_install']['bbl_binary_name']
  owner system_owner
  group install_group
  mode '0755'
end

node['bbl_install']['helper_bin_names'].each do |bin_name|
  target_path = ::File.join(helper_bin_dir, bin_name)
  cookbook_file target_path do
    source bin_name
    owner posix_owner
    group posix_group
    mode '0755'
  end
end

node['bbl_install']['pack_names'].each do |pack_name|
  target_path = ::File.join(pack_dir, pack_name)
  cookbook_file target_path do
    source pack_name
    owner posix_owner
    group posix_group
    mode '0644'
  end
end

node['bbl_install']['deferred_helper_bin_names'].each do |bin_name|
  cookbook_file ::File.join(install_source_dir, bin_name) do
    source bin_name
    owner system_owner
    group install_group
    mode '0755'
  end
end

node['bbl_install']['deferred_pack_names'].each do |pack_name|
  cookbook_file ::File.join(install_source_dir, pack_name) do
    source pack_name
    owner system_owner
    group install_group
    mode '0644'
  end
end

test_attrs_path = '/tmp/bbl-test-attributes.json'

ruby_block 'save bbl_install attributes for InSpec tests' do
  block do
    require 'json'
    ::File.write(test_attrs_path, JSON.pretty_generate(node['bbl_install'].to_hash))
  end
end
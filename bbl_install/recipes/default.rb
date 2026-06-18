require 'etc'
require 'fileutils'

windows = platform_family?('windows')

# Include Windows-specific recipe for Windows
include_recipe 'bbl_install::install_windows' if windows
return if windows

# POSIX (Linux/macOS) specific installation
install_root = node['bbl_install']['install_root']
bin_dir = node['bbl_install']['bin_dir']
helper_bin_dir = node['bbl_install']['helper_bin_dir'] || bin_dir
pack_dir = node['bbl_install']['pack_dir']
completion_dir = node['bbl_install']['completion_dir']
version_file_path = node['bbl_install']['version_file_path']
install_source_dir = node['bbl_install']['install_source_dir']
bbl_bin_path = node['bbl_install']['bbl_binary_path']

local_owner = ENV['SUDO_USER'] || ENV['USER'] || Etc.getlogin
local_group = Etc.getgrgid(Etc.getpwnam(local_owner).gid).name if local_owner
install_user = node['bbl_install']['install_user']
install_home = node['bbl_install']['home_dir'] || ENV['HOME']
install_owner = install_user || local_owner || 'root'
install_group = node['bbl_install']['install_group'] || local_group || 'root'
system_owner = node['bbl_install']['system_user'] || local_owner || 'root'
system_group = node['bbl_install']['system_group'] || install_group
manage_install_user = node['bbl_install']['manage_install_user']

user install_owner do
  home install_home
  manage_home false
  only_if { manage_install_user && !install_owner.to_s.empty? }
end

directory install_home do
  owner install_owner
  group install_group
  mode '0755'
  only_if { manage_install_user && install_home && !install_home.to_s.empty? }
end

directory install_root do
  owner install_owner
  group install_group
  mode '0755'
end

directory bin_dir do
  owner install_owner
  group install_group
  mode '0755'
end

if helper_bin_dir != bin_dir
  directory helper_bin_dir do
    owner install_owner
    group install_group
    mode '0755'
  end
end

directory pack_dir do
  owner install_owner
  group install_group
  mode '0755'
end

directory completion_dir do
  owner install_owner
  group install_group
  mode '0755'
end

if node['bbl_install']['clean_pack_dir']
  ruby_block "clean #{pack_dir}" do
    block do
      if ::Dir.exist?(pack_dir)
        ::Dir.children(pack_dir).each do |entry|
          ::FileUtils.rm_rf(::File.join(pack_dir, entry))
        end
      end
    end
  end
end

cookbook_file version_file_path do
  source 'version.txt'
  owner install_owner
  group install_group
  mode '0644'
end

if install_source_dir
  directory install_source_dir do
    owner system_owner
    group system_group
    mode '0755'
  end

  if node['bbl_install']['clean_install_source_dir']
    ruby_block "clean #{install_source_dir}" do
      block do
        if ::Dir.exist?(install_source_dir)
          ::Dir.children(install_source_dir).each do |entry|
            ::FileUtils.rm_rf(::File.join(install_source_dir, entry))
          end
        end
      end
    end
  end
end

cookbook_file bbl_bin_path do
  source node['bbl_install']['bbl_binary_name']
  owner system_owner
  group system_group
  mode '0755'
end

node['bbl_install']['helper_bin_names'].each do |bin_name|
  target_path = ::File.join(helper_bin_dir, bin_name)
  cookbook_file target_path do
    source bin_name
    owner install_owner
    group install_group
    mode '0755'
  end
end

node['bbl_install']['pack_names'].each do |pack_name|
  target_path = ::File.join(pack_dir, pack_name)
  cookbook_file target_path do
    source pack_name
    owner install_owner
    group install_group
    mode '0644'
  end
end

%w[bbl.bash _bbl bbl.fish].each do |completion_file|
  cookbook_file ::File.join(completion_dir, completion_file) do
    source completion_file
    owner install_owner
    group install_group
    mode '0644'
  end
end

node['bbl_install']['deferred_helper_bin_names'].each do |bin_name|
  cookbook_file ::File.join(install_source_dir, bin_name) do
    source bin_name
    owner system_owner
    group system_group
    mode '0755'
  end
end

node['bbl_install']['deferred_pack_names'].each do |pack_name|
  cookbook_file ::File.join(install_source_dir, pack_name) do
    source pack_name
    owner system_owner
    group system_group
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

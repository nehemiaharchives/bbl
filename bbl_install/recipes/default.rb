require 'etc'
require 'fileutils'

install_root = node['bbl_install']['install_root']
bin_dir = node['bbl_install']['bin_dir']
helper_bin_dir = node['bbl_install']['helper_bin_dir'] || bin_dir
pack_dir = node['bbl_install']['pack_dir']
version_file_path = node['bbl_install']['version_file_path']
artifact_compatibility_version_file_path = node['bbl_install']['artifact_compatibility_version_file_path']
install_source_dir = node['bbl_install']['install_source_dir']
bbl_bin_path = node['bbl_install']['bbl_binary_path']
windows = platform_family?('windows')
macos = platform_family?('mac_os_x')
posix_owner = macos ? (ENV['SUDO_USER'] || ENV['USER'] || Etc.getlogin) : 'root'
posix_group = macos ? Etc.getgrgid(Etc.getpwnam(posix_owner).gid).name : 'root'
system_group = macos ? 'wheel' : 'root'
system_owner = macos ? posix_owner : 'root'
install_group = macos ? posix_group : system_group

def install_windows_cookbook_file(path, source_name)
  ruby_block "copy #{source_name} to #{path}" do
    block do
      source_path = ::File.join(::File.expand_path('../files', __dir__), source_name)
      ::FileUtils.mkdir_p(::File.dirname(path))
      ::FileUtils.cp(source_path, path)
    end
  end
end

directory install_root do
  recursive windows
  owner posix_owner unless windows
  group posix_group unless windows
  mode '0755' unless windows
end

directory bin_dir do
  recursive windows
  owner posix_owner unless windows
  group posix_group unless windows
  mode '0755' unless windows
end

if helper_bin_dir != bin_dir
  directory helper_bin_dir do
    recursive windows
    owner posix_owner unless windows
    group posix_group unless windows
    mode '0755' unless windows
  end
end

directory pack_dir do
  recursive windows
  owner posix_owner unless windows
  group posix_group unless windows
  mode '0755' unless windows
end

if windows
  install_windows_cookbook_file(version_file_path, 'version.txt')
  install_windows_cookbook_file(artifact_compatibility_version_file_path, 'artifact_compatibility_version.txt')
else
  cookbook_file version_file_path do
    source 'version.txt'
    owner posix_owner
    group posix_group
    mode '0644'
  end

  cookbook_file artifact_compatibility_version_file_path do
    source 'artifact_compatibility_version.txt'
    owner posix_owner
    group posix_group
    mode '0644'
  end
end

if install_source_dir
  directory install_source_dir do
    owner system_owner
    group install_group
    mode '0755'
  end
end

if windows
  install_windows_cookbook_file(bbl_bin_path, node['bbl_install']['bbl_binary_name'])
else
  cookbook_file bbl_bin_path do
    source node['bbl_install']['bbl_binary_name']
    owner system_owner
    group install_group
    mode '0755'
  end
end

node['bbl_install']['helper_bin_names'].each do |bin_name|
  target_path = ::File.join(helper_bin_dir, bin_name)
  if windows
    install_windows_cookbook_file(target_path, bin_name)
  else
    cookbook_file target_path do
      source bin_name
      owner posix_owner
      group posix_group
      mode '0755'
    end
  end
end

node['bbl_install']['pack_names'].each do |pack_name|
  target_path = ::File.join(pack_dir, pack_name)
  if windows
    install_windows_cookbook_file(target_path, pack_name)
  else
    cookbook_file target_path do
      source pack_name
      owner posix_owner
      group posix_group
      mode '0644'
    end
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

if windows
  ruby_block 'add bbl install directories to user PATH' do
    block do
      install_path_entries = [bin_dir, helper_bin_dir].map { |path| path.tr('/', '\\') }.uniq
      ::Win32::Registry::HKEY_CURRENT_USER.open('Environment', ::Win32::Registry::KEY_READ | ::Win32::Registry::KEY_WRITE) do |registry|
        path_value = begin
          registry['Path']
        rescue ::Win32::Registry::Error
          ''
        end

        path_entries = path_value.split(';').reject(&:empty?)
        missing_entries = install_path_entries.reject do |install_path_entry|
          path_entries.any? { |entry| entry.casecmp?(install_path_entry) }
        end
        next if missing_entries.empty?

        registry.write_s('Path', (missing_entries + path_entries).join(';'))
        ENV['Path'] = (missing_entries + ENV.fetch('Path', '').split(';').reject(&:empty?)).uniq.join(';')
      end
    end
  end
end

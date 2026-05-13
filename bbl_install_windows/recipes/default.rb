require 'digest'
require 'fileutils'
require 'win32/registry'

local_app_data = ENV['LOCALAPPDATA']
local_app_data ||= ::File.join(ENV.fetch('USERPROFILE'), 'AppData', 'Local')

install_root = ::File.join(local_app_data, '.bbl')
bin_dir = ::File.join(install_root, 'bin')
pack_dir = ::File.join(install_root, 'packs')
bbl_bin_path = ::File.join(bin_dir, 'bbl.exe')
current_cookbook = cookbook_name

copy_cookbook_file = lambda do |target_path, source_name|
  ruby_block "copy #{source_name} to #{target_path}" do
    block do
      source_path = run_context
        .cookbook_collection[current_cookbook]
        .preferred_filename_on_disk_location(node, :files, source_name)

      next if ::File.exist?(target_path) &&
              ::File.size(target_path) == ::File.size(source_path) &&
              Digest::SHA256.file(target_path).hexdigest == Digest::SHA256.file(source_path).hexdigest

      FileUtils.mkdir_p(::File.dirname(target_path))
      FileUtils.cp(source_path, target_path)
    end
  end
end

directory install_root do
  recursive true
end

directory bin_dir do
  recursive true
end

directory pack_dir do
  recursive true
end

copy_cookbook_file.call(bbl_bin_path, 'bbl.exe')

node['bbl_install_windows']['helper_bin_names'].each do |bin_name|
  copy_cookbook_file.call(::File.join(bin_dir, bin_name), bin_name)
end

node['bbl_install_windows']['pack_names'].each do |pack_name|
  copy_cookbook_file.call(::File.join(pack_dir, pack_name), pack_name)
end

ruby_block "add #{bin_dir} to user PATH" do
  block do
    normalized_bin_dir = bin_dir.tr('/', '\\')
    ::Win32::Registry::HKEY_CURRENT_USER.open('Environment', ::Win32::Registry::KEY_READ | ::Win32::Registry::KEY_WRITE) do |registry|
      path_value = begin
        registry['Path']
      rescue ::Win32::Registry::Error
        ''
      end

      path_entries = path_value.split(';').reject(&:empty?)
      next if path_entries.any? { |entry| entry.casecmp?(normalized_bin_dir) }

      registry.write_s('Path', ([normalized_bin_dir] + path_entries).join(';'))
      ENV['Path'] = ([normalized_bin_dir] + ENV.fetch('Path', '').split(';').reject(&:empty?)).uniq.join(';')
    end
  end
end

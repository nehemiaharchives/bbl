require 'fileutils'

def install_windows_cookbook_file(path, source_name)
  ruby_block "copy #{source_name} to #{path}" do
    block do
      source_path = ::File.join(::File.expand_path('../files', __dir__), source_name)
      ::FileUtils.mkdir_p(::File.dirname(path))
      ::FileUtils.cp(source_path, path)
    end
  end
end

install_root = node['bbl_install']['install_root']
bin_dir = node['bbl_install']['bin_dir']
helper_bin_dir = node['bbl_install']['helper_bin_dir'] || bin_dir
pack_dir = node['bbl_install']['pack_dir']
version_file_path = node['bbl_install']['version_file_path']
install_source_dir = node['bbl_install']['install_source_dir']
bbl_bin_path = node['bbl_install']['bbl_binary_path']

directory install_root do
  recursive true
end

directory bin_dir do
  recursive true
end

if helper_bin_dir != bin_dir
  directory helper_bin_dir do
    recursive true
  end
end

directory pack_dir do
  recursive true
end

install_windows_cookbook_file(version_file_path, 'version.txt')
install_windows_cookbook_file(bbl_bin_path, node['bbl_install']['bbl_binary_name'])

node['bbl_install']['helper_bin_names'].each do |bin_name|
  target_path = ::File.join(helper_bin_dir, bin_name)
  install_windows_cookbook_file(target_path, bin_name)
end

node['bbl_install']['pack_names'].each do |pack_name|
  target_path = ::File.join(pack_dir, pack_name)
  install_windows_cookbook_file(target_path, pack_name)
end

if install_source_dir
  directory install_source_dir do
    recursive true
  end
end

node['bbl_install']['deferred_helper_bin_names'].each do |bin_name|
  ruby_block "copy #{bin_name} to #{install_source_dir}" do
    block do
      source_path = ::File.join(::File.expand_path('../files', __dir__), bin_name)
      target_path = ::File.join(install_source_dir, bin_name)
      ::FileUtils.mkdir_p(::File.dirname(target_path))
      ::FileUtils.cp(source_path, target_path)
    end
  end
end

node['bbl_install']['deferred_pack_names'].each do |pack_name|
  ruby_block "copy #{pack_name} to #{install_source_dir}" do
    block do
      source_path = ::File.join(::File.expand_path('../files', __dir__), pack_name)
      target_path = ::File.join(install_source_dir, pack_name)
      ::FileUtils.mkdir_p(::File.dirname(target_path))
      ::FileUtils.cp(source_path, target_path)
    end
  end
end

test_attrs_path = "#{ENV['TEMP']}\\bbl-test-attributes.json"

ruby_block 'save bbl_install attributes for InSpec tests' do
  block do
    require 'json'
    ::File.write(test_attrs_path, JSON.pretty_generate(node['bbl_install'].to_hash))
  end
end

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
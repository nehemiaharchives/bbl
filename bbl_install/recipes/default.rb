install_root = node['bbl_install']['install_root']
bin_dir = node['bbl_install']['bin_dir']
pack_dir = node['bbl_install']['pack_dir']
install_source_dir = node['bbl_install']['install_source_dir']
bbl_bin_path = node['bbl_install']['bbl_binary_path']
windows = platform_family?('windows')

directory install_root do
  recursive windows
  owner 'root' unless windows
  group 'root' unless windows
  mode '0755' unless windows
end

directory bin_dir do
  recursive windows
  owner 'root' unless windows
  group 'root' unless windows
  mode '0755' unless windows
end

directory pack_dir do
  recursive windows
  owner 'root' unless windows
  group 'root' unless windows
  mode '0755' unless windows
end

if install_source_dir
  directory install_source_dir do
    owner 'root'
    group 'root'
    mode '0755'
  end
end

cookbook_file bbl_bin_path do
  source node['bbl_install']['bbl_binary_name']
  owner 'root' unless windows
  group 'root' unless windows
  mode '0755' unless windows
end

node['bbl_install']['helper_bin_names'].each do |bin_name|
  cookbook_file ::File.join(bin_dir, bin_name) do
    source bin_name
    owner 'root' unless windows
    group 'root' unless windows
    mode '0755' unless windows
  end
end

node['bbl_install']['pack_names'].each do |pack_name|
  cookbook_file ::File.join(pack_dir, pack_name) do
    source pack_name
    owner 'root' unless windows
    group 'root' unless windows
    mode '0644' unless windows
  end
end

node['bbl_install']['deferred_helper_bin_names'].each do |bin_name|
  cookbook_file ::File.join(install_source_dir, bin_name) do
    source bin_name
    owner 'root'
    group 'root'
    mode '0755'
  end
end

node['bbl_install']['deferred_pack_names'].each do |pack_name|
  cookbook_file ::File.join(install_source_dir, pack_name) do
    source pack_name
    owner 'root'
    group 'root'
    mode '0644'
  end
end

if windows
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
end

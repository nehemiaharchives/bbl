install_root = '/root/.bbl'
bin_dir = "#{install_root}/bin"
pack_dir = "#{install_root}/packs"
install_source_dir = '/tmp/bbl-install-downloads'
bbl_bin_path = '/usr/local/bin/bbl'

directory install_root do
  owner 'root'
  group 'root'
  mode '0755'
end

directory bin_dir do
  owner 'root'
  group 'root'
  mode '0755'
end

directory pack_dir do
  owner 'root'
  group 'root'
  mode '0755'
end

directory install_source_dir do
  owner 'root'
  group 'root'
  mode '0755'
end

cookbook_file bbl_bin_path do
  source 'bbl'
  owner 'root'
  group 'root'
  mode '0755'
end

node['bbl_install_linux']['helper_bin_names'].each do |bin_name|
  cookbook_file "#{bin_dir}/#{bin_name}" do
    source bin_name
    owner 'root'
    group 'root'
    mode '0755'
  end
end

node['bbl_install_linux']['pack_names'].each do |pack_name|
  cookbook_file "#{pack_dir}/#{pack_name}" do
    source pack_name
    owner 'root'
    group 'root'
    mode '0644'
  end
end

node['bbl_install_linux']['deferred_helper_bin_names'].each do |bin_name|
  cookbook_file "#{install_source_dir}/#{bin_name}" do
    source bin_name
    owner 'root'
    group 'root'
    mode '0755'
  end
end

node['bbl_install_linux']['deferred_pack_names'].each do |pack_name|
  cookbook_file "#{install_source_dir}/#{pack_name}" do
    source pack_name
    owner 'root'
    group 'root'
    mode '0644'
  end
end

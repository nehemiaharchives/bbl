pack_names = ::Dir.glob(::File.expand_path('../files/*.zip', __dir__)).map { |path| ::File.basename(path) }.sort

# Linux defaults. macOS and Windows set their paths in platform-specific
# attribute files.

unless platform_family?('windows') || platform_family?('mac_os_x')
  default['bbl_install']['install_user'] = 'ubuntu'
  default['bbl_install']['install_group'] = 'ubuntu'
  default['bbl_install']['system_user'] = 'root'
  default['bbl_install']['system_group'] = 'root'
  default['bbl_install']['manage_install_user'] = true
  default['bbl_install']['test_command_user'] = 'ubuntu'
  default['bbl_install']['test_command_home'] = '/home/ubuntu'
  default['bbl_install']['home_dir'] = '/home/ubuntu'
  default['bbl_install']['install_root'] = '/home/ubuntu/.bbl'
  default['bbl_install']['bin_dir'] = '/home/ubuntu/.bbl/bin'
  default['bbl_install']['helper_bin_dir'] = '/home/ubuntu/.bbl/bin'
  default['bbl_install']['pack_dir'] = '/home/ubuntu/.bbl/packs'
  default['bbl_install']['completion_dir'] = '/home/ubuntu/.bbl/completions'
  default['bbl_install']['version_file_path'] = '/home/ubuntu/.bbl/version.txt'
  default['bbl_install']['bbl_binary_path'] = '/usr/bin/bbl'
  default['bbl_install']['install_source_dir'] = '/tmp/bbl-install-downloads'
end

default['bbl_install']['bbl_binary_name'] = 'bbl'
default['bbl_install']['helper_bin_names'] = %w[
  bbl-search-common
  bbl-search-extra
  bbl-search-kuromoji
  bbl-search-morfologik
  bbl-search-nori
  bbl-search-smartcn
]
default['bbl_install']['deferred_helper_bin_names'] = []
default['bbl_install']['pack_names'] = pack_names
default['bbl_install']['deferred_pack_names'] = []
default['bbl_install']['clean_pack_dir'] = false
default['bbl_install']['clean_install_source_dir'] = false

default['bbl_install']['stat_size_command'] = 'stat -c %s'
default['bbl_install']['zip_manifest_method'] = 'ruby_zip'

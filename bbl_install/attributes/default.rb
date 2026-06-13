pack_names = ::Dir.glob(::File.expand_path('../files/*.zip', __dir__)).map { |path| ::File.basename(path) }.sort

# the default.rb expects attributes for Linux

default['bbl_install']['install_root'] = '/root/.bbl'
default['bbl_install']['bin_dir'] = '/root/.bbl/bin'
default['bbl_install']['helper_bin_dir'] = '/root/.bbl/bin'
default['bbl_install']['pack_dir'] = '/root/.bbl/packs'
default['bbl_install']['version_file_path'] = '/root/.bbl/version.txt'
default['bbl_install']['bbl_binary_path'] = '/usr/bin/bbl'
default['bbl_install']['install_source_dir'] = '/tmp/bbl-install-downloads'

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

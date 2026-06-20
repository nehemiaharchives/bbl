if platform_family?('alpine')
  default['bbl_install']['install_user'] = 'alpine'
  default['bbl_install']['install_group'] = 'alpine'
  default['bbl_install']['system_user'] = 'root'
  default['bbl_install']['system_group'] = 'root'
  default['bbl_install']['manage_install_user'] = true
  default['bbl_install']['test_command_user'] = 'alpine'
  default['bbl_install']['test_command_home'] = '/home/alpine'
  default['bbl_install']['home_dir'] = '/home/alpine'
  default['bbl_install']['install_root'] = '/home/alpine/.bbl'
  default['bbl_install']['bin_dir'] = '/home/alpine/.bbl/bin'
  default['bbl_install']['helper_bin_dir'] = '/home/alpine/.bbl/bin'
  default['bbl_install']['pack_dir'] = '/home/alpine/.bbl/packs'
  default['bbl_install']['completion_dir'] = '/home/alpine/.bbl/completions'
  default['bbl_install']['version_file_path'] = '/home/alpine/.bbl/version.txt'
  default['bbl_install']['bbl_binary_path'] = '/usr/bin/bbl'
end

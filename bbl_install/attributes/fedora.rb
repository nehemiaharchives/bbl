if platform_family?('fedora')
  default['bbl_install']['install_user'] = 'fedora'
  default['bbl_install']['install_group'] = 'fedora'
  default['bbl_install']['system_user'] = 'root'
  default['bbl_install']['system_group'] = 'root'
  default['bbl_install']['manage_install_user'] = true
  default['bbl_install']['test_command_user'] = 'fedora'
  default['bbl_install']['test_command_home'] = '/home/fedora'
  default['bbl_install']['home_dir'] = '/home/fedora'
  default['bbl_install']['install_root'] = '/home/fedora/.bbl'
  default['bbl_install']['bin_dir'] = '/home/fedora/.bbl/bin'
  default['bbl_install']['helper_bin_dir'] = '/home/fedora/.bbl/bin'
  default['bbl_install']['pack_dir'] = '/home/fedora/.bbl/packs'
  default['bbl_install']['completion_dir'] = '/home/fedora/.bbl/completions'
  default['bbl_install']['version_file_path'] = '/home/fedora/.bbl/version.txt'
  default['bbl_install']['bbl_binary_path'] = '/usr/bin/bbl'
end

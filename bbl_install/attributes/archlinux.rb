if platform_family?('arch')
  default['bbl_install']['install_user'] = 'arch'
  default['bbl_install']['install_group'] = 'arch'
  default['bbl_install']['system_user'] = 'root'
  default['bbl_install']['system_group'] = 'root'
  default['bbl_install']['manage_install_user'] = true
  default['bbl_install']['test_command_user'] = 'arch'
  default['bbl_install']['test_command_home'] = '/home/arch'
  default['bbl_install']['home_dir'] = '/home/arch'
  default['bbl_install']['install_root'] = '/home/arch/.bbl'
  default['bbl_install']['bin_dir'] = '/home/arch/.bbl/bin'
  default['bbl_install']['helper_bin_dir'] = '/home/arch/.bbl/bin'
  default['bbl_install']['pack_dir'] = '/home/arch/.bbl/packs'
  default['bbl_install']['completion_dir'] = '/home/arch/.bbl/completions'
  default['bbl_install']['version_file_path'] = '/home/arch/.bbl/version.txt'
  default['bbl_install']['bbl_binary_path'] = '/usr/bin/bbl'
end

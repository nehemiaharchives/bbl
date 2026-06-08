if platform_family?('windows')
  user_profile = ENV['USERPROFILE'] || '/root'
  local_app_data = ENV['LOCALAPPDATA'] || ::File.join(user_profile, 'AppData', 'Local')
  install_root = ::File.join(user_profile, '.bbl')
  bin_dir = ::File.join(local_app_data, 'Programs', 'bbl')
  helper_bin_dir = ::File.join(install_root, 'bin')

  default['bbl_install']['install_root'] = install_root
  default['bbl_install']['bin_dir'] = bin_dir
  default['bbl_install']['helper_bin_dir'] = helper_bin_dir
  default['bbl_install']['pack_dir'] = ::File.join(install_root, 'packs')
  default['bbl_install']['version_file_path'] = ::File.join(install_root, 'version.txt')
  default['bbl_install']['bbl_binary_path'] = ::File.join(bin_dir, 'bbl.exe')
  default['bbl_install']['bbl_binary_name'] = 'bbl.exe'
  default['bbl_install']['install_source_dir'] = nil

  default['bbl_install']['helper_bin_names'] = %w[
    bbl-search-common.exe
    bbl-search-extra.exe
    bbl-search-kuromoji.exe
    bbl-search-morfologik.exe
    bbl-search-nori.exe
    bbl-search-smartcn.exe
  ]

  default['bbl_install']['stat_size_command'] = nil
  default['bbl_install']['zip_manifest_method'] = 'powershell'
end

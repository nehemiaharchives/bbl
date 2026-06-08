if platform_family?('mac_os_x')
  require 'etc'

  home_dir = if ENV['SUDO_USER']
               Etc.getpwnam(ENV['SUDO_USER']).dir
             else
               ENV['HOME'] || '/root'
             end
  install_root = ::File.join(home_dir, '.bbl')
  bin_dir = ::File.join(install_root, 'bin')

  default['bbl_install']['install_root'] = install_root
  default['bbl_install']['bin_dir'] = bin_dir
  default['bbl_install']['helper_bin_dir'] = bin_dir
  default['bbl_install']['pack_dir'] = ::File.join(install_root, 'packs')
  default['bbl_install']['version_file_path'] = ::File.join(install_root, 'version.txt')
  default['bbl_install']['bbl_binary_path'] = ::File.join(bin_dir, 'bbl')
  default['bbl_install']['bbl_binary_name'] = 'bbl'
  default['bbl_install']['stat_size_command'] = 'stat -f %z'
end

require 'etc'

platform_family = node['platform_family']
windows = platform_family == 'windows'
macos = platform_family == 'mac_os_x'

user_profile = ENV['USERPROFILE'] || '/root'
home_dir = if macos && ENV['SUDO_USER']
             Etc.getpwnam(ENV['SUDO_USER']).dir
           else
             ENV['HOME'] || '/root'
           end
local_app_data = windows ? (ENV['LOCALAPPDATA'] || ::File.join(user_profile, 'AppData', 'Local')) : '/root'
install_root = if windows
                 ::File.join(user_profile, '.bbl')
               elsif macos
                 ::File.join(home_dir, '.bbl')
               else
                 '/root/.bbl'
               end
bin_dir = if windows
            ::File.join(local_app_data, 'Programs', 'bbl')
          else
            ::File.join(install_root, 'bin')
          end
helper_bin_dir = if windows
                   ::File.join(install_root, 'bin')
                 else
                   bin_dir
                 end
pack_dir = ::File.join(install_root, 'packs')

default['bbl_install']['install_root'] = install_root
default['bbl_install']['bin_dir'] = bin_dir
default['bbl_install']['helper_bin_dir'] = helper_bin_dir
default['bbl_install']['pack_dir'] = pack_dir
default['bbl_install']['bbl_binary_path'] = if windows
                                             ::File.join(bin_dir, 'bbl.exe')
                                           elsif macos
                                             ::File.join(bin_dir, 'bbl')
                                           else
                                             '/usr/bin/bbl'
                                           end
default['bbl_install']['install_source_dir'] = windows ? nil : '/tmp/bbl-install-downloads'
default['bbl_install']['bbl_binary_name'] = windows ? 'bbl.exe' : 'bbl'

if windows
  default['bbl_install']['helper_bin_names'] = %w[
    bbl-search-common.exe
    bbl-search-extra.exe
    bbl-search-kuromoji.exe
    bbl-search-morfologik.exe
    bbl-search-nori.exe
    bbl-search-smartcn.exe
  ]

  default['bbl_install']['deferred_helper_bin_names'] = []
  default['bbl_install']['pack_names'] = %w[
    cunp.zip
    webus.zip
    kjv.zip
    jc.zip
    krv.zip
    kttv.zip
    ubg.zip
  ]
  default['bbl_install']['deferred_pack_names'] = []
else
  default['bbl_install']['helper_bin_names'] = %w[
    bbl-search-common
    bbl-search-extra
    bbl-search-morfologik
    bbl-search-nori
    bbl-search-smartcn
  ]

  default['bbl_install']['deferred_helper_bin_names'] = %w[
    bbl-search-kuromoji
  ]

  default['bbl_install']['pack_names'] = %w[
    cunp.zip
    webus.zip
    kjv.zip
    krv.zip
    kttv.zip
    ubg.zip
  ]

  default['bbl_install']['deferred_pack_names'] = %w[
    jc.zip
  ]
end

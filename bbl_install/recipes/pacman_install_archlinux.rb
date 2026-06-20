package_path = '/tmp/bbl.pkg.tar.zst'
install_user = node['bbl_install']['install_user'] || 'arch'
install_group = node['bbl_install']['install_group'] || install_user
install_home = node['bbl_install']['home_dir'] || "/home/#{install_user}"

group install_group do
  only_if { install_group && !install_group.empty? }
end

user install_user do
  gid install_group
  home install_home
  shell '/bin/bash'
  manage_home true
  only_if { install_user && !install_user.empty? }
end

cookbook_file package_path do
  source 'bbl.pkg.tar.zst'
  mode '0644'
end

execute 'remove existing bbl pacman package and stale files' do
  command <<~SH
    set -e
    pacman -Q bbl >/dev/null 2>&1 && pacman -Rns --noconfirm bbl || true
    rm -f /usr/bin/bbl
    rm -f #{install_home}/.bbl/bin/bbl-search-common
    rm -f #{install_home}/.bbl/webus.zip
    rm -f #{install_home}/.bbl/packs/webus.zip
  SH
end

execute 'install bbl Arch Linux pacman package' do
  command "pacman -U --noconfirm #{package_path}"
end

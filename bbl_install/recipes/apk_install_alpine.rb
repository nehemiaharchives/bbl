apk_path = '/tmp/bbl.apk'
install_user = node['bbl_install']['install_user'] || 'alpine'
install_group = node['bbl_install']['install_group'] || install_user
install_home = node['bbl_install']['home_dir'] || "/home/#{install_user}"

group install_group do
  only_if { install_group && !install_group.empty? }
end

user install_user do
  gid install_group
  home install_home
  shell '/bin/sh'
  manage_home true
  only_if { install_user && !install_user.empty? }
end

cookbook_file apk_path do
  source 'bbl.apk'
  mode '0644'
end

execute 'remove existing bbl apk package and stale files' do
  command <<~SH
    set -e
    apk del bbl >/dev/null 2>&1 || true
    rm -f /usr/bin/bbl
    rm -f #{install_home}/.bbl/bin/bbl-search-common
    rm -f #{install_home}/.bbl/webus.zip
    rm -f #{install_home}/.bbl/packs/webus.zip
  SH
end

execute 'install bbl Alpine APK package' do
  command <<~SH
    set -e
    apk update
    apk add --no-cache --allow-untrusted #{apk_path}
  SH
end

execute 'diagnose installed bbl binary on Alpine' do
  command <<~SH
    set -e
    file /usr/bin/bbl || true
    ldd /usr/bin/bbl || true
    readelf -l /usr/bin/bbl | grep -i interpreter || true
    /usr/bin/bbl --version
  SH
end

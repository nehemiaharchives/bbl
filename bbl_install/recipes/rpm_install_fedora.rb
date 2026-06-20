rpm_path = '/tmp/bbl.rpm'
install_user = node['bbl_install']['install_user'] || 'fedora'
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

cookbook_file rpm_path do
  source 'bbl.rpm'
  mode '0644'
end

execute 'remove existing bbl rpm package before test' do
  command <<~SH
    set -e
    rpm -q bbl >/dev/null 2>&1 && rpm -e bbl || true
    rm -f /usr/bin/bbl
    rm -f #{install_home}/.bbl/bin/bbl-search-common
    rm -f #{install_home}/.bbl/webus.zip
    rm -f #{install_home}/.bbl/packs/webus.zip
  SH
end

execute 'install bbl rpm package with dnf' do
  command <<~SH
    set -e
    dnf install -y #{rpm_path}
  SH
end

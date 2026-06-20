deb_path = '/tmp/bbl.deb'

cookbook_file deb_path do
  source 'bbl.deb'
  mode '0644'
end

execute 'remove existing bbl deb package before test' do
  command <<~SH
    set -e
    dpkg -s bbl >/dev/null 2>&1 && dpkg -r bbl || true
    rm -f /usr/bin/bbl
    rm -f /home/ubuntu/.bbl/bin/bbl-search-common
    rm -f /home/ubuntu/.bbl/webus.zip
    rm -f /home/ubuntu/.bbl/packs/webus.zip
  SH
end

execute 'install bbl deb package' do
  command <<~SH
    set -e
    dpkg -i #{deb_path}
  SH
end

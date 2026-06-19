pkg_path = '/tmp/bbl.pkg'

cookbook_file pkg_path do
  source 'bbl.pkg'
  mode '0644'
end

execute 'install bbl macOS package' do
  command <<~SH
    if sudo -n true >/dev/null 2>&1; then
      sudo -n rm -f /usr/local/bin/bbl
      sudo -n installer -pkg #{pkg_path} -target /
    else
      osascript -e 'do shell script "rm -f /usr/local/bin/bbl; installer -pkg /tmp/bbl.pkg -target /" with administrator privileges'
    fi
  SH
end

#!/bin/sh
set -eu

target_user="${SUDO_USER:-${USER:-}}"
if [ -z "$target_user" ] || [ "$target_user" = "root" ]; then
  target_home="${HOME:-/root}"
else
  target_home="$(eval printf '%s' "~$target_user")"
fi

install_root="$target_home/.bbl"
install_source_dir="/tmp/bbl-install-downloads"

has_bbl=false
has_receipt=false
[ -e /usr/local/bin/bbl ] && has_bbl=true
pkgutil --pkg-info org.gnit.bbl >/dev/null 2>&1 && has_receipt=true

if [ "$has_bbl" = true ] || [ "$has_receipt" = true ]; then
  if sudo -n true >/dev/null 2>&1; then
    sudo -n rm -f /usr/local/bin/bbl
    sudo -n pkgutil --forget org.gnit.bbl >/dev/null 2>&1 || true
  else
    osascript -e 'do shell script "rm -f /usr/local/bin/bbl; pkgutil --forget org.gnit.bbl >/dev/null 2>&1 || true" with administrator privileges'
  fi
fi

[ "$has_bbl" = true ] && echo "Removed /usr/local/bin/bbl" || echo "/usr/local/bin/bbl does not exist"
[ "$has_receipt" = true ] && echo "Forgot package receipt org.gnit.bbl" || echo "Package receipt org.gnit.bbl does not exist"

rm -f /tmp/bbl.pkg

if [ -d "$install_root" ]; then
  rm -rf "$install_root"
  echo "Removed $install_root"
else
  echo "$install_root does not exist"
fi

if [ -d "$install_source_dir" ]; then
  rm -rf "$install_source_dir"
  echo "Removed $install_source_dir"
else
  echo "$install_source_dir does not exist"
fi

#!/bin/sh
set -eu

target_user="${SUDO_USER:-${USER:-}}"
if [ -z "$target_user" ] || [ "$target_user" = "root" ]; then
  target_home="${HOME:-/root}"
else
  target_home="$(eval printf '%s' "~$target_user")"
fi

install_root="$target_home/.bbl"
bbl_binary="/usr/local/bin/bbl"
install_source_dir="/tmp/bbl-install-downloads"

if [ -e "$bbl_binary" ] || [ -L "$bbl_binary" ]; then
  rm -f "$bbl_binary"
  echo "Removed $bbl_binary"
else
  echo "$bbl_binary does not exist"
fi

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

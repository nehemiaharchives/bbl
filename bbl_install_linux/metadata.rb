# frozen_string_literal: true

name             'bbl_install_linux'
maintainer       'Hokuto Joel Ide'
maintainer_email 'nehemiaharchive@gmail.com'
license          'Apache 2'
description      'installs native bbl fixture binaries and packs'
long_description 'installs native bbl fixture binaries and packs'
version          '0.3.0'

supports 'ubuntu'

# We use 13 chef but it should be compatible with 12.7
chef_version '>= 13'

# Provided recipes
recipe 'bbl_install_linux::default', 'Installs native bbl binary, helper binaries, and pack zips'

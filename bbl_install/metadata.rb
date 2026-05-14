# frozen_string_literal: true

name             'bbl_install'
maintainer       'Hokuto Joel Ide'
maintainer_email 'nehemiaharchive@gmail.com'
license          'Apache 2'
description      'installs native bbl fixture binaries and packs'
long_description 'installs native bbl fixture binaries and packs'
version          '0.3.0'

supports 'ubuntu'
supports 'windows'

# We pin Chef 18 because the latest Chef release is not stable on Windows in this repo.
chef_version '~> 18.10'

# Provided recipes
recipe 'bbl_install::default', 'Installs native bbl binary, helper binaries, and pack zips'

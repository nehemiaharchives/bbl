# frozen_string_literal: true

name             'bbl_install'
maintainer       'Hokuto Joel Ide'
maintainer_email 'nehemiaharchive@gmail.com'
license          'MIT'
description      'installs bbl'
long_description 'installs bbl'
version          '0.3.0'

supports 'ubuntu'

# We use 13 chef but it should be compatible with 12.7
chef_version '>= 13'

# Provided recipes
recipe 'bbl_install::default', 'Installs bbl package'

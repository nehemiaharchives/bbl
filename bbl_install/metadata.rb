# frozen_string_literal: true

name             'bbl_install'
maintainer       'Hokuto Joel Ide'
maintainer_email 'nehemiaharchive@gmail.com'
license          'MIT'
description      'installs bbl'
long_description 'installs bbl'
version          '0.3.0'

supports 'ubuntu'

#depends 'apt'

# We use 13 chef but it should be compatible with 12.7
chef_version '>= 13'

# Provided recipes
recipe 'mongodb-lib::mongo_gem', 'Installs mongo gem in compile time. Required for cluster operations'

# Automatically installed gems before chef run (new in 12.8)
# gem 'mongo', '>= 2.4.0'
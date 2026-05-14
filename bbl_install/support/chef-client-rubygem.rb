#!/usr/bin/env ruby

require "chef/application/client"
require "chef/resources"
require "chef/providers"

Chef::Application::Client.new.run

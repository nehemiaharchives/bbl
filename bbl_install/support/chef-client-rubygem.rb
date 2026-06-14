#!/usr/bin/env ruby

require "chef/application/client"
require "chef/resources"
require "chef/providers"

if Gem.win_platform?
  require "chef/http/authenticator"

  # Local Kitchen runs use the generated PEM file; avoid Chef's Windows cert-store auth path.
  class Chef::HTTP::Authenticator
    def self.retrieve_certificate_key(_client_name)
      false
    end
  end
end

Chef::Application::Client.new.run

return if platform_family?('windows') || platform_family?('mac_os_x')

package %w[zsh fish] do
  action :install
end

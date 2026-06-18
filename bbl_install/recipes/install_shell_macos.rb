return unless platform_family?('mac_os_x')

# zsh is installed with macOS. Install fish for the completion load check when
# Homebrew is available on the local/CI runner.
execute 'install fish with Homebrew' do
  command 'brew install fish'
  environment(
    'PATH' => '/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin'
  )
  only_if 'command -v brew >/dev/null 2>&1'
  not_if 'command -v fish >/dev/null 2>&1'
end

fixture_dir = '/tmp/bbl-homebrew-fixture'
formula_dir = "#{fixture_dir}/Formula"
formula_path = "#{formula_dir}/bbl.rb"
archive_path = "#{fixture_dir}/bbl.tar.gz"
tap = 'bbl-kmp-e2e/local'

directory formula_dir do
  recursive true
end

cookbook_file formula_path do
  source 'Formula/bbl.rb'
  mode '0644'
end

cookbook_file archive_path do
  source 'bbl.tar.gz'
  mode '0644'
end

ruby_block 'patch Homebrew formula archive path' do
  block do
    formula = ::File.read(formula_path)
    ::File.write(formula_path, formula.gsub('__BBL_HOMEBREW_ARCHIVE__', archive_path))
  end
end

execute 'uninstall bbl before Homebrew formula test' do
  command "brew uninstall --force bbl || true; brew untap --force #{tap} || true"
end

execute 'create local Homebrew formula test tap' do
  command <<~SH
    brew tap-new #{tap}
    cp #{formula_path} "$(brew --repository #{tap})/Formula/bbl.rb"
  SH
end

execute 'install bbl from local Homebrew formula test tap' do
  command "HOMEBREW_NO_AUTO_UPDATE=1 brew install --formula #{tap}/bbl"
end

execute 'test bbl Homebrew formula' do
  command "brew test #{tap}/bbl"
end

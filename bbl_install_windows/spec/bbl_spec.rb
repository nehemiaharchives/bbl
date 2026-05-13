require 'chefspec'

describe 'bbl_install_windows::default' do
  let(:local_app_data) { 'C:\\Users\\runneradmin\\AppData\\Local' }
  let(:install_root) { ::File.join(local_app_data, '.bbl') }
  let(:bin_dir) { ::File.join(install_root, 'bin') }
  let(:pack_dir) { ::File.join(install_root, 'packs') }

  let(:helper_bin_names) do
    %w[
      bbl-search-common.exe
      bbl-search-extra.exe
      bbl-search-kuromoji.exe
      bbl-search-morfologik.exe
      bbl-search-nori.exe
      bbl-search-smartcn.exe
    ]
  end

  let(:pack_names) do
    %w[
      cunp.zip
      webus.zip
      kjv.zip
      jc.zip
      krv.zip
      kttv.zip
      ubg.zip
    ]
  end

  let(:chef_run) do
    stub_const('ENV', ENV.to_hash.merge('LOCALAPPDATA' => local_app_data))

    ChefSpec::SoloRunner.new(platform: 'windows', version: '2022') do |node|
      node.normal['bbl_install_windows']['helper_bin_names'] = helper_bin_names
      node.normal['bbl_install_windows']['pack_names'] = pack_names
    end.converge(described_recipe)
  end

  context 'installs fixture binaries and packs' do
    it 'creates the native bin and pack directories' do
      expect(chef_run).to create_directory(install_root).with(recursive: true)
      expect(chef_run).to create_directory(bin_dir).with(recursive: true)
      expect(chef_run).to create_directory(pack_dir).with(recursive: true)
    end

    it 'copies the native bbl binary' do
      expect(chef_run).to run_ruby_block("copy bbl.exe to #{::File.join(bin_dir, 'bbl.exe')}")
    end

    it 'copies the search helpers' do
      helper_bin_names.each do |bin_name|
        expect(chef_run).to run_ruby_block("copy #{bin_name} to #{::File.join(bin_dir, bin_name)}")
      end
    end

    it 'copies the pack fixture zips' do
      pack_names.each do |pack_name|
        expect(chef_run).to run_ruby_block("copy #{pack_name} to #{::File.join(pack_dir, pack_name)}")
      end
    end

    it 'adds the bbl bin directory to PATH' do
      expect(chef_run).to run_ruby_block("add #{bin_dir} to user PATH")
    end
  end
end

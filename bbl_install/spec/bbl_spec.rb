require 'chefspec'

describe 'bbl_install::default' do
  let(:helper_bin_names) do
    %w[
      bbl-search-common
      bbl-search-extra
      bbl-search-kuromoji
      bbl-search-morfologik
      bbl-search-nori
      bbl-search-smartcn
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
    ChefSpec::SoloRunner.new(platform: 'ubuntu', version: '24.04') do |node|
      node.normal['bbl_install']['helper_bin_names'] = helper_bin_names
      node.normal['bbl_install']['pack_names'] = pack_names
    end.converge(described_recipe)
  end

  context 'installs fixture binaries and packs' do
    it 'creates the native bin and pack directories' do
      expect(chef_run).to create_directory('/root/.bbl/bin').with(
        owner: 'root',
        group: 'root',
        mode: '0755'
      )
      expect(chef_run).to create_directory('/root/.bbl/packs').with(
        owner: 'root',
        group: 'root',
        mode: '0755'
      )
    end

    it 'copies the native bbl binary' do
      expect(chef_run).to create_cookbook_file('/usr/local/bin/bbl').with(
        source: 'bbl',
        owner: 'root',
        group: 'root',
        mode: '0755'
      )
    end

    it 'copies the search helpers' do
      helper_bin_names.each do |bin_name|
        expect(chef_run).to create_cookbook_file("/root/.bbl/bin/#{bin_name}").with(
          source: bin_name,
          owner: 'root',
          group: 'root',
          mode: '0755'
        )
      end
    end

    it 'copies the pack fixture zips' do
      pack_names.each do |pack_name|
        expect(chef_run).to create_cookbook_file("/root/.bbl/packs/#{pack_name}").with(
          source: pack_name,
          owner: 'root',
          group: 'root',
          mode: '0644'
        )
      end
    end
  end
end

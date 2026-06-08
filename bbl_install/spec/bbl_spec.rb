require 'chefspec'

describe 'bbl_install::default' do
  let(:linux_helper_bin_names) do
    %w[
      bbl-search-common
      bbl-search-extra
      bbl-search-kuromoji
      bbl-search-morfologik
      bbl-search-nori
      bbl-search-smartcn
    ]
  end

  let(:linux_deferred_helper_bin_names) { [] }

  let(:linux_pack_names) do
    %w[
      abtag.zip
      ayt.zip
      cunp.zip
      delut.zip
      irvben.zip
      irvguj.zip
      irvhin.zip
      irvmar.zip
      irvtam.zip
      irvtel.zip
      irvurd.zip
      jc.zip
      kjv.zip
      krv.zip
      kttv.zip
      lsg.zip
      npiulb.zip
      rdv24.zip
      rvr09.zip
      sinod.zip
      sven.zip
      svrj.zip
      tb.zip
      th1971.zip
      ubg.zip
      ubio.zip
      webus.zip
    ]
  end

  let(:linux_deferred_pack_names) { [] }

  context 'on linux' do
    let(:chef_run) do
      ChefSpec::SoloRunner.new(platform: 'ubuntu', version: '24.04') do |node|
        node.normal['bbl_install']['helper_bin_names'] = linux_helper_bin_names
        node.normal['bbl_install']['deferred_helper_bin_names'] = linux_deferred_helper_bin_names
        node.normal['bbl_install']['pack_names'] = linux_pack_names
        node.normal['bbl_install']['deferred_pack_names'] = linux_deferred_pack_names
      end.converge(described_recipe)
    end

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
      expect(chef_run).to create_directory('/tmp/bbl-install-downloads').with(
        owner: 'root',
        group: 'root',
        mode: '0755'
      )
    end

    it 'copies the native bbl binary' do
      expect(chef_run).to create_cookbook_file('/usr/bin/bbl').with(
        source: 'bbl',
        owner: 'root',
        group: 'root',
        mode: '0755'
      )
    end

    it 'copies the expected bbl version file' do
      expect(chef_run).to create_cookbook_file('/root/.bbl/version.txt').with(
        source: 'version.txt',
        owner: 'root',
        group: 'root',
        mode: '0644'
      )
    end

    it 'copies the search helpers' do
      linux_helper_bin_names.each do |bin_name|
        expect(chef_run).to create_cookbook_file("/root/.bbl/bin/#{bin_name}").with(
          source: bin_name,
          owner: 'root',
          group: 'root',
          mode: '0755'
        )
      end
    end

    it 'copies deferred search helpers to the install source directory' do
      linux_deferred_helper_bin_names.each do |bin_name|
        expect(chef_run).to create_cookbook_file("/tmp/bbl-install-downloads/#{bin_name}").with(
          source: bin_name,
          owner: 'root',
          group: 'root',
          mode: '0755'
        )
      end
    end

    it 'copies the pack fixture zips' do
      linux_pack_names.each do |pack_name|
        expect(chef_run).to create_cookbook_file("/root/.bbl/packs/#{pack_name}").with(
          source: pack_name,
          owner: 'root',
          group: 'root',
          mode: '0644'
        )
      end
    end

    it 'copies deferred pack fixture zips to the install source directory' do
      linux_deferred_pack_names.each do |pack_name|
        expect(chef_run).to create_cookbook_file("/tmp/bbl-install-downloads/#{pack_name}").with(
          source: pack_name,
          owner: 'root',
          group: 'root',
          mode: '0644'
        )
      end
    end
  end

  context 'on windows' do
    let(:local_app_data) { 'C:\\Users\\runneradmin\\AppData\\Local' }
    let(:user_profile) { 'C:\\Users\\runneradmin' }
    let(:install_root) { ::File.join(user_profile, '.bbl') }
    let(:bin_dir) { ::File.join(local_app_data, 'Programs', 'bbl') }
    let(:helper_bin_dir) { ::File.join(install_root, 'bin') }
    let(:pack_dir) { ::File.join(install_root, 'packs') }

    let(:windows_helper_bin_names) do
      %w[
        bbl-search-common.exe
        bbl-search-extra.exe
        bbl-search-kuromoji.exe
        bbl-search-morfologik.exe
        bbl-search-nori.exe
        bbl-search-smartcn.exe
      ]
    end

    let(:windows_pack_names) do
      %w[
        abtag.zip
        ayt.zip
        cunp.zip
        delut.zip
        irvben.zip
        irvguj.zip
        irvhin.zip
        irvmar.zip
        irvtam.zip
        irvtel.zip
        irvurd.zip
        kjv.zip
        jc.zip
        krv.zip
        kttv.zip
        lsg.zip
        npiulb.zip
        rdv24.zip
        rvr09.zip
        sinod.zip
        sven.zip
        svrj.zip
        tb.zip
        th1971.zip
        ubg.zip
        ubio.zip
        webus.zip
      ]
    end

    let(:chef_run) do
      stub_const('ENV', ENV.to_hash.merge('LOCALAPPDATA' => local_app_data, 'USERPROFILE' => user_profile))

      ChefSpec::SoloRunner.new(platform: 'windows', version: '2022') do |node|
        node.normal['bbl_install']['helper_bin_names'] = windows_helper_bin_names
        node.normal['bbl_install']['pack_names'] = windows_pack_names
        node.normal['bbl_install']['install_root'] = install_root
        node.normal['bbl_install']['bin_dir'] = bin_dir
        node.normal['bbl_install']['helper_bin_dir'] = helper_bin_dir
        node.normal['bbl_install']['pack_dir'] = pack_dir
        node.normal['bbl_install']['version_file_path'] = ::File.join(install_root, 'version.txt')
        node.normal['bbl_install']['bbl_binary_path'] = ::File.join(bin_dir, 'bbl.exe')
        node.normal['bbl_install']['bbl_binary_name'] = 'bbl.exe'
      end.converge(described_recipe)
    end

    it 'creates the native bin and pack directories' do
      expect(chef_run).to create_directory(install_root).with(recursive: true)
      expect(chef_run).to create_directory(bin_dir).with(recursive: true)
      expect(chef_run).to create_directory(helper_bin_dir).with(recursive: true)
      expect(chef_run).to create_directory(pack_dir).with(recursive: true)
    end

    it 'copies the native bbl binary' do
      expect(chef_run).to run_ruby_block("copy bbl.exe to #{::File.join(bin_dir, 'bbl.exe')}")
    end

    it 'copies the expected bbl version file' do
      expect(chef_run).to run_ruby_block("copy version.txt to #{::File.join(install_root, 'version.txt')}")
    end

    it 'copies the search helpers' do
      windows_helper_bin_names.each do |bin_name|
        expect(chef_run).to run_ruby_block("copy #{bin_name} to #{::File.join(helper_bin_dir, bin_name)}")
      end
    end

    it 'copies the pack fixture zips' do
      windows_pack_names.each do |pack_name|
        expect(chef_run).to run_ruby_block("copy #{pack_name} to #{::File.join(pack_dir, pack_name)}")
      end
    end

    it 'adds the bbl bin directory to PATH' do
      expect(chef_run).to run_ruby_block('add bbl install directories to user PATH')
    end
  end

  context 'on macos' do
    let(:home_dir) { '/Users/runner' }
    let(:install_root) { ::File.join(home_dir, '.bbl') }
    let(:bin_dir) { ::File.join(install_root, 'bin') }
    let(:pack_dir) { ::File.join(install_root, 'packs') }
    let(:install_source_dir) { '/tmp/bbl-install-downloads' }
    let(:current_user) { 'runner' }
    let(:current_group) { 'staff' }

    let(:chef_run) do
      stub_const('ENV', ENV.to_hash.merge('HOME' => '/var/root', 'SUDO_USER' => current_user, 'USER' => 'root'))
      allow(Etc).to receive(:getpwnam).with(current_user).and_return(double(dir: home_dir, gid: 20))
      allow(Etc).to receive(:getgrgid).with(20).and_return(double(name: current_group))

      ChefSpec::SoloRunner.new(platform: 'mac_os_x', version: '12') do |node|
        node.normal['bbl_install']['helper_bin_names'] = linux_helper_bin_names
        node.normal['bbl_install']['deferred_helper_bin_names'] = linux_deferred_helper_bin_names
        node.normal['bbl_install']['pack_names'] = linux_pack_names
        node.normal['bbl_install']['deferred_pack_names'] = linux_deferred_pack_names
      end.converge(described_recipe)
    end

    it 'creates the native bin and pack directories under the local home directory' do
      expect(chef_run).to create_directory(bin_dir).with(
        owner: current_user,
        group: current_group,
        mode: '0755'
      )
      expect(chef_run).to create_directory(pack_dir).with(
        owner: current_user,
        group: current_group,
        mode: '0755'
      )
      expect(chef_run).to create_directory(install_source_dir).with(
        owner: current_user,
        group: current_group,
        mode: '0755'
      )
    end

    it 'copies the native bbl binary under the local home directory' do
      expect(chef_run).to create_cookbook_file(::File.join(bin_dir, 'bbl')).with(
        source: 'bbl',
        owner: current_user,
        group: current_group,
        mode: '0755'
      )
    end

    it 'copies the expected bbl version file under the local home directory' do
      expect(chef_run).to create_cookbook_file(::File.join(install_root, 'version.txt')).with(
        source: 'version.txt',
        owner: current_user,
        group: current_group,
        mode: '0644'
      )
    end

    it 'copies the search helpers under the local home directory' do
      linux_helper_bin_names.each do |bin_name|
        expect(chef_run).to create_cookbook_file(::File.join(bin_dir, bin_name)).with(
          source: bin_name,
          owner: current_user,
          group: current_group,
          mode: '0755'
        )
      end
    end

    it 'copies the pack fixture zips under the local home directory' do
      linux_pack_names.each do |pack_name|
        expect(chef_run).to create_cookbook_file(::File.join(pack_dir, pack_name)).with(
          source: pack_name,
          owner: current_user,
          group: current_group,
          mode: '0644'
        )
      end
    end
  end
end

describe 'bbl binary' do
  subject { file($bbl_bin) }
  it { should exist }
  it { should be_file }
  it 'is executable' do
    skip 'N/A on Windows' if $bbl_windows
    expect(subject).to be_executable
  end
end

describe 'bbl version file' do
  subject { file($bbl_version_file) }
  it { should exist }
  it { should be_file }
  its('content') { should match(/\Av\d+\.\d+\s*\z/) }
end

describe 'bbl Linux install home' do
  it 'uses the ubuntu home directory' do
    skip 'N/A outside Linux' if $bbl_windows || $bbl_macos

    expect($bbl_home_dir).to eq('/home/ubuntu')
    expect($bbl_pack_dir).to eq('/home/ubuntu/.bbl/packs')
    expect(file('/home/ubuntu/.bbl')).to be_directory
  end
end

describe 'bbl -v' do
  subject(:cmd) { command($bbl_run.call('-v')) }
  its('stdout') { should include("bbl version #{$bbl_expected_version}") }
end

$bbl_normalized_stdout = ->(cmd) { cmd.stdout.gsub("\r\n", "\n").force_encoding('UTF-8') }

describe 'bbl' do
  subject(:cmd) { command($bbl_run.call('')) }

  it 'prints Genesis 1:1 in WEBUS by default' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      [
        "1 In the beginning, God created the heavens and the earth.",
        "2 The earth was formless and empty. Darkness was on the surface of the deep and God’s Spirit was hovering over the surface of the waters.",
        "3 God said, “Let there be light,” and there was light.",
        "4 God saw the light, and saw that it was good. God divided the light from the darkness.",
        "5 God called the light “day”, and the darkness he called “night”. There was evening and there was morning, the first day.",
        "6 God said, “Let there be an expanse in the middle of the waters, and let it divide the waters from the waters.”",
        "7 God made the expanse, and divided the waters which were under the expanse from the waters which were above the expanse; and it was so.",
        "8 God called the expanse “sky”. There was evening and there was morning, a second day.",
        "9 God said, “Let the waters under the sky be gathered together to one place, and let the dry land appear;” and it was so.",
        "10 God called the dry land “earth”, and the gathering together of the waters he called “seas”. God saw that it was good.",
        "11 God said, “Let the earth yield grass, herbs yielding seeds, and fruit trees bearing fruit after their kind, with their seeds in it, on the earth;” and it was so.",
        "12 The earth yielded grass, herbs yielding seed after their kind, and trees bearing fruit, with their seeds in it, after their kind; and God saw that it was good.",
        "13 There was evening and there was morning, a third day.",
        "14 God said, “Let there be lights in the expanse of the sky to divide the day from the night; and let them be for signs to mark seasons, days, and years;",
        "15 and let them be for lights in the expanse of the sky to give light on the earth;” and it was so.",
        "16 God made the two great lights: the greater light to rule the day, and the lesser light to rule the night. He also made the stars.",
        "17 God set them in the expanse of the sky to give light to the earth,",
        "18 and to rule over the day and over the night, and to divide the light from the darkness. God saw that it was good.",
        "19 There was evening and there was morning, a fourth day.",
        "20 God said, “Let the waters abound with living creatures, and let birds fly above the earth in the open expanse of the sky.”",
        "21 God created the large sea creatures and every living creature that moves, with which the waters swarmed, after their kind, and every winged bird after its kind. God saw that it was good.",
        "22 God blessed them, saying, “Be fruitful, and multiply, and fill the waters in the seas, and let birds multiply on the earth.”",
        "23 There was evening and there was morning, a fifth day.",
        "24 God said, “Let the earth produce living creatures after their kind, livestock, creeping things, and animals of the earth after their kind;” and it was so.",
        "25 God made the animals of the earth after their kind, and the livestock after their kind, and everything that creeps on the ground after its kind. God saw that it was good.",
        "26 God said, “Let’s make man in our image, after our likeness. Let them have dominion over the fish of the sea, and over the birds of the sky, and over the livestock, and over all the earth, and over every creeping thing that creeps on the earth.”",
        "27 God created man in his own image. In God’s image he created him; male and female he created them.",
        "28 God blessed them. God said to them, “Be fruitful, multiply, fill the earth, and subdue it. Have dominion over the fish of the sea, over the birds of the sky, and over every living thing that moves on the earth.”",
        "29 God said, “Behold, I have given you every herb yielding seed, which is on the surface of all the earth, and every tree, which bears fruit yielding seed. It will be your food.",
        "30 To every animal of the earth, and to every bird of the sky, and to everything that creeps on the earth, in which there is life, I have given every green herb for food;” and it was so.",
        "31 God saw everything that he had made, and, behold, it was very good. There was evening and there was morning, a sixth day.",
      ].join("\n") + "\n"
    )
  end
end

describe 'bbl gen 1' do
  subject(:cmd) { command($bbl_run.call('gen 1')) }

  it 'prints the exact WEBUS Genesis 1 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      [
        "1 In the beginning, God created the heavens and the earth.",
        "2 The earth was formless and empty. Darkness was on the surface of the deep and God’s Spirit was hovering over the surface of the waters.",
        "3 God said, “Let there be light,” and there was light.",
        "4 God saw the light, and saw that it was good. God divided the light from the darkness.",
        "5 God called the light “day”, and the darkness he called “night”. There was evening and there was morning, the first day.",
        "6 God said, “Let there be an expanse in the middle of the waters, and let it divide the waters from the waters.”",
        "7 God made the expanse, and divided the waters which were under the expanse from the waters which were above the expanse; and it was so.",
        "8 God called the expanse “sky”. There was evening and there was morning, a second day.",
        "9 God said, “Let the waters under the sky be gathered together to one place, and let the dry land appear;” and it was so.",
        "10 God called the dry land “earth”, and the gathering together of the waters he called “seas”. God saw that it was good.",
        "11 God said, “Let the earth yield grass, herbs yielding seeds, and fruit trees bearing fruit after their kind, with their seeds in it, on the earth;” and it was so.",
        "12 The earth yielded grass, herbs yielding seed after their kind, and trees bearing fruit, with their seeds in it, after their kind; and God saw that it was good.",
        "13 There was evening and there was morning, a third day.",
        "14 God said, “Let there be lights in the expanse of the sky to divide the day from the night; and let them be for signs to mark seasons, days, and years;",
        "15 and let them be for lights in the expanse of the sky to give light on the earth;” and it was so.",
        "16 God made the two great lights: the greater light to rule the day, and the lesser light to rule the night. He also made the stars.",
        "17 God set them in the expanse of the sky to give light to the earth,",
        "18 and to rule over the day and over the night, and to divide the light from the darkness. God saw that it was good.",
        "19 There was evening and there was morning, a fourth day.",
        "20 God said, “Let the waters abound with living creatures, and let birds fly above the earth in the open expanse of the sky.”",
        "21 God created the large sea creatures and every living creature that moves, with which the waters swarmed, after their kind, and every winged bird after its kind. God saw that it was good.",
        "22 God blessed them, saying, “Be fruitful, and multiply, and fill the waters in the seas, and let birds multiply on the earth.”",
        "23 There was evening and there was morning, a fifth day.",
        "24 God said, “Let the earth produce living creatures after their kind, livestock, creeping things, and animals of the earth after their kind;” and it was so.",
        "25 God made the animals of the earth after their kind, and the livestock after their kind, and everything that creeps on the ground after its kind. God saw that it was good.",
        "26 God said, “Let’s make man in our image, after our likeness. Let them have dominion over the fish of the sea, and over the birds of the sky, and over the livestock, and over all the earth, and over every creeping thing that creeps on the earth.”",
        "27 God created man in his own image. In God’s image he created him; male and female he created them.",
        "28 God blessed them. God said to them, “Be fruitful, multiply, fill the earth, and subdue it. Have dominion over the fish of the sea, over the birds of the sky, and over every living thing that moves on the earth.”",
        "29 God said, “Behold, I have given you every herb yielding seed, which is on the surface of all the earth, and every tree, which bears fruit yielding seed. It will be your food.",
        "30 To every animal of the earth, and to every bird of the sky, and to everything that creeps on the earth, in which there is life, I have given every green herb for food;” and it was so.",
        "31 God saw everything that he had made, and, behold, it was very good. There was evening and there was morning, a sixth day.",
      ].join("\n") + "\n"
    )
  end
end

describe 'bbl john 3:16' do
  subject(:cmd) { command($bbl_run.call('john 3:16')) }

  it 'prints the exact WEBUS John 3:16 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq("16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life.\n")
  end
end

describe 'bbl john 3:16 in kjv' do
  subject(:cmd) { command($bbl_run.call('john 3:16 in kjv')) }

  it 'prints the exact KJV John 3:16 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq("16 For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.\n")
  end
end

describe 'bbl matthew 28:18-20' do
  subject(:cmd) { command($bbl_run.call('matthew 28:18-20')) }

  it 'prints the exact WEBUS Matthew 28:18-20 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      "18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.\n" \
      "19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,\n" \
      "20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.\n\n"
    )
  end
end

describe 'bbl matthew 28:18-20 in kjv' do
  subject(:cmd) { command($bbl_run.call('matthew 28:18-20 in kjv')) }

  it 'prints the exact KJV Matthew 28:18-20 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      "18 And Jesus came and spake unto them, saying, All power is given unto me in heaven and in earth.\n" \
      "19 Go ye therefore, and teach all nations, baptizing them in the name of the Father, and of the Son, and of the Holy Ghost:\n" \
      "20 Teaching them to observe all things whatsoever I have commanded you: and, lo, I am with you alway, [even] unto the end of the world. Amen.\n"
    )
  end
end

describe 'bbl pack dir' do
  subject { file($bbl_pack_dir) }
  it { should exist }
  it { should be_directory }

  it 'is owned by the Linux install user' do
    skip 'N/A outside Linux' if $bbl_windows || $bbl_macos

    expect(subject.owner).to eq($bbl_install_user)
  end
end

describe 'bbl pack files' do
  let(:pack_codes) { $bbl_installed_pack_codes }
  let(:pack_dir) { $bbl_pack_dir }
  let(:sep) { $bbl_sep }

  it 'exist and are non-empty' do
    pack_codes.each do |code|
      pack_file = file("#{pack_dir}#{sep}#{code}.zip")
      expect(pack_file).to exist
      expect(pack_file).to be_file
      expect(pack_file.size).to be > 0
      expect(pack_file.owner).to eq($bbl_install_user) unless $bbl_windows || $bbl_macos
    end
  end
end

describe 'bbl helper binaries' do
  let(:helper_names) { $bbl_installed_search_helpers }
  let(:helper_bin_dir) { $bbl_helper_bin_dir }
  let(:sep) { $bbl_sep }

  it 'exist and are executable' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      helper_file = file(path)
      expect(helper_file).to exist
      expect(helper_file).to be_file
      unless $bbl_windows
        expect(helper_file).to be_executable
      end
    end
  end

  it 'report correct version via --version' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      expected_name = $bbl_windows ? name.delete_suffix('.exe') : name
      expected_stdout = "#{expected_name} #{$bbl_expected_version}#{$bbl_eol}"
      cmd = command($bbl_helper_run.call(path, '--version'))
      expect(cmd.exit_status).to eq 0
      expect(cmd.stdout).to eq(expected_stdout)
    end
  end

  it 'report correct version via -v' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      expected_name = $bbl_windows ? name.delete_suffix('.exe') : name
      expected_stdout = "#{expected_name} #{$bbl_expected_version}#{$bbl_eol}"
      cmd = command($bbl_helper_run.call(path, '-v'))
      expect(cmd.exit_status).to eq 0
      expect(cmd.stdout).to eq(expected_stdout)
    end
  end
end

describe 'bbl pack manifest versions' do
  let(:pack_codes) { $bbl_installed_pack_codes }
  let(:pack_dir) { $bbl_pack_dir }
  let(:sep) { $bbl_sep }
  let(:expected_version) { $bbl_expected_version }

  it 'match expected version' do
    pack_codes.each do |code|
      pack_file = "#{pack_dir}#{sep}#{code}.zip"
      manifest = "#{code}.0.manifest.json"

      version = if $bbl_windows
        $bbl_zip_manifest_version.call(pack_file, manifest)
      else
        $bbl_zip_manifest_version.call(file(pack_file).content, manifest)
      end

      expect(version).to eq(expected_version)
    end
  end
end

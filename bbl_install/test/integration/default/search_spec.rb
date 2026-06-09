RSpec.shared_context 'search helpers' do
  def search_stdout(cmd)
    command(cmd).stdout.force_encoding('UTF-8')
  end

  def translation_list_lines(cmd)
    search_stdout(cmd).lines.map(&:strip).reject(&:empty?).select { |l| l.match?(/^[A-Z0-9]+\s+\|/) }
  end

  def search_results(cmd)
    results = []
    current = []
    search_stdout(cmd).each_line do |line|
      s = line.strip
      next if s.empty?
      if s.match?(/^\S.*\d+:\d+\s+/)
        results << current.join("\n").strip unless current.empty?
        current = [s]
      else
        current << s
      end
    end
    results << current.join("\n").strip unless current.empty?
    results.reject(&:empty?)
  end
end

describe command($bbl_run.call('search Jesus')) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/The book of the genealogy of Jesus Christ/) }
end

describe 'bbl list translations output' do
  include_context 'search helpers'
  subject(:translations) { translation_list_lines($bbl_run.call('list translations')) }

  it 'lists the full translation catalog' do
    expect(translations.length).to eq(27)
  end
end

['Jesus', 'Christ', 'Jesus Christ'].each do |search_single_term|
  describe command($bbl_run.call("search #{search_single_term}")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/The book of the genealogy of Jesus Christ/) }
  end

  describe "bbl search #{search_single_term} exact output" do
    include_context 'search helpers'
    subject(:results) { search_results($bbl_run.call("search #{search_single_term}")) }

    it 'starts with the expected webus verse text' do
      expect(results.first).to eq('Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.')
    end

    it 'returns multiple results by default' do
      expect(results.length).to be > 1
    end
  end

  describe command($bbl_run.call("search #{search_single_term} in kjv")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/The book of the generation of Jesus Christ/) }
  end

  describe "bbl search #{search_single_term} in kjv exact output" do
    include_context 'search helpers'
    subject(:results) { search_results($bbl_run.call("search #{search_single_term} in kjv")) }

    it 'starts with the expected kjv verse text' do
      expect(results.first).to eq('Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.')
    end

    it 'returns multiple results by default' do
      expect(results.length).to be > 1
    end
  end

  describe command($bbl_run.call("search #{search_single_term} in romans")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Paul, a servant of Jesus Christ/) }
  end

  describe "bbl search #{search_single_term} in romans exact output" do
    include_context 'search helpers'
    subject(:results) { search_results($bbl_run.call("search #{search_single_term} in romans")) }

    it 'starts with the expected romans webus verse text' do
      expect(results.first).to eq('Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,')
    end

    it 'returns multiple Romans hits' do
      expect(results.length).to be > 1
    end
  end

  describe command($bbl_run.call("search #{search_single_term} in romans 5-12")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Being therefore justified by faith/) }
  end

  describe "bbl search #{search_single_term} in romans 5-12 exact output" do
    include_context 'search helpers'
    subject(:results) { search_results($bbl_run.call("search #{search_single_term} in romans 5-12")) }

    it 'starts with the expected romans chapter-range webus verse text' do
      expect(results.first).to eq('Romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;')
    end

    it 'returns multiple hits in the requested chapter range' do
      expect(results.length).to be > 1
    end
  end

  describe command($bbl_run.call("search #{search_single_term} in romans 5-12 in kjv")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Therefore being justified by faith/) }
  end

  describe "bbl search #{search_single_term} in romans 5-12 in kjv exact output" do
    include_context 'search helpers'
    subject(:results) { search_results($bbl_run.call("search #{search_single_term} in romans 5-12 in kjv")) }

    it 'starts with the expected romans chapter-range kjv verse text' do
      expect(results.first).to eq('Romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:')
    end

    it 'returns multiple hits in the requested kjv chapter range' do
      expect(results.length).to be > 1
    end
  end

  describe command($bbl_run.call("search #{search_single_term} in johns letters")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/1 John 1:3/) }
  end

  describe "bbl search #{search_single_term} in johns letters exact output" do
    include_context 'search helpers'
    subject(:results) { search_results($bbl_run.call("search #{search_single_term} in johns letters")) }

    it 'starts with the expected Johns letters verse text' do
      expect(results.first).to match(/\A1 John 1:3 /)
    end

    it 'returns multiple hits from the requested category' do
      expect(results.length).to be > 1
    end
  end

  describe command($bbl_run.call("search #{search_single_term} in johns letters in kjv")) do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/1 John 1:3/) }
  end

  describe "bbl search #{search_single_term} --category johns letters exact output" do
    include_context 'search helpers'
    subject(:results) { search_results($bbl_run.call("search #{search_single_term} --category 'johns letters'")) }

    it 'starts with the expected Johns letters verse text' do
      expect(results.first).to match(/\A1 John 1:3 /)
    end

    it 'returns multiple hits from the requested category' do
      expect(results.length).to be > 1
    end
  end
end

describe 'bbl search Jesus exact output' do
  include_context 'search helpers'
  subject(:results) { search_results($bbl_run.call('search Jesus')) }

  it 'starts with the expected webus verse text' do
    expect(results.first).to eq('Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.')
  end

  it 'returns multiple results by default' do
    expect(results.length).to be > 1
  end
end

describe 'bbl search Jesus wept exact output' do
  include_context 'search helpers'
  subject(:results) { search_results($bbl_run.call('search Jesus wept')) }

  it 'starts with the expected plain-search verse text' do
    expect(results.first).to eq("Matthew 26:75 Peter remembered the word which Jesus had said to him, \u{201C}Before the rooster crows, you will deny me three times.\u{201D} Then he went out and wept bitterly.")
  end

  it 'returns multiple results by default' do
    expect(results.length).to be > 1
  end
end

describe 'bbl search Jesus weep exact output' do
  include_context 'search helpers'
  subject(:results) { search_results($bbl_run.call('search Jesus weep')) }

  it 'starts with the expected normalized-search verse text' do
    expect(results.first).to eq("Matthew 26:75 Peter remembered the word which Jesus had said to him, \u{201C}Before the rooster crows, you will deny me three times.\u{201D} Then he went out and wept bitterly.")
  end

  it 'returns multiple results by default' do
    expect(results.length).to be > 1
  end
end

describe 'bbl search quoted Jesus wept exact output' do
  include_context 'search helpers'
  subject(:results) { search_results($bbl_run.call('search "Jesus wept"')) }

  it 'starts with the expected exact-search verse text' do
    expect(results.first).to eq('John 11:35 Jesus wept.')
  end

  it 'returns only the exact phrase result' do
    expect(results.length).to eq(1)
  end
end

describe 'bbl search 예수 그리스도 in krv exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search 예수 그리스도 in krv') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('includes nori phrase') { expect(search_stdout(cmd)).to include('예수 그리스도의 세계라') }
  it('starts with nori verse') { expect(results.first).to eq('마태복음 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라') }
end

describe 'bbl search 耶稣基督 in cunp exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search 耶稣基督 in cunp') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('includes smartcn phrase') { expect(search_stdout(cmd)).to include('耶稣基督的家谱：') }
  it('starts with smartcn verse') { expect(results.first).to eq('马太福音 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：') }
end

describe 'bbl search Jezusa Chrystusa in ubg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jezusa Chrystusa in ubg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('includes morfologik phrase') { expect(search_stdout(cmd)).to include('Księga rodu Jezusa Chrystusa') }
  it('starts with morfologik verse') { expect(results.first).to eq('Mateusza 1:1 Księga rodu Jezusa Chrystusa, syna Dawida, syna Abrahama.') }
end

[
  ['Ісуса Христа', 'in ubio', 'Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:'],
  ['Ісуса Христа', 'in romans in ubio', 'До римлян 1:1 Павло, раб Ісуса Христа, покликаний апостол, вибраний для звіщання Євангелії Божої,'],
  ['Ісуса Христа', 'in romans 2 in ubio', 'До римлян 2:16 дня, коли Бог, згідно з моїм благовістям, буде судити таємні речі людей через Ісуса Христа.'],
  ['Ісуса Христа', 'in romans 3-5 in ubio', 'До римлян 3:22 А Божа правда через віру в Ісуса Христа в усіх і на всіх, хто вірує, бо різниці немає,'],
  ['Ісуса Христа', 'in johns letters in ubio', '1-е Iвана 1:3 що ми бачили й чули про те ми звіщаємо вам, щоб і ви мали спільність із нами. Спільність же наша з Отцем і Сином Його Ісусом Христом.'],
  ['Ісуса', 'in ubio', 'Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:'],
  ['Ісуса', 'in romans in ubio', 'До римлян 1:1 Павло, раб Ісуса Христа, покликаний апостол, вибраний для звіщання Євангелії Божої,'],
  ['Ісуса', 'in romans 2 in ubio', 'До римлян 2:16 дня, коли Бог, згідно з моїм благовістям, буде судити таємні речі людей через Ісуса Христа.'],
  ['Ісуса', 'in romans 3-5 in ubio', 'До римлян 3:22 А Божа правда через віру в Ісуса Христа в усіх і на всіх, хто вірує, бо різниці немає,'],
  ['Ісуса', 'in johns letters in ubio', '1-е Iвана 1:3 що ми бачили й чули про те ми звіщаємо вам, щоб і ви мали спільність із нами. Спільність же наша з Отцем і Сином Його Ісусом Христом.'],
  ['Христа', 'in ubio', 'Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:'],
  ['Христа', 'in romans in ubio', 'До римлян 1:1 Павло, раб Ісуса Христа, покликаний апостол, вибраний для звіщання Євангелії Божої,'],
  ['Христа', 'in romans 2 in ubio', 'До римлян 2:16 дня, коли Бог, згідно з моїм благовістям, буде судити таємні речі людей через Ісуса Христа.'],
  ['Христа', 'in romans 3-5 in ubio', 'До римлян 3:22 А Божа правда через віру в Ісуса Христа в усіх і на всіх, хто вірує, бо різниці немає,'],
  ['Христа', 'in johns letters in ubio', '1-е Iвана 1:3 що ми бачили й чули про те ми звіщаємо вам, щоб і ви мали спільність із нами. Спільність же наша з Отцем і Сином Його Ісусом Христом.'],
].each do |term, scope, expected|
  describe "bbl search #{term} #{scope} exact output" do
    include_context 'search helpers'
    let(:cmd) { $bbl_run.call("search #{term} #{scope}") }
    let(:result) { command(cmd) }
    subject(:results) { search_results(cmd) }

    it { expect(result.exit_status).to eq 0 }
    it('starts with Ukrainian text') { expect(results.first).to eq(expected) }
  end
end

describe 'bbl search Jêsus Christ in kttv exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jêsus Christ in kttv') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('includes extra phrase') { expect(search_stdout(cmd)).to include('Gia-phổ Đức Chúa Jêsus-Christ') }
  it('starts with extra verse') { expect(results.first).to eq('Ma-thi-ơ 1:1 Gia-phổ Đức Chúa Jêsus-Christ, con cháu Đa-vít và con cháu Áp-ra-ham.') }
end

describe 'bbl search Japanese term in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call("search \u{30A4}\u{30A8}\u{30B9} \u{30AD}\u{30EA}\u{30B9}\u{30C8} in jc") }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
end

describe 'bbl search Vietnamese term in kttv exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call("search J\u{00EA}sus Christ in kttv") }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
end

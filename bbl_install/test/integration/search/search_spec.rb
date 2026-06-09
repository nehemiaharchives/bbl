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

describe 'bbl search Jesus' do
  subject(:cmd) { command($bbl_run.call('search Jesus')) }
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

# webus (English - World English Bible)

['Jesus', 'Christ', 'Jesus Christ'].each do |search_single_term|
  describe "bbl search #{search_single_term}" do
    subject(:cmd) { command($bbl_run.call("search #{search_single_term}")) }
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

  describe "bbl search #{search_single_term} in kjv" do
    subject(:cmd) { command($bbl_run.call("search #{search_single_term} in kjv")) }
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

  describe "bbl search #{search_single_term} in romans" do
    subject(:cmd) { command($bbl_run.call("search #{search_single_term} in romans")) }
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

  describe "bbl search #{search_single_term} in romans 5-12" do
    subject(:cmd) { command($bbl_run.call("search #{search_single_term} in romans 5-12")) }
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

  describe "bbl search #{search_single_term} in romans 5-12 in kjv" do
    subject(:cmd) { command($bbl_run.call("search #{search_single_term} in romans 5-12 in kjv")) }
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

  describe "bbl search #{search_single_term} in johns letters" do
    subject(:cmd) { command($bbl_run.call("search #{search_single_term} in johns letters")) }
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

  describe "bbl search #{search_single_term} in johns letters in kjv" do
    subject(:cmd) { command($bbl_run.call("search #{search_single_term} in johns letters in kjv")) }
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

# krv (Korean - Revised Version)
describe 'bbl search 예수 그리스도 in krv exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search 예수 그리스도 in krv') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('includes nori phrase') { expect(search_stdout(cmd)).to include('예수 그리스도의 세계라') }
  it('starts with nori verse') { expect(results.first).to eq('마태복음 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라') }
end

# cunp (Chinese - Union Simplified)
describe 'bbl search 耶稣基督 in cunp exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search 耶稣基督 in cunp') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('includes smartcn phrase') { expect(search_stdout(cmd)).to include('耶稣基督的家谱：') }
  it('starts with smartcn verse') { expect(results.first).to eq('马太福音 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：') }
end

# ubg (Polish - Biblia Gdańska)
describe 'bbl search Jezusa Chrystusa in ubg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jezusa Chrystusa in ubg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('includes morfologik phrase') { expect(search_stdout(cmd)).to include('Księga rodu Jezusa Chrystusa') }
  it('starts with morfologik verse') { expect(results.first).to eq('Mateusza 1:1 Księga rodu Jezusa Chrystusa, syna Dawida, syna Abrahama.') }
end

# jc (Japanese - Colloquial)
describe 'bbl search Japanese term in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call("search \u{30A4}\u{30A8}\u{30B9} \u{30AD}\u{30EA}\u{30B9}\u{30C8} in jc") }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
end

# ---------------------------------------------------------------------------
# kttv (Vietnamese - Kinh Thánh)
# ---------------------------------------------------------------------------
describe 'bbl search Vietnamese term in kttv exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call("search J\u{00EA}sus Christ in kttv") }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
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

# ---------------------------------------------------------------------------
# ubio (Ukrainian - Sv. Pysma)
# ---------------------------------------------------------------------------

describe 'bbl search Ісуса Христа in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса Христа in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:') }
end

describe 'bbl search Ісуса Христа in romans in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса Христа in romans in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 1:1 Павло, раб Ісуса Христа, покликаний апостол, вибраний для звіщання Євангелії Божої,') }
end

describe 'bbl search Ісуса Христа in romans 2 in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса Христа in romans 2 in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 2:16 дня, коли Бог, згідно з моїм благовістям, буде судити таємні речі людей через Ісуса Христа.') }
end

describe 'bbl search Ісуса Христа in romans 3-5 in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса Христа in romans 3-5 in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 3:22 А Божа правда через віру в Ісуса Христа в усіх і на всіх, хто вірує, бо різниці немає,') }
end

describe 'bbl search Ісуса Христа in johns letters in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса Христа in johns letters in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1-е Iвана 1:3 що ми бачили й чули про те ми звіщаємо вам, щоб і ви мали спільність із нами. Спільність же наша з Отцем і Сином Його Ісусом Христом.') }
end

describe 'bbl search Ісуса in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:') }
end

describe 'bbl search Ісуса in romans in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса in romans in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 1:1 Павло, раб Ісуса Христа, покликаний апостол, вибраний для звіщання Євангелії Божої,') }
end

describe 'bbl search Ісуса in romans 2 in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса in romans 2 in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 2:16 дня, коли Бог, згідно з моїм благовістям, буде судити таємні речі людей через Ісуса Христа.') }
end

describe 'bbl search Ісуса in romans 3-5 in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса in romans 3-5 in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 3:22 А Божа правда через віру в Ісуса Христа в усіх і на всіх, хто вірує, бо різниці немає,') }
end

describe 'bbl search Ісуса in johns letters in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Ісуса in johns letters in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1-е Iвана 1:3 що ми бачили й чули про те ми звіщаємо вам, щоб і ви мали спільність із нами. Спільність же наша з Отцем і Сином Його Ісусом Христом.') }
end

describe 'bbl search Христа in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:') }
end

describe 'bbl search Христа in romans in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in romans in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 1:1 Павло, раб Ісуса Христа, покликаний апостол, вибраний для звіщання Євангелії Божої,') }
end

describe 'bbl search Христа in romans 2 in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in romans 2 in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 2:16 дня, коли Бог, згідно з моїм благовістям, буде судити таємні речі людей через Ісуса Христа.') }
end

describe 'bbl search Христа in romans 3-5 in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in romans 3-5 in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('До римлян 3:22 А Божа правда через віру в Ісуса Христа в усіх і на всіх, хто вірує, бо різниці немає,') }
end

describe 'bbl search Христа in johns letters in ubio exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in johns letters in ubio') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1-е Iвана 1:3 що ми бачили й чули про те ми звіщаємо вам, щоб і ви мали спільність із нами. Спільність же наша з Отцем і Сином Його Ісусом Христом.') }
end

# ---------------------------------------------------------------------------
# rvr09 (Spanish - Reina Valera 1909)
# ---------------------------------------------------------------------------

describe 'bbl search Jesucristo in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Mateo 1:1 LIBRO de la generación de Jesucristo, hijo de David, hijo de Abraham.') }
end

describe 'bbl search Jesucristo in romans in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in romans in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 1:1 PABLO, siervo de Jesucristo, llamado á ser apóstol, apartado para el evangelio de Dios,') }
end

describe 'bbl search Jesucristo in romans 2 in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in romans 2 in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 2:16 En el día que juzgará el Señor lo encubierto de los hombres, conforme á mi evangelio, por Jesucristo.') }
end

describe 'bbl search Jesucristo in romans 3-5 in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in romans 3-5 in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 3:22 La justicia de Dios por la fe de Jesucristo, para todos los que creen en él: porque no hay diferencia;') }
end

describe 'bbl search Jesucristo in johns letters in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in johns letters in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Juan 1:3 Lo que hemos visto y oído, eso os anunciamos, para que también vosotros tengáis comunión con nosotros: y nuestra comunión verdaderamente es con el Padre, y con su Hijo Jesucristo.') }
end

describe 'bbl search Jesús in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesús in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Mateo 1:1 LIBRO de la generación de Jesucristo, hijo de David, hijo de Abraham.') }
end

describe 'bbl search Jesús in romans in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesús in romans in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 1:1 PABLO, siervo de Jesucristo, llamado á ser apóstol, apartado para el evangelio de Dios,') }
end

describe 'bbl search Jesús in romans 2 in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesús in romans 2 in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 2:16 En el día que juzgará el Señor lo encubierto de los hombres, conforme á mi evangelio, por Jesucristo.') }
end

describe 'bbl search Jesús in romans 3-5 in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesús in romans 3-5 in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 3:22 La justicia de Dios por la fe de Jesucristo, para todos los que creen en él: porque no hay diferencia;') }
end

describe 'bbl search Jesús in johns letters in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesús in johns letters in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Juan 1:3 Lo que hemos visto y oído, eso os anunciamos, para que también vosotros tengáis comunión con nosotros: y nuestra comunión verdaderamente es con el Padre, y con su Hijo Jesucristo.') }
end

describe 'bbl search Cristo in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Mateo 1:1 LIBRO de la generación de Jesucristo, hijo de David, hijo de Abraham.') }
end

describe 'bbl search Cristo in romans in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 1:1 PABLO, siervo de Jesucristo, llamado á ser apóstol, apartado para el evangelio de Dios,') }
end

describe 'bbl search Cristo in romans 2 in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans 2 in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 2:16 En el día que juzgará el Señor lo encubierto de los hombres, conforme á mi evangelio, por Jesucristo.') }
end

describe 'bbl search Cristo in romans 3-5 in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans 3-5 in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 3:22 La justicia de Dios por la fe de Jesucristo, para todos los que creen en él: porque no hay diferencia;') }
end

describe 'bbl search Cristo in johns letters in rvr09 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in johns letters in rvr09') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Juan 1:3 Lo que hemos visto y oído, eso os anunciamos, para que también vosotros tengáis comunión con nosotros: y nuestra comunión verdaderamente es con el Padre, y con su Hijo Jesucristo.') }
end

# ---------------------------------------------------------------------------
# tb (Portuguese - Tradução Brasileira)
# ---------------------------------------------------------------------------

describe 'bbl search Jesus Cristo in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus Cristo in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Mateus 1:1 Livro da geração de Jesus Cristo, filho de Davi, filho de Abraão.') }
end

describe 'bbl search Jesus Cristo in romans in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus Cristo in romans in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 1:1 Paulo, servo de Cristo Jesus, chamado para ser apóstolo, separado para o Evangelho de Deus,') }
end

describe 'bbl search Jesus Cristo in romans 2 in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus Cristo in romans 2 in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 2:16 no dia em que Deus, segundo o meu evangelho, há de julgar as coisas ocultas dos homens, por Cristo Jesus.') }
end

describe 'bbl search Jesus Cristo in romans 3-5 in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus Cristo in romans 3-5 in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 3:22 a saber, a justiça de Deus mediante a fé em Jesus Cristo, para com todos os que creem. Pois não há distinção,') }
end

describe 'bbl search Jesus Cristo in johns letters in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus Cristo in johns letters in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 João 1:3 o que temos visto e ouvido também vo-lo anunciamos, para que vós também tenhais comunhão conosco. A nossa comunhão é com o Pai e com seu Filho, Jesus Cristo.') }
end

describe 'bbl search Jesus in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Mateus 1:1 Livro da geração de Jesus Cristo, filho de Davi, filho de Abraão.') }
end

describe 'bbl search Jesus in romans in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in romans in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 1:1 Paulo, servo de Cristo Jesus, chamado para ser apóstolo, separado para o Evangelho de Deus,') }
end

describe 'bbl search Jesus in romans 2 in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in romans 2 in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 2:16 no dia em que Deus, segundo o meu evangelho, há de julgar as coisas ocultas dos homens, por Cristo Jesus.') }
end

describe 'bbl search Jesus in romans 3-5 in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in romans 3-5 in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 3:22 a saber, a justiça de Deus mediante a fé em Jesus Cristo, para com todos os que creem. Pois não há distinção,') }
end

describe 'bbl search Jesus in johns letters in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in johns letters in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 João 1:3 o que temos visto e ouvido também vo-lo anunciamos, para que vós também tenhais comunhão conosco. A nossa comunhão é com o Pai e com seu Filho, Jesus Cristo.') }
end

describe 'bbl search Cristo in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Mateus 1:1 Livro da geração de Jesus Cristo, filho de Davi, filho de Abraão.') }
end

describe 'bbl search Cristo in romans in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 1:1 Paulo, servo de Cristo Jesus, chamado para ser apóstolo, separado para o Evangelho de Deus,') }
end

describe 'bbl search Cristo in romans 2 in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans 2 in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 2:16 no dia em que Deus, segundo o meu evangelho, há de julgar as coisas ocultas dos homens, por Cristo Jesus.') }
end

describe 'bbl search Cristo in romans 3-5 in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans 3-5 in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romanos 3:22 a saber, a justiça de Deus mediante a fé em Jesus Cristo, para com todos os que creem. Pois não há distinção,') }
end

describe 'bbl search Cristo in johns letters in tb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in johns letters in tb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 João 1:3 o que temos visto e ouvido também vo-lo anunciamos, para que vós também tenhais comunhão conosco. A nossa comunhão é com o Pai e com seu Filho, Jesus Cristo.') }
end

# ---------------------------------------------------------------------------
# delut (German - Luther 1912)
# ---------------------------------------------------------------------------

describe 'bbl search Jesu Christi in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Christi in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matthäus 1:1 Dies ist das Buch von der Geburt Jesu Christi, der da ist ein Sohn Davids, des Sohnes Abrahams.') }
end

describe 'bbl search Jesu Christi in romans in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Christi in romans in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 1:1 Paulus, ein Knecht Jesu Christi, berufen zum Apostel, ausgesondert, zu predigen das Evangelium Gottes,') }
end

describe 'bbl search Jesu Christi in romans 2 in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Christi in romans 2 in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 2:16 auf den Tag, da Gott das Verborgene der Menschen durch Jesus Christus richten wird laut meines Evangeliums.') }
end

describe 'bbl search Jesu Christi in romans 3-5 in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Christi in romans 3-5 in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 3:22 Ich sage aber von solcher Gerechtigkeit vor Gott, die da kommt durch den Glauben an Jesum Christum zu allen und auf alle, die da glauben.') }
end

describe 'bbl search Jesu Christi in johns letters in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Christi in johns letters in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1. Johannes 1:3 was wir gesehen und gehört haben, das verkündigen wir euch, auf daß ihr mit uns Gemeinschaft habt; und unsre Gemeinschaft ist mit dem Vater und mit seinem Sohn Jesus Christus.') }
end

describe 'bbl search Jesu in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matthäus 1:1 Dies ist das Buch von der Geburt Jesu Christi, der da ist ein Sohn Davids, des Sohnes Abrahams.') }
end

describe 'bbl search Jesu in romans in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in romans in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 1:1 Paulus, ein Knecht Jesu Christi, berufen zum Apostel, ausgesondert, zu predigen das Evangelium Gottes,') }
end

describe 'bbl search Jesu in romans 2 in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in romans 2 in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 2:16 auf den Tag, da Gott das Verborgene der Menschen durch Jesus Christus richten wird laut meines Evangeliums.') }
end

describe 'bbl search Jesu in romans 3-5 in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in romans 3-5 in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 3:22 Ich sage aber von solcher Gerechtigkeit vor Gott, die da kommt durch den Glauben an Jesum Christum zu allen und auf alle, die da glauben.') }
end

describe 'bbl search Jesu in johns letters in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in johns letters in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1. Johannes 1:3 was wir gesehen und gehört haben, das verkündigen wir euch, auf daß ihr mit uns Gemeinschaft habt; und unsre Gemeinschaft ist mit dem Vater und mit seinem Sohn Jesus Christus.') }
end

describe 'bbl search Christi in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christi in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matthäus 1:1 Dies ist das Buch von der Geburt Jesu Christi, der da ist ein Sohn Davids, des Sohnes Abrahams.') }
end

describe 'bbl search Christi in romans in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christi in romans in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 1:1 Paulus, ein Knecht Jesu Christi, berufen zum Apostel, ausgesondert, zu predigen das Evangelium Gottes,') }
end

describe 'bbl search Christi in romans 2 in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christi in romans 2 in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 2:16 auf den Tag, da Gott das Verborgene der Menschen durch Jesus Christus richten wird laut meines Evangeliums.') }
end

describe 'bbl search Christi in romans 3-5 in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christi in romans 3-5 in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Römer 3:22 Ich sage aber von solcher Gerechtigkeit vor Gott, die da kommt durch den Glauben an Jesum Christum zu allen und auf alle, die da glauben.') }
end

describe 'bbl search Christi in johns letters in delut exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christi in johns letters in delut') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1. Johannes 1:3 was wir gesehen und gehört haben, das verkündigen wir euch, auf daß ihr mit uns Gemeinschaft habt; und unsre Gemeinschaft ist mit dem Vater und mit seinem Sohn Jesus Christus.') }
end

# ---------------------------------------------------------------------------
# lsg (French - Louis Segond)
# ---------------------------------------------------------------------------

describe 'bbl search Jésus-Christ in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus-Christ in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matthieu 1:1 Généalogie de Jésus-Christ, fils de David, fils d’Abraham.') }
end

describe 'bbl search Jésus-Christ in romans in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus-Christ in romans in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 1:1 Paul, serviteur de Jésus-Christ, appelé à être apôtre, mis à part pour annoncer l’Évangile de Dieu,') }
end

describe 'bbl search Jésus-Christ in romans 2 in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus-Christ in romans 2 in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 2:16 C’est ce qui paraîtra au jour où, selon mon Évangile, Dieu jugera par Jésus-Christ les actions secrètes des hommes.') }
end

describe 'bbl search Jésus-Christ in romans 3-5 in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus-Christ in romans 3-5 in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 3:22 justice de Dieu par la foi en Jésus-Christ pour tous ceux qui croient. Il n’y a point de distinction.') }
end

describe 'bbl search Jésus-Christ in johns letters in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus-Christ in johns letters in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Jean 1:3 ce que nous avons vu et entendu, nous vous l’annonçons, à vous aussi, afin que vous aussi vous soyez en communion avec nous. Or, notre communion est avec le Père et avec son Fils Jésus-Christ.') }
end

describe 'bbl search Jésus in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matthieu 1:1 Généalogie de Jésus-Christ, fils de David, fils d’Abraham.') }
end

describe 'bbl search Jésus in romans in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus in romans in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 1:1 Paul, serviteur de Jésus-Christ, appelé à être apôtre, mis à part pour annoncer l’Évangile de Dieu,') }
end

describe 'bbl search Jésus in romans 2 in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus in romans 2 in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 2:16 C’est ce qui paraîtra au jour où, selon mon Évangile, Dieu jugera par Jésus-Christ les actions secrètes des hommes.') }
end

describe 'bbl search Jésus in romans 3-5 in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus in romans 3-5 in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 3:22 justice de Dieu par la foi en Jésus-Christ pour tous ceux qui croient. Il n’y a point de distinction.') }
end

describe 'bbl search Jésus in johns letters in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jésus in johns letters in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Jean 1:3 ce que nous avons vu et entendu, nous vous l’annonçons, à vous aussi, afin que vous aussi vous soyez en communion avec nous. Or, notre communion est avec le Père et avec son Fils Jésus-Christ.') }
end

describe 'bbl search Christ in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christ in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matthieu 1:1 Généalogie de Jésus-Christ, fils de David, fils d’Abraham.') }
end

describe 'bbl search Christ in romans in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christ in romans in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 1:1 Paul, serviteur de Jésus-Christ, appelé à être apôtre, mis à part pour annoncer l’Évangile de Dieu,') }
end

describe 'bbl search Christ in romans 2 in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christ in romans 2 in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 2:16 C’est ce qui paraîtra au jour où, selon mon Évangile, Dieu jugera par Jésus-Christ les actions secrètes des hommes.') }
end

describe 'bbl search Christ in romans 3-5 in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christ in romans 3-5 in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romains 3:22 justice de Dieu par la foi en Jésus-Christ pour tous ceux qui croient. Il n’y a point de distinction.') }
end

describe 'bbl search Christ in johns letters in lsg exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Christ in johns letters in lsg') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Jean 1:3 ce que nous avons vu et entendu, nous vous l’annonçons, à vous aussi, afin que vous aussi vous soyez en communion avec nous. Or, notre communion est avec le Père et avec son Fils Jésus-Christ.') }
end

# ---------------------------------------------------------------------------
# sinod (Russian - Synodal)
# ---------------------------------------------------------------------------

describe 'bbl search Иисуса Христа in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса Христа in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('От Матфея святое благовествование 1:1 Родословие Иисуса Христа, Сына Давидова, Сына Авраамова.') }
end

describe 'bbl search Иисуса Христа in romans in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса Христа in romans in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 1:1 Павел, раб Иисуса Христа, призванный Апостол, избранный к благовестию Божию,') }
end

describe 'bbl search Иисуса Христа in romans 2 in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса Христа in romans 2 in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 2:16 в день, когда, по благовествованию моему, Бог будет судить тайные дела человеков через Иисуса Христа.') }
end

describe 'bbl search Иисуса Христа in romans 3-5 in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса Христа in romans 3-5 in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 3:22 правда Божия через веру в Иисуса Христа во всех и на всех верующих, ибо нет различия,') }
end

describe 'bbl search Иисуса Христа in johns letters in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса Христа in johns letters in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Первое послание Иоанна 1:3 о том, что мы видели и слышали, возвещаем вам, чтобы и вы имели общение с нами: а наше общение — с Отцем и Сыном Его, Иисусом Христом.') }
end

describe 'bbl search Иисуса in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('От Матфея святое благовествование 1:1 Родословие Иисуса Христа, Сына Давидова, Сына Авраамова.') }
end

describe 'bbl search Иисуса in romans in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса in romans in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 1:1 Павел, раб Иисуса Христа, призванный Апостол, избранный к благовестию Божию,') }
end

describe 'bbl search Иисуса in romans 2 in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса in romans 2 in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 2:16 в день, когда, по благовествованию моему, Бог будет судить тайные дела человеков через Иисуса Христа.') }
end

describe 'bbl search Иисуса in romans 3-5 in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса in romans 3-5 in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 3:22 правда Божия через веру в Иисуса Христа во всех и на всех верующих, ибо нет различия,') }
end

describe 'bbl search Иисуса in johns letters in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Иисуса in johns letters in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Первое послание Иоанна 1:3 о том, что мы видели и слышали, возвещаем вам, чтобы и вы имели общение с нами: а наше общение — с Отцем и Сыном Его, Иисусом Христом.') }
end

describe 'bbl search Христа in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('От Матфея святое благовествование 1:1 Родословие Иисуса Христа, Сына Давидова, Сына Авраамова.') }
end

describe 'bbl search Христа in romans in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in romans in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 1:1 Павел, раб Иисуса Христа, призванный Апостол, избранный к благовестию Божию,') }
end

describe 'bbl search Христа in romans 2 in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in romans 2 in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 2:16 в день, когда, по благовествованию моему, Бог будет судить тайные дела человеков через Иисуса Христа.') }
end

describe 'bbl search Христа in romans 3-5 in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in romans 3-5 in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Послание к Римлянам 3:22 правда Божия через веру в Иисуса Христа во всех и на всех верующих, ибо нет различия,') }
end

describe 'bbl search Христа in johns letters in sinod exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Христа in johns letters in sinod') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Первое послание Иоанна 1:3 о том, что мы видели и слышали, возвещаем вам, чтобы и вы имели общение с нами: а наше общение — с Отцем и Сыном Его, Иисусом Христом.') }
end

# ---------------------------------------------------------------------------
# svrj (Dutch - Statenvertaling)
# ---------------------------------------------------------------------------

describe 'bbl search JEZUS CHRISTUS in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS CHRISTUS in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MATTHEÜS 1:1 Het boek des geslachts van JEZUS CHRISTUS, den Zoon van David, den zoon van Abraham.') }
end

describe 'bbl search JEZUS CHRISTUS in romans in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS CHRISTUS in romans in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 1:1 Paulus, een dienstknecht van Jezus Christus, een geroepen apostel, afgezonderd tot het Evangelie van God,') }
end

describe 'bbl search JEZUS CHRISTUS in romans 2 in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS CHRISTUS in romans 2 in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 2:16 In den dag wanneer God de verborgene dingen der mensen zal oordelen door Jezus Christus, naar mijn Evangelie.') }
end

describe 'bbl search JEZUS CHRISTUS in romans 3-5 in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS CHRISTUS in romans 3-5 in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 3:22 Namelijk de rechtvaardigheid Gods door het geloof van Jezus Christus, tot allen, en over allen, die geloven; want er is geen onderscheid.') }
end

describe 'bbl search JEZUS CHRISTUS in johns letters in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS CHRISTUS in johns letters in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 JOHANNES 1:3 Hetgeen wij dan gezien en gehoord hebben, dat verkondigen wij u, opdat ook gij met ons gemeenschap zoudt hebben, en deze onze gemeenschap ook zij met den Vader, en met Zijn Zoon Jezus Christus.') }
end

describe 'bbl search JEZUS in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MATTHEÜS 1:1 Het boek des geslachts van JEZUS CHRISTUS, den Zoon van David, den zoon van Abraham.') }
end

describe 'bbl search JEZUS in romans in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS in romans in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 1:1 Paulus, een dienstknecht van Jezus Christus, een geroepen apostel, afgezonderd tot het Evangelie van God,') }
end

describe 'bbl search JEZUS in romans 2 in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS in romans 2 in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 2:16 In den dag wanneer God de verborgene dingen der mensen zal oordelen door Jezus Christus, naar mijn Evangelie.') }
end

describe 'bbl search JEZUS in romans 3-5 in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS in romans 3-5 in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 3:22 Namelijk de rechtvaardigheid Gods door het geloof van Jezus Christus, tot allen, en over allen, die geloven; want er is geen onderscheid.') }
end

describe 'bbl search JEZUS in johns letters in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search JEZUS in johns letters in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 JOHANNES 1:3 Hetgeen wij dan gezien en gehoord hebben, dat verkondigen wij u, opdat ook gij met ons gemeenschap zoudt hebben, en deze onze gemeenschap ook zij met den Vader, en met Zijn Zoon Jezus Christus.') }
end

describe 'bbl search CHRISTUS in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search CHRISTUS in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MATTHEÜS 1:1 Het boek des geslachts van JEZUS CHRISTUS, den Zoon van David, den zoon van Abraham.') }
end

describe 'bbl search CHRISTUS in romans in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search CHRISTUS in romans in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 1:1 Paulus, een dienstknecht van Jezus Christus, een geroepen apostel, afgezonderd tot het Evangelie van God,') }
end

describe 'bbl search CHRISTUS in romans 2 in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search CHRISTUS in romans 2 in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 2:16 In den dag wanneer God de verborgene dingen der mensen zal oordelen door Jezus Christus, naar mijn Evangelie.') }
end

describe 'bbl search CHRISTUS in romans 3-5 in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search CHRISTUS in romans 3-5 in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ROMEINEN 3:22 Namelijk de rechtvaardigheid Gods door het geloof van Jezus Christus, tot allen, en over allen, die geloven; want er is geen onderscheid.') }
end

describe 'bbl search CHRISTUS in johns letters in svrj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search CHRISTUS in johns letters in svrj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 JOHANNES 1:3 Hetgeen wij dan gezien en gehoord hebben, dat verkondigen wij u, opdat ook gij met ons gemeenschap zoudt hebben, en deze onze gemeenschap ook zij met den Vader, en met Zijn Zoon Jezus Christus.') }
end

# ---------------------------------------------------------------------------
# rdv24 (Italian - Giovanni Diodati 1649)
# ---------------------------------------------------------------------------

describe 'bbl search Gesù Cristo in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù Cristo in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matteo 1:1 Genealogia di Gesù Cristo figliuolo di Davide, figliuolo d\'Abramo.') }
end

describe 'bbl search Gesù Cristo in romans in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù Cristo in romans in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 1:1 Paolo, servo di Cristo Gesù, chiamato ad essere apostolo, appartato per l\'Evangelo di Dio,') }
end

describe 'bbl search Gesù Cristo in romans 2 in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù Cristo in romans 2 in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 2:16 Tutto ciò si vedrà nel giorno in cui Dio giudicherà i segreti degli uomini per mezzo di Gesù Cristo, secondo il mio Evangelo.') }
end

describe 'bbl search Gesù Cristo in romans 3-5 in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù Cristo in romans 3-5 in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 3:22 vale a dire la giustizia di Dio mediante la fede in Gesù Cristo, per tutti i credenti; poiché non v\'è distinzione;') }
end

describe 'bbl search Gesù Cristo in johns letters in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù Cristo in johns letters in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLA I DI S. GIOVANNI 1:3 quello, dico, che abbiamo veduto e udito, noi l\'annunziamo anche a voi, affinché voi pure abbiate comunione con noi, e la nostra comunione è col Padre e col suo Figliuolo, Gesù Cristo.') }
end

describe 'bbl search Gesù in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matteo 1:1 Genealogia di Gesù Cristo figliuolo di Davide, figliuolo d\'Abramo.') }
end

describe 'bbl search Gesù in romans in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù in romans in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 1:1 Paolo, servo di Cristo Gesù, chiamato ad essere apostolo, appartato per l\'Evangelo di Dio,') }
end

describe 'bbl search Gesù in romans 2 in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù in romans 2 in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 2:16 Tutto ciò si vedrà nel giorno in cui Dio giudicherà i segreti degli uomini per mezzo di Gesù Cristo, secondo il mio Evangelo.') }
end

describe 'bbl search Gesù in romans 3-5 in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù in romans 3-5 in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 3:22 vale a dire la giustizia di Dio mediante la fede in Gesù Cristo, per tutti i credenti; poiché non v\'è distinzione;') }
end

describe 'bbl search Gesù in johns letters in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Gesù in johns letters in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLA I DI S. GIOVANNI 1:3 quello, dico, che abbiamo veduto e udito, noi l\'annunziamo anche a voi, affinché voi pure abbiate comunione con noi, e la nostra comunione è col Padre e col suo Figliuolo, Gesù Cristo.') }
end

describe 'bbl search Cristo in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matteo 1:1 Genealogia di Gesù Cristo figliuolo di Davide, figliuolo d\'Abramo.') }
end

describe 'bbl search Cristo in romans in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 1:1 Paolo, servo di Cristo Gesù, chiamato ad essere apostolo, appartato per l\'Evangelo di Dio,') }
end

describe 'bbl search Cristo in romans 2 in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans 2 in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 2:16 Tutto ciò si vedrà nel giorno in cui Dio giudicherà i segreti degli uomini per mezzo di Gesù Cristo, secondo il mio Evangelo.') }
end

describe 'bbl search Cristo in romans 3-5 in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans 3-5 in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLE DI S. PAOLO AI~ROMANI 3:22 vale a dire la giustizia di Dio mediante la fede in Gesù Cristo, per tutti i credenti; poiché non v\'è distinzione;') }
end

describe 'bbl search Cristo in johns letters in rdv24 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in johns letters in rdv24') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('EPISTOLA I DI S. GIOVANNI 1:3 quello, dico, che abbiamo veduto e udito, noi l\'annunziamo anche a voi, affinché voi pure abbiate comunione con noi, e la nostra comunione è col Padre e col suo Figliuolo, Gesù Cristo.') }
end

# ---------------------------------------------------------------------------
# sven (Swedish - 1917)
# ---------------------------------------------------------------------------

describe 'bbl search Jesu Kristi in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Kristi in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matteus 1:1 Detta är Jesu Kristi, Davids sons, Abrahams sons, släkttavla.') }
end

describe 'bbl search Jesu Kristi in romans in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Kristi in romans in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 1:1 Paulus, Jesu Kristi tjänare, kallad till apostel, avskild till att förkunna Guds evangelium,') }
end

describe 'bbl search Jesu Kristi in romans 2 in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Kristi in romans 2 in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 2:16 Ja, så skall det befinnas vara på den dag då Gud, enligt det evangelium jag förkunnar, genom Kristus Jesus dömer över vad som är fördolt hos människorna.') }
end

describe 'bbl search Jesu Kristi in romans 3-5 in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Kristi in romans 3-5 in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 3:22 en rättfärdighet från Gud genom tro på Jesus Kristus, för alla dem som tro. Ty här är ingen åtskillnad.') }
end

describe 'bbl search Jesu Kristi in johns letters in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu Kristi in johns letters in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Johannesbrevet 1:3 Ja, det vi hava sett och hört, det förkunna vi ock för eder, på det att också I mån hava gemenskap med oss; och vi hava vår gemenskap med Fadern och med hans Son, Jesus Kristus.') }
end

describe 'bbl search Jesu in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matteus 1:1 Detta är Jesu Kristi, Davids sons, Abrahams sons, släkttavla.') }
end

describe 'bbl search Jesu in romans in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in romans in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 1:1 Paulus, Jesu Kristi tjänare, kallad till apostel, avskild till att förkunna Guds evangelium,') }
end

describe 'bbl search Jesu in romans 2 in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in romans 2 in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 2:16 Ja, så skall det befinnas vara på den dag då Gud, enligt det evangelium jag förkunnar, genom Kristus Jesus dömer över vad som är fördolt hos människorna.') }
end

describe 'bbl search Jesu in romans 3-5 in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in romans 3-5 in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 3:22 en rättfärdighet från Gud genom tro på Jesus Kristus, för alla dem som tro. Ty här är ingen åtskillnad.') }
end

describe 'bbl search Jesu in johns letters in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesu in johns letters in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Johannesbrevet 1:3 Ja, det vi hava sett och hört, det förkunna vi ock för eder, på det att också I mån hava gemenskap med oss; och vi hava vår gemenskap med Fadern och med hans Son, Jesus Kristus.') }
end

describe 'bbl search Kristi in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristi in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matteus 1:1 Detta är Jesu Kristi, Davids sons, Abrahams sons, släkttavla.') }
end

describe 'bbl search Kristi in romans in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristi in romans in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 1:1 Paulus, Jesu Kristi tjänare, kallad till apostel, avskild till att förkunna Guds evangelium,') }
end

describe 'bbl search Kristi in romans 2 in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristi in romans 2 in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 2:16 Ja, så skall det befinnas vara på den dag då Gud, enligt det evangelium jag förkunnar, genom Kristus Jesus dömer över vad som är fördolt hos människorna.') }
end

describe 'bbl search Kristi in romans 3-5 in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristi in romans 3-5 in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Romarbrevet 3:22 en rättfärdighet från Gud genom tro på Jesus Kristus, för alla dem som tro. Ty här är ingen åtskillnad.') }
end

describe 'bbl search Kristi in johns letters in sven exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristi in johns letters in sven') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Johannesbrevet 1:3 Ja, det vi hava sett och hört, det förkunna vi ock för eder, på det att också I mån hava gemenskap med oss; och vi hava vår gemenskap med Fadern och med hans Son, Jesus Kristus.') }
end

# ---------------------------------------------------------------------------
# jc (Japanese - Colloquial)
# ---------------------------------------------------------------------------

describe 'bbl search イエス・キリスト in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス・キリスト in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。') }
end

describe 'bbl search イエス・キリスト in romans in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス・キリスト in romans in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-') }
end

describe 'bbl search イエス・キリスト in romans 2 in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス・キリスト in romans 2 in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 2:16 そして、これらのことは、わたしの福音によれば、神がキリスト・イエスによって人々の隠れた事がらをさばかれるその日に、明らかにされるであろう。') }
end

describe 'bbl search イエス・キリスト in romans 3-5 in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス・キリスト in romans 3-5 in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 3:22 それは、イエス・キリストを信じる信仰による神の義であって、すべて信じる人に与えられるものである。そこにはなんらの差別もない。') }
end

describe 'bbl search イエス・キリスト in johns letters in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス・キリスト in johns letters in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ヨハネの第一の手紙 1:3 すなわち、わたしたちが見たもの、聞いたものを、あなたがたにも告げ知らせる。それは、あなたがたも、わたしたちの交わりにあずかるようになるためである。わたしたちの交わりとは、父ならびに御子イエス・キリストとの交わりのことである。') }
end

describe 'bbl search イエス in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。') }
end

describe 'bbl search イエス in romans in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス in romans in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-') }
end

describe 'bbl search イエス in romans 2 in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス in romans 2 in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 2:16 そして、これらのことは、わたしの福音によれば、神がキリスト・イエスによって人々の隠れた事がらをさばかれるその日に、明らかにされるであろう。') }
end

describe 'bbl search イエス in romans 3-5 in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス in romans 3-5 in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 3:22 それは、イエス・キリストを信じる信仰による神の義であって、すべて信じる人に与えられるものである。そこにはなんらの差別もない。') }
end

describe 'bbl search イエス in johns letters in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search イエス in johns letters in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ヨハネの第一の手紙 1:3 すなわち、わたしたちが見たもの、聞いたものを、あなたがたにも告げ知らせる。それは、あなたがたも、わたしたちの交わりにあずかるようになるためである。わたしたちの交わりとは、父ならびに御子イエス・キリストとの交わりのことである。') }
end

describe 'bbl search キリスト in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search キリスト in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。') }
end

describe 'bbl search キリスト in romans in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search キリスト in romans in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-') }
end

describe 'bbl search キリスト in romans 2 in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search キリスト in romans 2 in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 2:16 そして、これらのことは、わたしの福音によれば、神がキリスト・イエスによって人々の隠れた事がらをさばかれるその日に、明らかにされるであろう。') }
end

describe 'bbl search キリスト in romans 3-5 in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search キリスト in romans 3-5 in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ローマ人への手紙 3:22 それは、イエス・キリストを信じる信仰による神の義であって、すべて信じる人に与えられるものである。そこにはなんらの差別もない。') }
end

describe 'bbl search キリスト in johns letters in jc exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search キリスト in johns letters in jc') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ヨハネの第一の手紙 1:3 すなわち、わたしたちが見たもの、聞いたものを、あなたがたにも告げ知らせる。それは、あなたがたも、わたしたちの交わりにあずかるようになるためである。わたしたちの交わりとは、父ならびに御子イエス・キリストとの交わりのことである。') }
end

# ---------------------------------------------------------------------------
# ayt (Indonesian - Terjemahan Lama)
# ---------------------------------------------------------------------------

describe 'bbl search Yesus Kristus in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus Kristus in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matius 1:1 Kitab silsilah Yesus Kristus, anak Daud, anak Abraham.') }
end

describe 'bbl search Yesus Kristus in romans in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus Kristus in romans in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 1:1 Paulus, hamba Yesus Kristus, yang dipanggil menjadi rasul dan dikhususkan bagi Injil Allah;') }
end

describe 'bbl search Yesus Kristus in romans 2 in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus Kristus in romans 2 in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 2:16 pada hari ketika Allah menghakimi pikiran-pikiran manusia yang tersembunyi melalui Yesus Kristus, menurut Injilku.') }
end

describe 'bbl search Yesus Kristus in romans 3-5 in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus Kristus in romans 3-5 in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 3:22 yaitu, kebenaran Allah melalui iman kepada Kristus Yesus bagi semua yang percaya. Sebab, tidak ada perbedaan;') }
end

describe 'bbl search Yesus Kristus in johns letters in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus Kristus in johns letters in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Yohanes 1:3 Hal yang sudah kami lihat dan dengar itu, kami beritakan juga kepadamu supaya kamu juga mempunyai persekutuan bersama kami. Sesungguhnya, persekutuan kami itu adalah bersama Allah Bapa dan anak-Nya, Kristus Yesus.') }
end

describe 'bbl search Yesus in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matius 1:1 Kitab silsilah Yesus Kristus, anak Daud, anak Abraham.') }
end

describe 'bbl search Yesus in romans in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus in romans in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 1:1 Paulus, hamba Yesus Kristus, yang dipanggil menjadi rasul dan dikhususkan bagi Injil Allah;') }
end

describe 'bbl search Yesus in romans 2 in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus in romans 2 in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 2:16 pada hari ketika Allah menghakimi pikiran-pikiran manusia yang tersembunyi melalui Yesus Kristus, menurut Injilku.') }
end

describe 'bbl search Yesus in romans 3-5 in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus in romans 3-5 in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 3:22 yaitu, kebenaran Allah melalui iman kepada Kristus Yesus bagi semua yang percaya. Sebab, tidak ada perbedaan;') }
end

describe 'bbl search Yesus in johns letters in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Yesus in johns letters in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Yohanes 1:3 Hal yang sudah kami lihat dan dengar itu, kami beritakan juga kepadamu supaya kamu juga mempunyai persekutuan bersama kami. Sesungguhnya, persekutuan kami itu adalah bersama Allah Bapa dan anak-Nya, Kristus Yesus.') }
end

describe 'bbl search Kristus in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristus in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Matius 1:1 Kitab silsilah Yesus Kristus, anak Daud, anak Abraham.') }
end

describe 'bbl search Kristus in romans in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristus in romans in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 1:1 Paulus, hamba Yesus Kristus, yang dipanggil menjadi rasul dan dikhususkan bagi Injil Allah;') }
end

describe 'bbl search Kristus in romans 2 in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristus in romans 2 in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 2:16 pada hari ketika Allah menghakimi pikiran-pikiran manusia yang tersembunyi melalui Yesus Kristus, menurut Injilku.') }
end

describe 'bbl search Kristus in romans 3-5 in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristus in romans 3-5 in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('Roma 3:22 yaitu, kebenaran Allah melalui iman kepada Kristus Yesus bagi semua yang percaya. Sebab, tidak ada perbedaan;') }
end

describe 'bbl search Kristus in johns letters in ayt exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Kristus in johns letters in ayt') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 Yohanes 1:3 Hal yang sudah kami lihat dan dengar itu, kami beritakan juga kepadamu supaya kamu juga mempunyai persekutuan bersama kami. Sesungguhnya, persekutuan kami itu adalah bersama Allah Bapa dan anak-Nya, Kristus Yesus.') }
end

# ---------------------------------------------------------------------------
# th1971 (Thai - 1971)
# ---------------------------------------------------------------------------

describe 'bbl search พระเยซูคริสต์ in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซูคริสต์ in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('มัทธิว 1:1 หนังสือลำดับพงศ์ของพระเยซูคริสต์ ผู้เป็นเชื้อสายของดาวิด ผู้สืบตระกูลเนื่องมาจากอับราฮัม') }
end

describe 'bbl search พระเยซูคริสต์ in romans in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซูคริสต์ in romans in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 1:1 เปาโล ผู้รับใช้ของพระเยซูคริสต์ ผู้ซึ่งพระองค์ทรงเรียกให้เป็นอัครทูต และได้ทรงตั้งไว้ให้ประกาศข่าวประเสริฐของพระเจ้า') }
end

describe 'bbl search พระเยซูคริสต์ in romans 2 in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซูคริสต์ in romans 2 in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 2:16 ในวันที่พระเจ้าทรงพิพากษาความลับของมนุษย์โดยพระเยซูคริสต์ ทั้งนี้ตามข่าวประเสริฐที่ข้าพเจ้าได้ประกาศนั้น') }
end

describe 'bbl search พระเยซูคริสต์ in romans 3-5 in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซูคริสต์ in romans 3-5 in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 3:22 คือความชอบธรรมของพระเจ้า ซึ่งทรงประทานโดยความเชื่อในพระเยซูคริสต์ แก่ทุกคนที่เชื่อ เพราะว่าคนทั้งหลายไม่ต่างกัน') }
end

describe 'bbl search พระเยซูคริสต์ in johns letters in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซูคริสต์ in johns letters in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 ยอห์น 1:3 ซึ่งเราได้เห็นและได้ยินนั้น เราก็ได้ประกาศให้ท่านทั้งหลายรู้ด้วย เพื่อท่านทั้งหลายจะได้ร่วมสามัคคีธรรมกับเรา เราทั้งหลายก็ร่วมสามัคคีกับพระบิดา และกับพระเยซูคริสต์พระบุตรของพระองค์') }
end

describe 'bbl search พระเยซู in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซู in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('มัทธิว 1:1 หนังสือลำดับพงศ์ของพระเยซูคริสต์ ผู้เป็นเชื้อสายของดาวิด ผู้สืบตระกูลเนื่องมาจากอับราฮัม') }
end

describe 'bbl search พระเยซู in romans in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซู in romans in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 1:1 เปาโล ผู้รับใช้ของพระเยซูคริสต์ ผู้ซึ่งพระองค์ทรงเรียกให้เป็นอัครทูต และได้ทรงตั้งไว้ให้ประกาศข่าวประเสริฐของพระเจ้า') }
end

describe 'bbl search พระเยซู in romans 2 in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซู in romans 2 in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 2:16 ในวันที่พระเจ้าทรงพิพากษาความลับของมนุษย์โดยพระเยซูคริสต์ ทั้งนี้ตามข่าวประเสริฐที่ข้าพเจ้าได้ประกาศนั้น') }
end

describe 'bbl search พระเยซู in romans 3-5 in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซู in romans 3-5 in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 3:22 คือความชอบธรรมของพระเจ้า ซึ่งทรงประทานโดยความเชื่อในพระเยซูคริสต์ แก่ทุกคนที่เชื่อ เพราะว่าคนทั้งหลายไม่ต่างกัน') }
end

describe 'bbl search พระเยซู in johns letters in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search พระเยซู in johns letters in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 ยอห์น 1:3 ซึ่งเราได้เห็นและได้ยินนั้น เราก็ได้ประกาศให้ท่านทั้งหลายรู้ด้วย เพื่อท่านทั้งหลายจะได้ร่วมสามัคคีธรรมกับเรา เราทั้งหลายก็ร่วมสามัคคีกับพระบิดา และกับพระเยซูคริสต์พระบุตรของพระองค์') }
end

describe 'bbl search คริสต์ in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search คริสต์ in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('มัทธิว 1:1 หนังสือลำดับพงศ์ของพระเยซูคริสต์ ผู้เป็นเชื้อสายของดาวิด ผู้สืบตระกูลเนื่องมาจากอับราฮัม') }
end

describe 'bbl search คริสต์ in romans in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search คริสต์ in romans in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 1:1 เปาโล ผู้รับใช้ของพระเยซูคริสต์ ผู้ซึ่งพระองค์ทรงเรียกให้เป็นอัครทูต และได้ทรงตั้งไว้ให้ประกาศข่าวประเสริฐของพระเจ้า') }
end

describe 'bbl search คริสต์ in romans 2 in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search คริสต์ in romans 2 in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 2:16 ในวันที่พระเจ้าทรงพิพากษาความลับของมนุษย์โดยพระเยซูคริสต์ ทั้งนี้ตามข่าวประเสริฐที่ข้าพเจ้าได้ประกาศนั้น') }
end

describe 'bbl search คริสต์ in romans 3-5 in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search คริสต์ in romans 3-5 in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('โรม 3:22 คือความชอบธรรมของพระเจ้า ซึ่งทรงประทานโดยความเชื่อในพระเยซูคริสต์ แก่ทุกคนที่เชื่อ เพราะว่าคนทั้งหลายไม่ต่างกัน') }
end

describe 'bbl search คริสต์ in johns letters in th1971 exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search คริสต์ in johns letters in th1971') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 ยอห์น 1:3 ซึ่งเราได้เห็นและได้ยินนั้น เราก็ได้ประกาศให้ท่านทั้งหลายรู้ด้วย เพื่อท่านทั้งหลายจะได้ร่วมสามัคคีธรรมกับเรา เราทั้งหลายก็ร่วมสามัคคีกับพระบิดา และกับพระเยซูคริสต์พระบุตรของพระองค์') }
end

# ---------------------------------------------------------------------------
# irvhin (Hindi - Indian Revised Version)
# ---------------------------------------------------------------------------

describe 'bbl search यीशु मसीह in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु मसीह in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्ती 1:1 अब्राहम की सन्तान, दाऊद की सन्तान, यीशु मसीह की वंशावली ।') }
end

describe 'bbl search यीशु मसीह in romans in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु मसीह in romans in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 1:1 पौलुस  की ओर से जो यीशु मसीह का दास है, और प्रेरित होने के लिये बुलाया गया, और परमेश्वर के उस सुसमाचार के लिये अलग किया गया है') }
end

describe 'bbl search यीशु मसीह in romans 2 in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु मसीह in romans 2 in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 2:16 जिस दिन परमेश्वर मेरे सुसमाचार के अनुसार यीशु मसीह के द्वारा मनुष्यों की गुप्त बातों का न्याय करेगा।') }
end

describe 'bbl search यीशु मसीह in romans 3-5 in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु मसीह in romans 3-5 in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 3:22 अर्थात् परमेश्वर की वह धार्मिकता, जो यीशु मसीह पर विश्वास करने से सब विश्वास करनेवालों के लिये है। क्योंकि कुछ भेद नहीं;') }
end

describe 'bbl search यीशु मसीह in johns letters in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु मसीह in johns letters in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 यूहन्ना 1:3 जो कुछ हमने देखा और सुना है उसका समाचार तुम्हें भी देते हैं, इसलिए कि तुम भी हमारे साथ सहभागी हो; और हमारी यह सहभागिता पिता के साथ, और उसके पुत्र यीशु मसीह के साथ है।') }
end

describe 'bbl search यीशु in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्ती 1:1 अब्राहम की सन्तान, दाऊद की सन्तान, यीशु मसीह की वंशावली ।') }
end

describe 'bbl search यीशु in romans in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु in romans in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 1:1 पौलुस  की ओर से जो यीशु मसीह का दास है, और प्रेरित होने के लिये बुलाया गया, और परमेश्वर के उस सुसमाचार के लिये अलग किया गया है') }
end

describe 'bbl search यीशु in romans 2 in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु in romans 2 in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 2:16 जिस दिन परमेश्वर मेरे सुसमाचार के अनुसार यीशु मसीह के द्वारा मनुष्यों की गुप्त बातों का न्याय करेगा।') }
end

describe 'bbl search यीशु in romans 3-5 in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु in romans 3-5 in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 3:22 अर्थात् परमेश्वर की वह धार्मिकता, जो यीशु मसीह पर विश्वास करने से सब विश्वास करनेवालों के लिये है। क्योंकि कुछ भेद नहीं;') }
end

describe 'bbl search यीशु in johns letters in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search यीशु in johns letters in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 यूहन्ना 1:3 जो कुछ हमने देखा और सुना है उसका समाचार तुम्हें भी देते हैं, इसलिए कि तुम भी हमारे साथ सहभागी हो; और हमारी यह सहभागिता पिता के साथ, और उसके पुत्र यीशु मसीह के साथ है।') }
end

describe 'bbl search मसीह in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्ती 1:1 अब्राहम की सन्तान, दाऊद की सन्तान, यीशु मसीह की वंशावली ।') }
end

describe 'bbl search मसीह in romans in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in romans in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 1:1 पौलुस  की ओर से जो यीशु मसीह का दास है, और प्रेरित होने के लिये बुलाया गया, और परमेश्वर के उस सुसमाचार के लिये अलग किया गया है') }
end

describe 'bbl search मसीह in romans 2 in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in romans 2 in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 2:16 जिस दिन परमेश्वर मेरे सुसमाचार के अनुसार यीशु मसीह के द्वारा मनुष्यों की गुप्त बातों का न्याय करेगा।') }
end

describe 'bbl search मसीह in romans 3-5 in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in romans 3-5 in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमियों 3:22 अर्थात् परमेश्वर की वह धार्मिकता, जो यीशु मसीह पर विश्वास करने से सब विश्वास करनेवालों के लिये है। क्योंकि कुछ भेद नहीं;') }
end

describe 'bbl search मसीह in johns letters in irvhin exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in johns letters in irvhin') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 यूहन्ना 1:3 जो कुछ हमने देखा और सुना है उसका समाचार तुम्हें भी देते हैं, इसलिए कि तुम भी हमारे साथ सहभागी हो; और हमारी यह सहभागिता पिता के साथ, और उसके पुत्र यीशु मसीह के साथ है।') }
end

# ---------------------------------------------------------------------------
# irvben (Bengali - Indian Revised Version)
# ---------------------------------------------------------------------------

describe 'bbl search যীশু খ্রীষ্ট in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু খ্রীষ্ট in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('মথি 1:1 যীশু খ্রীষ্টের বংশ তালিকা, তিনি দায়ূদের সন্তান, অব্রাহামের সন্তান।') }
end

describe 'bbl search যীশু খ্রীষ্ট in romans in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু খ্রীষ্ট in romans in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 1:1 পৌল, একজন যীশু খ্রীষ্টের দাস, প্রেরিত হবার জন্য ডাকা হয়েছে এবং ঈশ্বরের সুসমাচার প্রচারের জন্য আলাদা ভাবে মনোনীত করেছেন,') }
end

describe 'bbl search যীশু খ্রীষ্ট in romans 2 in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু খ্রীষ্ট in romans 2 in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 2:16 যে দিন ঈশ্বর আমার প্রচারিত সুসমাচার অনুযায়ী খ্রীষ্ট যীশুর মাধ্যমে মানুষদের গোপন বিষয়গুলি বিচার করবেন।') }
end

describe 'bbl search যীশু খ্রীষ্ট in romans 3-5 in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু খ্রীষ্ট in romans 3-5 in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 3:22 ঈশ্বরের সেই ধার্ম্মিকতা যীশু খ্রীষ্টে বিশ্বাসের মাধ্যমে যারা সবাই বিশ্বাস করে তাদের জন্য। কারণ সেখানে কোনো বিভেদ নেই।') }
end

describe 'bbl search যীশু খ্রীষ্ট in johns letters in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু খ্রীষ্ট in johns letters in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 যোহন 1:3 আমরা যাকে দেখেছি ও শুনেছি, তার খবর তোমাদেরকেও দিচ্ছি, যেন আমাদের সঙ্গে তোমাদেরও সহভাগীতা হয়। আর আমাদের সহভাগীতা হল পিতার এবং তাঁর পুত্র যীশু খ্রীষ্টের সহভাগীতা।') }
end

describe 'bbl search যীশু in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('মথি 1:1 যীশু খ্রীষ্টের বংশ তালিকা, তিনি দায়ূদের সন্তান, অব্রাহামের সন্তান।') }
end

describe 'bbl search যীশু in romans in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু in romans in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 1:1 পৌল, একজন যীশু খ্রীষ্টের দাস, প্রেরিত হবার জন্য ডাকা হয়েছে এবং ঈশ্বরের সুসমাচার প্রচারের জন্য আলাদা ভাবে মনোনীত করেছেন,') }
end

describe 'bbl search যীশু in romans 2 in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু in romans 2 in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 2:16 যে দিন ঈশ্বর আমার প্রচারিত সুসমাচার অনুযায়ী খ্রীষ্ট যীশুর মাধ্যমে মানুষদের গোপন বিষয়গুলি বিচার করবেন।') }
end

describe 'bbl search যীশু in romans 3-5 in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু in romans 3-5 in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 3:22 ঈশ্বরের সেই ধার্ম্মিকতা যীশু খ্রীষ্টে বিশ্বাসের মাধ্যমে যারা সবাই বিশ্বাস করে তাদের জন্য। কারণ সেখানে কোনো বিভেদ নেই।') }
end

describe 'bbl search যীশু in johns letters in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search যীশু in johns letters in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 যোহন 1:3 আমরা যাকে দেখেছি ও শুনেছি, তার খবর তোমাদেরকেও দিচ্ছি, যেন আমাদের সঙ্গে তোমাদেরও সহভাগীতা হয়। আর আমাদের সহভাগীতা হল পিতার এবং তাঁর পুত্র যীশু খ্রীষ্টের সহভাগীতা।') }
end

describe 'bbl search খ্রীষ্ট in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search খ্রীষ্ট in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('মথি 1:1 যীশু খ্রীষ্টের বংশ তালিকা, তিনি দায়ূদের সন্তান, অব্রাহামের সন্তান।') }
end

describe 'bbl search খ্রীষ্ট in romans in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search খ্রীষ্ট in romans in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 1:1 পৌল, একজন যীশু খ্রীষ্টের দাস, প্রেরিত হবার জন্য ডাকা হয়েছে এবং ঈশ্বরের সুসমাচার প্রচারের জন্য আলাদা ভাবে মনোনীত করেছেন,') }
end

describe 'bbl search খ্রীষ্ট in romans 2 in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search খ্রীষ্ট in romans 2 in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 2:16 যে দিন ঈশ্বর আমার প্রচারিত সুসমাচার অনুযায়ী খ্রীষ্ট যীশুর মাধ্যমে মানুষদের গোপন বিষয়গুলি বিচার করবেন।') }
end

describe 'bbl search খ্রীষ্ট in romans 3-5 in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search খ্রীষ্ট in romans 3-5 in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('রোমীয় 3:22 ঈশ্বরের সেই ধার্ম্মিকতা যীশু খ্রীষ্টে বিশ্বাসের মাধ্যমে যারা সবাই বিশ্বাস করে তাদের জন্য। কারণ সেখানে কোনো বিভেদ নেই।') }
end

describe 'bbl search খ্রীষ্ট in johns letters in irvben exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search খ্রীষ্ট in johns letters in irvben') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 যোহন 1:3 আমরা যাকে দেখেছি ও শুনেছি, তার খবর তোমাদেরকেও দিচ্ছি, যেন আমাদের সঙ্গে তোমাদেরও সহভাগীতা হয়। আর আমাদের সহভাগীতা হল পিতার এবং তাঁর পুত্র যীশু খ্রীষ্টের সহভাগীতা।') }
end

# ---------------------------------------------------------------------------
# irvtam (Tamil - Indian Revised Version)
# ---------------------------------------------------------------------------

describe 'bbl search இயேசுகிறிஸ்து in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசுகிறிஸ்து in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('மத் 1:1 ஆபிரகாமின் மகனாகிய தாவீதின் குமாரனான இயேசுகிறிஸ்துவின் வம்சவரலாறு:') }
end

describe 'bbl search இயேசுகிறிஸ்து in romans in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசுகிறிஸ்து in romans in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 1:1 இயேசுகிறிஸ்துவின் ஊழியக்காரனும், அப்போஸ்தலனாக இருப்பதற்காக அழைக்கப்பட்டவனும், தேவனுடைய நற்செய்திக்காகப் பிரித்தெடுக்கப்பட்டவனுமாகிய பவுல்,') }
end

describe 'bbl search இயேசுகிறிஸ்து in romans 2 in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசுகிறிஸ்து in romans 2 in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 2:16 என்னுடைய நற்செய்தியின்படியே, தேவன் இயேசுகிறிஸ்துவைக்கொண்டு மனிதர்களுடைய இரகசியங்களைக்குறித்து நியாயத்தீர்ப்புக்கொடுக்கும் நாளிலே இது விளங்கும்.') }
end

describe 'bbl search இயேசுகிறிஸ்து in romans 3-5 in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசுகிறிஸ்து in romans 3-5 in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 3:22 அது இயேசுகிறிஸ்துவை விசுவாசிக்கும் விசுவாசத்தினாலே வரும் தேவநீதியே; விசுவாசிக்கிற எல்லோருக்குள்ளும் எவர்கள் மேலும் அது வரும், வித்தியாசமே இல்லை.') }
end

describe 'bbl search இயேசுகிறிஸ்து in johns letters in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசுகிறிஸ்து in johns letters in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 யோவா 1:3 நீங்களும் எங்களோடு ஐக்கியம் உள்ளவர்களாகும்படி, நாங்கள் பார்த்தும் கேட்டும் இருக்கிறதை உங்களுக்கும் அறிவிக்கிறோம்; எங்களுடைய ஐக்கியம் பிதாவோடும் அவருடைய குமாரனாகிய இயேசுகிறிஸ்துவோடும் இருக்கிறது.') }
end

describe 'bbl search இயேசு in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசு in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('மத் 1:1 ஆபிரகாமின் மகனாகிய தாவீதின் குமாரனான இயேசுகிறிஸ்துவின் வம்சவரலாறு:') }
end

describe 'bbl search இயேசு in romans in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசு in romans in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 1:1 இயேசுகிறிஸ்துவின் ஊழியக்காரனும், அப்போஸ்தலனாக இருப்பதற்காக அழைக்கப்பட்டவனும், தேவனுடைய நற்செய்திக்காகப் பிரித்தெடுக்கப்பட்டவனுமாகிய பவுல்,') }
end

describe 'bbl search இயேசு in romans 2 in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசு in romans 2 in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 2:16 என்னுடைய நற்செய்தியின்படியே, தேவன் இயேசுகிறிஸ்துவைக்கொண்டு மனிதர்களுடைய இரகசியங்களைக்குறித்து நியாயத்தீர்ப்புக்கொடுக்கும் நாளிலே இது விளங்கும்.') }
end

describe 'bbl search இயேசு in romans 3-5 in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசு in romans 3-5 in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 3:22 அது இயேசுகிறிஸ்துவை விசுவாசிக்கும் விசுவாசத்தினாலே வரும் தேவநீதியே; விசுவாசிக்கிற எல்லோருக்குள்ளும் எவர்கள் மேலும் அது வரும், வித்தியாசமே இல்லை.') }
end

describe 'bbl search இயேசு in johns letters in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search இயேசு in johns letters in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 யோவா 1:3 நீங்களும் எங்களோடு ஐக்கியம் உள்ளவர்களாகும்படி, நாங்கள் பார்த்தும் கேட்டும் இருக்கிறதை உங்களுக்கும் அறிவிக்கிறோம்; எங்களுடைய ஐக்கியம் பிதாவோடும் அவருடைய குமாரனாகிய இயேசுகிறிஸ்துவோடும் இருக்கிறது.') }
end

describe 'bbl search கிறிஸ்து in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search கிறிஸ்து in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('மத் 1:1 ஆபிரகாமின் மகனாகிய தாவீதின் குமாரனான இயேசுகிறிஸ்துவின் வம்சவரலாறு:') }
end

describe 'bbl search கிறிஸ்து in romans in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search கிறிஸ்து in romans in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 1:1 இயேசுகிறிஸ்துவின் ஊழியக்காரனும், அப்போஸ்தலனாக இருப்பதற்காக அழைக்கப்பட்டவனும், தேவனுடைய நற்செய்திக்காகப் பிரித்தெடுக்கப்பட்டவனுமாகிய பவுல்,') }
end

describe 'bbl search கிறிஸ்து in romans 2 in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search கிறிஸ்து in romans 2 in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 2:16 என்னுடைய நற்செய்தியின்படியே, தேவன் இயேசுகிறிஸ்துவைக்கொண்டு மனிதர்களுடைய இரகசியங்களைக்குறித்து நியாயத்தீர்ப்புக்கொடுக்கும் நாளிலே இது விளங்கும்.') }
end

describe 'bbl search கிறிஸ்து in romans 3-5 in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search கிறிஸ்து in romans 3-5 in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ரோமர் 3:22 அது இயேசுகிறிஸ்துவை விசுவாசிக்கும் விசுவாசத்தினாலே வரும் தேவநீதியே; விசுவாசிக்கிற எல்லோருக்குள்ளும் எவர்கள் மேலும் அது வரும், வித்தியாசமே இல்லை.') }
end

describe 'bbl search கிறிஸ்து in johns letters in irvtam exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search கிறிஸ்து in johns letters in irvtam') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 யோவா 1:3 நீங்களும் எங்களோடு ஐக்கியம் உள்ளவர்களாகும்படி, நாங்கள் பார்த்தும் கேட்டும் இருக்கிறதை உங்களுக்கும் அறிவிக்கிறோம்; எங்களுடைய ஐக்கியம் பிதாவோடும் அவருடைய குமாரனாகிய இயேசுகிறிஸ்துவோடும் இருக்கிறது.') }
end

# ---------------------------------------------------------------------------
# npiulb (Nepali - Nepali Bible)
# ---------------------------------------------------------------------------

describe 'bbl search येशू ख्रीष्‍ट in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रीष्‍ट in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्ती 1:1 दाऊदका पुत्र अब्राहामका पुत्र येशू ख्रीष्‍टको वंशावलीको पुस्तक ।') }
end

describe 'bbl search येशू ख्रीष्‍ट in romans in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रीष्‍ट in romans in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 1:1 प्रेरित हुनको निम्ति बोलाइएका र सुसमाचारको कामको निम्ति अलग गरिएका, येशू ख्रीष्‍टका दास पावल ।') }
end

describe 'bbl search येशू ख्रीष्‍ट in romans 2 in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रीष्‍ट in romans 2 in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 2:16 अनि परमेश्‍वरप्रति पनि यही कुरो लागु हुन्छ । त्यो मेरो सुसमाचारअनुसार येशू ख्रीष्‍टद्वारा परमेश्‍वरले सबै मानिसहरूको गोप्य कुराहरूको इन्साफ गर्नुहुने दिन हुनेछ ।') }
end

describe 'bbl search येशू ख्रीष्‍ट in romans 3-5 in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रीष्‍ट in romans 3-5 in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 3:22 अर्थात् यो विश्‍वास गर्ने सबैका निम्ति येशू ख्रीष्‍टमा विश्‍वासद्वारा आउने परमेश्‍वरको धार्मिकता हो । किनकि त्यहाँ कुनै भेदभाव छैन ।') }
end

describe 'bbl search येशू ख्रीष्‍ट in johns letters in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रीष्‍ट in johns letters in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('१ यूहन्ना 1:3 जुन हामीले देखेका र सुनेका छौँ, हामी तिमीहरूलाई पनि घोषणा गर्छौं, ताकि हामीहरूसँग तिमीहरूको सङ्गति होस् । हाम्रो सङ्गति पिता र उहाँको पुत्र येशू ख्रीष्‍टसँग हुन्छ ।') }
end

describe 'bbl search येशू in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्ती 1:1 दाऊदका पुत्र अब्राहामका पुत्र येशू ख्रीष्‍टको वंशावलीको पुस्तक ।') }
end

describe 'bbl search येशू in romans in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in romans in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 1:1 प्रेरित हुनको निम्ति बोलाइएका र सुसमाचारको कामको निम्ति अलग गरिएका, येशू ख्रीष्‍टका दास पावल ।') }
end

describe 'bbl search येशू in romans 2 in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in romans 2 in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 2:16 अनि परमेश्‍वरप्रति पनि यही कुरो लागु हुन्छ । त्यो मेरो सुसमाचारअनुसार येशू ख्रीष्‍टद्वारा परमेश्‍वरले सबै मानिसहरूको गोप्य कुराहरूको इन्साफ गर्नुहुने दिन हुनेछ ।') }
end

describe 'bbl search येशू in romans 3-5 in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in romans 3-5 in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 3:22 अर्थात् यो विश्‍वास गर्ने सबैका निम्ति येशू ख्रीष्‍टमा विश्‍वासद्वारा आउने परमेश्‍वरको धार्मिकता हो । किनकि त्यहाँ कुनै भेदभाव छैन ।') }
end

describe 'bbl search येशू in johns letters in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in johns letters in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('१ यूहन्ना 1:3 जुन हामीले देखेका र सुनेका छौँ, हामी तिमीहरूलाई पनि घोषणा गर्छौं, ताकि हामीहरूसँग तिमीहरूको सङ्गति होस् । हाम्रो सङ्गति पिता र उहाँको पुत्र येशू ख्रीष्‍टसँग हुन्छ ।') }
end

describe 'bbl search ख्रीष्‍ट in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रीष्‍ट in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्ती 1:1 दाऊदका पुत्र अब्राहामका पुत्र येशू ख्रीष्‍टको वंशावलीको पुस्तक ।') }
end

describe 'bbl search ख्रीष्‍ट in romans in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रीष्‍ट in romans in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 1:1 प्रेरित हुनको निम्ति बोलाइएका र सुसमाचारको कामको निम्ति अलग गरिएका, येशू ख्रीष्‍टका दास पावल ।') }
end

describe 'bbl search ख्रीष्‍ट in romans 2 in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रीष्‍ट in romans 2 in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 2:16 अनि परमेश्‍वरप्रति पनि यही कुरो लागु हुन्छ । त्यो मेरो सुसमाचारअनुसार येशू ख्रीष्‍टद्वारा परमेश्‍वरले सबै मानिसहरूको गोप्य कुराहरूको इन्साफ गर्नुहुने दिन हुनेछ ।') }
end

describe 'bbl search ख्रीष्‍ट in romans 3-5 in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रीष्‍ट in romans 3-5 in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमी 3:22 अर्थात् यो विश्‍वास गर्ने सबैका निम्ति येशू ख्रीष्‍टमा विश्‍वासद्वारा आउने परमेश्‍वरको धार्मिकता हो । किनकि त्यहाँ कुनै भेदभाव छैन ।') }
end

describe 'bbl search ख्रीष्‍ट in johns letters in npiulb exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रीष्‍ट in johns letters in npiulb') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('१ यूहन्ना 1:3 जुन हामीले देखेका र सुनेका छौँ, हामी तिमीहरूलाई पनि घोषणा गर्छौं, ताकि हामीहरूसँग तिमीहरूको सङ्गति होस् । हाम्रो सङ्गति पिता र उहाँको पुत्र येशू ख्रीष्‍टसँग हुन्छ ।') }
end

# ---------------------------------------------------------------------------
# abtag (Tagalog - Ang Biblia)
# ---------------------------------------------------------------------------

describe 'bbl search Jesucristo in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MATEO 1:1 Ang aklat ng lahi ni Jesucristo, na anak ni David, na anak ni Abraham.') }
end

describe 'bbl search Jesucristo in romans in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in romans in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 1:1 Si Pablo na alipin ni Jesucristo, na tinawag na maging apostol, ibinukod sa evangelio ng Dios,') }
end

describe 'bbl search Jesucristo in romans 2 in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in romans 2 in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 2:16 Sa araw na hahatulan ng Dios ang mga lihim ng mga tao, ayon sa aking evangelio, sa pamamagitan ni Jesucristo.') }
end

describe 'bbl search Jesucristo in romans 3-5 in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in romans 3-5 in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 3:22 Sa makatuwid baga\'y ang katuwiran ng Dios sa pamamagitan ng pananampalataya kay Jesucristo sa lahat ng mga nagsisisampalataya; sapagka\'t walang pagkakaiba;') }
end

describe 'bbl search Jesucristo in johns letters in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesucristo in johns letters in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('I JUAN 1:3 Yaong aming nakita at narinig ay siya rin naming ibinabalita sa inyo, upang kayo naman ay magkaroon ng pakikisama sa amin: oo, at tayo ay may pakikisama sa Ama, at sa kaniyang Anak na si Jesucristo:') }
end

describe 'bbl search Jesus in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MATEO 1:1 Ang aklat ng lahi ni Jesucristo, na anak ni David, na anak ni Abraham.') }
end

describe 'bbl search Jesus in romans in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in romans in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 1:1 Si Pablo na alipin ni Jesucristo, na tinawag na maging apostol, ibinukod sa evangelio ng Dios,') }
end

describe 'bbl search Jesus in romans 2 in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in romans 2 in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 2:16 Sa araw na hahatulan ng Dios ang mga lihim ng mga tao, ayon sa aking evangelio, sa pamamagitan ni Jesucristo.') }
end

describe 'bbl search Jesus in romans 3-5 in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in romans 3-5 in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 3:22 Sa makatuwid baga\'y ang katuwiran ng Dios sa pamamagitan ng pananampalataya kay Jesucristo sa lahat ng mga nagsisisampalataya; sapagka\'t walang pagkakaiba;') }
end

describe 'bbl search Jesus in johns letters in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Jesus in johns letters in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('I JUAN 1:3 Yaong aming nakita at narinig ay siya rin naming ibinabalita sa inyo, upang kayo naman ay magkaroon ng pakikisama sa amin: oo, at tayo ay may pakikisama sa Ama, at sa kaniyang Anak na si Jesucristo:') }
end

describe 'bbl search Cristo in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MATEO 1:1 Ang aklat ng lahi ni Jesucristo, na anak ni David, na anak ni Abraham.') }
end

describe 'bbl search Cristo in romans in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 1:1 Si Pablo na alipin ni Jesucristo, na tinawag na maging apostol, ibinukod sa evangelio ng Dios,') }
end

describe 'bbl search Cristo in romans 2 in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans 2 in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 2:16 Sa araw na hahatulan ng Dios ang mga lihim ng mga tao, ayon sa aking evangelio, sa pamamagitan ni Jesucristo.') }
end

describe 'bbl search Cristo in romans 3-5 in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in romans 3-5 in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('MGA TAGA ROMA 3:22 Sa makatuwid baga\'y ang katuwiran ng Dios sa pamamagitan ng pananampalataya kay Jesucristo sa lahat ng mga nagsisisampalataya; sapagka\'t walang pagkakaiba;') }
end

describe 'bbl search Cristo in johns letters in abtag exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search Cristo in johns letters in abtag') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('I JUAN 1:3 Yaong aming nakita at narinig ay siya rin naming ibinabalita sa inyo, upang kayo naman ay magkaroon ng pakikisama sa amin: oo, at tayo ay may pakikisama sa Ama, at sa kaniyang Anak na si Jesucristo:') }
end

# ---------------------------------------------------------------------------
# irvguj (Gujarati - Indian Revised Version)
# ---------------------------------------------------------------------------

describe 'bbl search ઈસુ ખ્રિસ્ત in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ ખ્રિસ્ત in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('માથ. 1:1 ઈસુ ખ્રિસ્ત જે ઇબ્રાહિમનાં દીકરા, જે દાઉદના દીકરા, તેમની વંશાવળી.') }
end

describe 'bbl search ઈસુ ખ્રિસ્ત in romans in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ ખ્રિસ્ત in romans in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 1:1 પ્રેરિત થવા સારુ તેડાયેલો અને ઈશ્વરની સુવાર્તા માટે અલગ કરાયેલો ઈસુ ખ્રિસ્તનો સેવક પાઉલ, રોમમાં રહેતા, ઈશ્વરના વહાલા અને પવિત્ર થવા સારુ પસંદ કરાયેલા સર્વ લોકોને લખે છે') }
end

describe 'bbl search ઈસુ ખ્રિસ્ત in romans 2 in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ ખ્રિસ્ત in romans 2 in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 2:16 ઈશ્વર મારી સુવાર્તા પ્રમાણે ઈસુ ખ્રિસ્તની મારફતે મનુષ્યોના ગુપ્ત કામોનો ન્યાય કરશે, તે દિવસે એમ થશે.') }
end

describe 'bbl search ઈસુ ખ્રિસ્ત in romans 3-5 in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ ખ્રિસ્ત in romans 3-5 in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 3:22 એટલે ઈશ્વરનું ન્યાયીપણું, જે ઈસુ ખ્રિસ્ત પરના વિશ્વાસદ્વારા સર્વ વિશ્વાસ કરનારાઓને માટે છે તે; કેમ કે એમાં કંઈ પણ તફાવત નથી.') }
end

describe 'bbl search ઈસુ ખ્રિસ્ત in johns letters in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ ખ્રિસ્ત in johns letters in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 યોહ. 1:3 હા, અમારી સાથે તમારી પણ સંગત થાય, એ માટે જે અમે જોયું તથા સાંભળ્યું છે, તે તમને પણ જાહેર કરીએ છીએ; અને ખરેખર અમારી સંગત પિતાની સાથે તથા તેમના પુત્ર ઈસુ ખ્રિસ્તની સાથે છે.') }
end

describe 'bbl search ઈસુ in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('માથ. 1:1 ઈસુ ખ્રિસ્ત જે ઇબ્રાહિમનાં દીકરા, જે દાઉદના દીકરા, તેમની વંશાવળી.') }
end

describe 'bbl search ઈસુ in romans in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ in romans in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 1:1 પ્રેરિત થવા સારુ તેડાયેલો અને ઈશ્વરની સુવાર્તા માટે અલગ કરાયેલો ઈસુ ખ્રિસ્તનો સેવક પાઉલ, રોમમાં રહેતા, ઈશ્વરના વહાલા અને પવિત્ર થવા સારુ પસંદ કરાયેલા સર્વ લોકોને લખે છે') }
end

describe 'bbl search ઈસુ in romans 2 in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ in romans 2 in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 2:16 ઈશ્વર મારી સુવાર્તા પ્રમાણે ઈસુ ખ્રિસ્તની મારફતે મનુષ્યોના ગુપ્ત કામોનો ન્યાય કરશે, તે દિવસે એમ થશે.') }
end

describe 'bbl search ઈસુ in romans 3-5 in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ in romans 3-5 in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 3:22 એટલે ઈશ્વરનું ન્યાયીપણું, જે ઈસુ ખ્રિસ્ત પરના વિશ્વાસદ્વારા સર્વ વિશ્વાસ કરનારાઓને માટે છે તે; કેમ કે એમાં કંઈ પણ તફાવત નથી.') }
end

describe 'bbl search ઈસુ in johns letters in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ઈસુ in johns letters in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 યોહ. 1:3 હા, અમારી સાથે તમારી પણ સંગત થાય, એ માટે જે અમે જોયું તથા સાંભળ્યું છે, તે તમને પણ જાહેર કરીએ છીએ; અને ખરેખર અમારી સંગત પિતાની સાથે તથા તેમના પુત્ર ઈસુ ખ્રિસ્તની સાથે છે.') }
end

describe 'bbl search ખ્રિસ્ત in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ખ્રિસ્ત in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('માથ. 1:1 ઈસુ ખ્રિસ્ત જે ઇબ્રાહિમનાં દીકરા, જે દાઉદના દીકરા, તેમની વંશાવળી.') }
end

describe 'bbl search ખ્રિસ્ત in romans in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ખ્રિસ્ત in romans in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 1:1 પ્રેરિત થવા સારુ તેડાયેલો અને ઈશ્વરની સુવાર્તા માટે અલગ કરાયેલો ઈસુ ખ્રિસ્તનો સેવક પાઉલ, રોમમાં રહેતા, ઈશ્વરના વહાલા અને પવિત્ર થવા સારુ પસંદ કરાયેલા સર્વ લોકોને લખે છે') }
end

describe 'bbl search ખ્રિસ્ત in romans 2 in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ખ્રિસ્ત in romans 2 in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 2:16 ઈશ્વર મારી સુવાર્તા પ્રમાણે ઈસુ ખ્રિસ્તની મારફતે મનુષ્યોના ગુપ્ત કામોનો ન્યાય કરશે, તે દિવસે એમ થશે.') }
end

describe 'bbl search ખ્રિસ્ત in romans 3-5 in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ખ્રિસ્ત in romans 3-5 in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('રોમ. 3:22 એટલે ઈશ્વરનું ન્યાયીપણું, જે ઈસુ ખ્રિસ્ત પરના વિશ્વાસદ્વારા સર્વ વિશ્વાસ કરનારાઓને માટે છે તે; કેમ કે એમાં કંઈ પણ તફાવત નથી.') }
end

describe 'bbl search ખ્રિસ્ત in johns letters in irvguj exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ખ્રિસ્ત in johns letters in irvguj') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 યોહ. 1:3 હા, અમારી સાથે તમારી પણ સંગત થાય, એ માટે જે અમે જોયું તથા સાંભળ્યું છે, તે તમને પણ જાહેર કરીએ છીએ; અને ખરેખર અમારી સંગત પિતાની સાથે તથા તેમના પુત્ર ઈસુ ખ્રિસ્તની સાથે છે.') }
end

# ---------------------------------------------------------------------------
# irvmar (Marathi - Indian Revised Version)
# ---------------------------------------------------------------------------

describe 'bbl search येशू ख्रिस्त in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रिस्त in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्त. 1:1 अब्राहामाचा पुत्र दावीद याचा पुत्र जो येशू ख्रिस्त याची वंशावळ.') }
end

describe 'bbl search येशू ख्रिस्त in romans in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रिस्त in romans in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 1:1 प्रेषित होण्यास बोलावलेला, येशू ख्रिस्ताचा दास, देवाच्या सुवार्तेसाठी वेगळा केलेला, पौल ह्याजकडून;') }
end

describe 'bbl search येशू ख्रिस्त in romans 2 in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रिस्त in romans 2 in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 2:16 देव, माझ्या सुवार्तेप्रमाणे जेव्हा मनुष्यांच्या गुप्त गोष्टींचा ख्रिस्त येशूकडून न्याय करील त्यादिवशी हे दिसून येईल.') }
end

describe 'bbl search येशू ख्रिस्त in romans 3-5 in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रिस्त in romans 3-5 in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 3:22 पण हे देवाचे नीतिमत्त्व येशू ख्रिस्तावरील विश्वासाद्वारे, विश्वास ठेवणार्‍या सर्वांसाठी आहे कारण तेथे कसलाही फरक नाही.') }
end

describe 'bbl search येशू ख्रिस्त in johns letters in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू ख्रिस्त in johns letters in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 योहा. 1:3 आम्ही जे पाहिले व ऐकले आहे ते आम्ही आता तुम्हांलाही घोषित करीत आहोत, यासाठी की तुमचीही आमच्यासोबत सहभागिता असावी. आमची सहभागिता तर देवपिता व त्याचा पुत्र येशू ख्रिस्त याजबरोबर आहे.') }
end

describe 'bbl search येशू in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्त. 1:1 अब्राहामाचा पुत्र दावीद याचा पुत्र जो येशू ख्रिस्त याची वंशावळ.') }
end

describe 'bbl search येशू in romans in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in romans in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 1:1 प्रेषित होण्यास बोलावलेला, येशू ख्रिस्ताचा दास, देवाच्या सुवार्तेसाठी वेगळा केलेला, पौल ह्याजकडून;') }
end

describe 'bbl search येशू in romans 2 in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in romans 2 in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 2:16 देव, माझ्या सुवार्तेप्रमाणे जेव्हा मनुष्यांच्या गुप्त गोष्टींचा ख्रिस्त येशूकडून न्याय करील त्यादिवशी हे दिसून येईल.') }
end

describe 'bbl search येशू in romans 3-5 in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in romans 3-5 in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 3:22 पण हे देवाचे नीतिमत्त्व येशू ख्रिस्तावरील विश्वासाद्वारे, विश्वास ठेवणार्‍या सर्वांसाठी आहे कारण तेथे कसलाही फरक नाही.') }
end

describe 'bbl search येशू in johns letters in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search येशू in johns letters in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 योहा. 1:3 आम्ही जे पाहिले व ऐकले आहे ते आम्ही आता तुम्हांलाही घोषित करीत आहोत, यासाठी की तुमचीही आमच्यासोबत सहभागिता असावी. आमची सहभागिता तर देवपिता व त्याचा पुत्र येशू ख्रिस्त याजबरोबर आहे.') }
end

describe 'bbl search ख्रिस्त in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रिस्त in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्त. 1:1 अब्राहामाचा पुत्र दावीद याचा पुत्र जो येशू ख्रिस्त याची वंशावळ.') }
end

describe 'bbl search ख्रिस्त in romans in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रिस्त in romans in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 1:1 प्रेषित होण्यास बोलावलेला, येशू ख्रिस्ताचा दास, देवाच्या सुवार्तेसाठी वेगळा केलेला, पौल ह्याजकडून;') }
end

describe 'bbl search ख्रिस्त in romans 2 in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रिस्त in romans 2 in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 2:16 देव, माझ्या सुवार्तेप्रमाणे जेव्हा मनुष्यांच्या गुप्त गोष्टींचा ख्रिस्त येशूकडून न्याय करील त्यादिवशी हे दिसून येईल.') }
end

describe 'bbl search ख्रिस्त in romans 3-5 in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रिस्त in romans 3-5 in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोम. 3:22 पण हे देवाचे नीतिमत्त्व येशू ख्रिस्तावरील विश्वासाद्वारे, विश्वास ठेवणार्‍या सर्वांसाठी आहे कारण तेथे कसलाही फरक नाही.') }
end

describe 'bbl search ख्रिस्त in johns letters in irvmar exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ख्रिस्त in johns letters in irvmar') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 योहा. 1:3 आम्ही जे पाहिले व ऐकले आहे ते आम्ही आता तुम्हांलाही घोषित करीत आहोत, यासाठी की तुमचीही आमच्यासोबत सहभागिता असावी. आमची सहभागिता तर देवपिता व त्याचा पुत्र येशू ख्रिस्त याजबरोबर आहे.') }
end

# ---------------------------------------------------------------------------
# irvtel (Telugu - Indian Revised Version)
# ---------------------------------------------------------------------------

describe 'bbl search యేసు క్రీస్తు in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు క్రీస్తు in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('మత్తయి 1:1 అబ్రాహాము వంశం వాడైన దావీదు వంశం వాడు యేసు క్రీస్తు వంశావళి.') }
end

describe 'bbl search యేసు క్రీస్తు in romans in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు క్రీస్తు in romans in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 1:1 యేసు క్రీస్తు దాసుడు, అపోస్తలుడుగా పిలుపు పొందినవాడు, దేవుని సువార్త కోసం ప్రభువు ప్రత్యేకించుకున్న') }
end

describe 'bbl search యేసు క్రీస్తు in romans 2 in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు క్రీస్తు in romans 2 in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 2:16 నా సువార్త ప్రకారం దేవుడు యేసు క్రీస్తు ద్వారా మానవుల రహస్యాలను విచారించే రోజున ఈ విధంగా జరుగుతుంది.') }
end

describe 'bbl search యేసు క్రీస్తు in romans 3-5 in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు క్రీస్తు in romans 3-5 in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 3:22 అది యేసు క్రీస్తులో విశ్వాసమూలంగా నమ్మే వారందరికీ కలిగే దేవుని నీతి.') }
end

describe 'bbl search యేసు క్రీస్తు in johns letters in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు క్రీస్తు in johns letters in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 యోహాను పత్రిక 1:3 మీరు కూడా మాతో సహవాసం కలిగి ఉండాలని మేము చూసిందీ, విన్నదీ మీకు ప్రకటిస్తున్నాం. నిజానికి మన సహవాసం తండ్రితోను, ఆయన కుమారుడు యేసు క్రీస్తుతోను ఉంది.') }
end

describe 'bbl search యేసు in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('మత్తయి 1:1 అబ్రాహాము వంశం వాడైన దావీదు వంశం వాడు యేసు క్రీస్తు వంశావళి.') }
end

describe 'bbl search యేసు in romans in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు in romans in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 1:1 యేసు క్రీస్తు దాసుడు, అపోస్తలుడుగా పిలుపు పొందినవాడు, దేవుని సువార్త కోసం ప్రభువు ప్రత్యేకించుకున్న') }
end

describe 'bbl search యేసు in romans 2 in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు in romans 2 in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 2:16 నా సువార్త ప్రకారం దేవుడు యేసు క్రీస్తు ద్వారా మానవుల రహస్యాలను విచారించే రోజున ఈ విధంగా జరుగుతుంది.') }
end

describe 'bbl search యేసు in romans 3-5 in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు in romans 3-5 in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 3:22 అది యేసు క్రీస్తులో విశ్వాసమూలంగా నమ్మే వారందరికీ కలిగే దేవుని నీతి.') }
end

describe 'bbl search యేసు in johns letters in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search యేసు in johns letters in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 యోహాను పత్రిక 1:3 మీరు కూడా మాతో సహవాసం కలిగి ఉండాలని మేము చూసిందీ, విన్నదీ మీకు ప్రకటిస్తున్నాం. నిజానికి మన సహవాసం తండ్రితోను, ఆయన కుమారుడు యేసు క్రీస్తుతోను ఉంది.') }
end

describe 'bbl search క్రీస్తు in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search క్రీస్తు in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('మత్తయి 1:1 అబ్రాహాము వంశం వాడైన దావీదు వంశం వాడు యేసు క్రీస్తు వంశావళి.') }
end

describe 'bbl search క్రీస్తు in romans in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search క్రీస్తు in romans in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 1:1 యేసు క్రీస్తు దాసుడు, అపోస్తలుడుగా పిలుపు పొందినవాడు, దేవుని సువార్త కోసం ప్రభువు ప్రత్యేకించుకున్న') }
end

describe 'bbl search క్రీస్తు in romans 2 in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search క్రీస్తు in romans 2 in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 2:16 నా సువార్త ప్రకారం దేవుడు యేసు క్రీస్తు ద్వారా మానవుల రహస్యాలను విచారించే రోజున ఈ విధంగా జరుగుతుంది.') }
end

describe 'bbl search క్రీస్తు in romans 3-5 in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search క్రీస్తు in romans 3-5 in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('రోమా పత్రిక 3:22 అది యేసు క్రీస్తులో విశ్వాసమూలంగా నమ్మే వారందరికీ కలిగే దేవుని నీతి.') }
end

describe 'bbl search క్రీస్తు in johns letters in irvtel exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search క్రీస్తు in johns letters in irvtel') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 యోహాను పత్రిక 1:3 మీరు కూడా మాతో సహవాసం కలిగి ఉండాలని మేము చూసిందీ, విన్నదీ మీకు ప్రకటిస్తున్నాం. నిజానికి మన సహవాసం తండ్రితోను, ఆయన కుమారుడు యేసు క్రీస్తుతోను ఉంది.') }
end

# ---------------------------------------------------------------------------
# irvurd (Urdu - Indian Revised Version)
# ---------------------------------------------------------------------------

describe 'bbl search ईसा मसीह in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा मसीह in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्त 1:1 ईसा मसीह इबने दाऊद इबने इब्राहीम का नसबनामा।') }
end

describe 'bbl search ईसा मसीह in romans in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा मसीह in romans in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 1:1 पौलुस की तरफ़ से जो ईसा मसीह का बन्दा है और रसूल होने के लिए बुलाया गया और ख़ुदा की उस ख़ुशख़बरी के लिए अलग किया गया।') }
end

describe 'bbl search ईसा मसीह in romans 2 in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा मसीह in romans 2 in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 2:16 जिस रोज़ ख़ुदा ख़ुशख़बरी के मुताबिक़ जो मै ऐलान करता हूँ ईसा मसीह की मारिफ़त आदमियों की छुपी बातों का इन्साफ़ करेगा।') }
end

describe 'bbl search ईसा मसीह in romans 3-5 in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा मसीह in romans 3-5 in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 3:22 यानी ख़ुदा की वो रास्तबाज़ी जो ईसा मसीह पर ईमान लाने से सब ईमान लानेवालों को हासिल होती है; क्यूँकि कुछ फ़र्क़ नहीं।') }
end

describe 'bbl search ईसा मसीह in johns letters in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा मसीह in johns letters in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 यूह 1:3 जो कुछ हम ने देखा और सुना है तुम्हें भी उसकी ख़बर देते है, ताकि तुम भी हमारे शरीक हो, और हमारा मेल मिलाप बाप के साथ और उसके बेटे ईसा मसीह के साथ है।') }
end

describe 'bbl search ईसा in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('मत्त 1:1 ईसा मसीह इबने दाऊद इबने इब्राहीम का नसबनामा।') }
end

describe 'bbl search ईसा in romans in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा in romans in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 1:1 पौलुस की तरफ़ से जो ईसा मसीह का बन्दा है और रसूल होने के लिए बुलाया गया और ख़ुदा की उस ख़ुशख़बरी के लिए अलग किया गया।') }
end

describe 'bbl search ईसा in romans 2 in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा in romans 2 in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 2:16 जिस रोज़ ख़ुदा ख़ुशख़बरी के मुताबिक़ जो मै ऐलान करता हूँ ईसा मसीह की मारिफ़त आदमियों की छुपी बातों का इन्साफ़ करेगा।') }
end

describe 'bbl search ईसा in romans 3-5 in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा in romans 3-5 in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 3:22 यानी ख़ुदा की वो रास्तबाज़ी जो ईसा मसीह पर ईमान लाने से सब ईमान लानेवालों को हासिल होती है; क्यूँकि कुछ फ़र्क़ नहीं।') }
end

describe 'bbl search ईसा in johns letters in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search ईसा in johns letters in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 यूह 1:3 जो कुछ हम ने देखा और सुना है तुम्हें भी उसकी ख़बर देते है, ताकि तुम भी हमारे शरीक हो, और हमारा मेल मिलाप बाप के साथ और उसके बेटे ईसा मसीह के साथ है।') }
end

# ---------------------------------------------------------------------------
# irvurd (Urdu - Indian Revised Version) - last name term
# ---------------------------------------------------------------------------

describe 'bbl search मसीह in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('ज़बूर 2:2 ख़ुदावन्द और उसके मसीह के ख़िलाफ़ ज़मीन के बादशाह एक हो कर, और हाकिम आपस में मशवरा करके कहते हैं,') }
end

describe 'bbl search मसीह in romans in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in romans in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 1:1 पौलुस की तरफ़ से जो ईसा मसीह का बन्दा है और रसूल होने के लिए बुलाया गया और ख़ुदा की उस ख़ुशख़बरी के लिए अलग किया गया।') }
end

describe 'bbl search मसीह in romans 2 in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in romans 2 in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 2:16 जिस रोज़ ख़ुदा ख़ुशख़बरी के मुताबिक़ जो मै ऐलान करता हूँ ईसा मसीह की मारिफ़त आदमियों की छुपी बातों का इन्साफ़ करेगा।') }
end

describe 'bbl search मसीह in romans 3-5 in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in romans 3-5 in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('रोमि 3:22 यानी ख़ुदा की वो रास्तबाज़ी जो ईसा मसीह पर ईमान लाने से सब ईमान लानेवालों को हासिल होती है; क्यूँकि कुछ फ़र्क़ नहीं।') }
end

describe 'bbl search मसीह in johns letters in irvurd exact output' do
  include_context 'search helpers'
  let(:cmd) { $bbl_run.call('search मसीह in johns letters in irvurd') }
  let(:result) { command(cmd) }
  subject(:results) { search_results(cmd) }

  it { expect(result.exit_status).to eq 0 }
  it('starts with expected text') { expect(results.first).to eq('1 यूह 1:3 जो कुछ हम ने देखा और सुना है तुम्हें भी उसकी ख़बर देते है, ताकि तुम भी हमारे शरीक हो, और हमारा मेल मिलाप बाप के साथ और उसके बेटे ईसा मसीह के साथ है।') }
end

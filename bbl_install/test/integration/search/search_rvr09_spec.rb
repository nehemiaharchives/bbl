['Jesucristo', 'Jesús', 'Cristo'].each do |term|
  describe "bbl search #{term} in rvr09" do
    subject(:result) { command($bbl_run.call("search #{term} in rvr09")) }
    it 'succeeds' do
      expect(result.exit_status).to eq 0
    end
    it 'returns Mateo 1:1 as first hit' do
      expect(result.stdout.force_encoding('UTF-8')).to include 'Mateo 1:1 LIBRO de la generación de Jesucristo, hijo de David, hijo de Abraham.'
    end
  end
end

describe 'bbl search Jesucristo --book romans in rvr09' do
  subject(:result) { command($bbl_run.call('search Jesucristo --book romans in rvr09')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Romanos 1:1 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'Romanos 1:1 PABLO, siervo de Jesucristo, llamado á ser apóstol, apartado para el evangelio de Dios,'
  end
end

describe 'bbl search Jesucristo in rvr09 --book romans --chapter 2' do
  subject(:result) { command($bbl_run.call('search Jesucristo in rvr09 --book romans --chapter 2')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Romanos 2:16 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'Romanos 2:16 En el día que juzgará el Señor lo encubierto de los hombres, conforme á mi evangelio, por Jesucristo.'
  end
end

describe 'bbl search Jesucristo in rvr09 --book romans --chapter 3 --end-chapter 5' do
  subject(:result) { command($bbl_run.call('search Jesucristo in rvr09 --book romans --chapter 3 --end-chapter 5')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns Romanos 3:22 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'Romanos 3:22 La justicia de Dios por la fe de Jesucristo, para todos los que creen en él: porque no hay diferencia;'
  end
end

describe 'bbl search Jesucristo in "johns letters" in rvr09' do
  subject(:result) { command($bbl_run.call('search Jesucristo in "johns letters" in rvr09')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns 1 Juan 1:3 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include '1 Juan 1:3 Lo que hemos visto y oído, eso os anunciamos, para que también vosotros tengáis comunión con nosotros: y nuestra comunión verdaderamente es con el Padre, y con su Hijo Jesucristo.'
  end
end

['Jesucristo', 'Jesus', 'Cristo'].each do |term|
  describe "bbl search #{term} in abtag" do
    subject(:result) { command($bbl_run.call("search #{term} in abtag")) }
    it 'succeeds' do
      expect(result.exit_status).to eq 0
    end
    it 'returns MATEO 1:1 as first hit' do
      expect(result.stdout.force_encoding('UTF-8')).to include 'MATEO 1:1 Ang aklat ng lahi ni Jesucristo, na anak ni David, na anak ni Abraham.'
    end
  end
end

describe 'bbl search Jesucristo --book romans in abtag' do
  subject(:result) { command($bbl_run.call('search Jesucristo --book romans in abtag')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns MGA TAGA ROMA 1:1 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'MGA TAGA ROMA 1:1 Si Pablo na alipin ni Jesucristo, na tinawag na maging apostol, ibinukod sa evangelio ng Dios,'
  end
end

describe 'bbl search Jesucristo in abtag --book romans --chapter 2' do
  subject(:result) { command($bbl_run.call('search Jesucristo in abtag --book romans --chapter 2')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns MGA TAGA ROMA 2:16 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'MGA TAGA ROMA 2:16 Sa araw na hahatulan ng Dios ang mga lihim ng mga tao, ayon sa aking evangelio, sa pamamagitan ni Jesucristo.'
  end
end

describe 'bbl search Jesucristo in abtag --book romans --chapter 3 --end-chapter 5' do
  subject(:result) { command($bbl_run.call('search Jesucristo in abtag --book romans --chapter 3 --end-chapter 5')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns MGA TAGA ROMA 3:22 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'MGA TAGA ROMA 3:22 Sa makatuwid baga\'y ang katuwiran ng Dios sa pamamagitan ng pananampalataya kay Jesucristo sa lahat ng mga nagsisisampalataya; sapagka\'t walang pagkakaiba;'
  end
end

describe 'bbl search Jesucristo in abtag in "johns letters"' do
  subject(:result) { command($bbl_run.call('search Jesucristo in abtag in "johns letters"')) }
  it 'succeeds' do
    expect(result.exit_status).to eq 0
  end
  it 'returns I JUAN 1:3 as first hit' do
    expect(result.stdout.force_encoding('UTF-8')).to include 'I JUAN 1:3 Yaong aming nakita at narinig ay siya rin naming ibinabalita sa inyo, upang kayo naman ay magkaroon ng pakikisama sa amin: oo, at tayo ay may pakikisama sa Ama, at sa kaniyang Anak na si Jesucristo:'
  end
end

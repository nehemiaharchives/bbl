# bbl search E2E tests for Windows
# The test targets and assertions follows test-framework/src/commonMain/kotlin/org/gnit/bible/test/search/person/NTGospelsPersonTest.kt

param(
  [string]$BblPath = "",
  [int]$ThrottleLimit = 4
)

if ($PSVersionTable.PSVersion.Major -lt 7) {
  Write-Host "ERROR: This parallel E2E test requires PowerShell 7+."
  Write-Host "Run it with pwsh, not Windows PowerShell 5.1."
  exit 1
}

if ($ThrottleLimit -lt 1) {
  Write-Host "ERROR: -ThrottleLimit must be 1 or greater."
  exit 1
}

if (-not $BblPath) {
  $candidates = @(
    "$env:LOCALAPPDATA\Programs\bbl\bbl.exe",
    "$env:USERPROFILE\.bbl\bin\bbl.exe",
    "$env:USERPROFILE\.bbl\bbl.exe"
  )

  $BblPath = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1

  if (-not $BblPath) {
    Write-Host "ERROR: bbl.exe not found. Provide -BblPath or ensure it is installed."
    exit 1
  }
}

# bbl.exe outputs UTF-8; tell PowerShell to decode native output as UTF-8.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$script:tests = [System.Collections.Generic.List[object]]::new()
$script:order = 0

function Add-Test(
        [string]$Group,
        [string]$Name,
        [string[]]$CliArgs,
        [string]$ExpectedLine,
        [string]$ExpectedOutput = ""
) {
  $script:tests.Add([pscustomobject]@{
    Order          = [int]$script:order
    Group          = [string]$Group
    Name           = [string]$Name
    CliArgs        = [string[]]$CliArgs
    ExpectedLine   = [string]$ExpectedLine
    ExpectedOutput = [string]$ExpectedOutput
  }) | Out-Null

  $script:order++
}

# --- WEBUS (default translation) ---
foreach ($t in @('Jesus Christ', 'Jesus', 'Christ')) {
  Add-Test `
    'WEBUS (default)' `
    "search $t" `
    @('search', $t) `
    'Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.'
}

Add-Test `
  'WEBUS (default)' `
  'search "Jesus wept" exact' `
  @('search', 'Jesus wept') `
  'John 11:35 Jesus wept.'

Add-Test `
  'WEBUS (default)' `
  'search Jesus wept unquoted' `
  @('search', 'Jesus', 'wept') `
  "Matthew 26:75 Peter remembered the word which Jesus had said to him, $([char]0x201C)Before the rooster crows, you will deny me three times.$([char]0x201D) Then he went out and wept bitterly."

Add-Test `
  'WEBUS (default)' `
  'search Jesus Christ in romans' `
  @('search', 'Jesus Christ', 'in', 'romans') `
  'Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,'

Add-Test `
  'WEBUS (default)' `
  'search Jesus Christ in "johns letters"' `
  @('search', 'Jesus Christ', 'in', 'johns letters') `
  '1 John 1:3 that which we have seen and heard we declare to you, that you also may have fellowship with us. Yes, and our fellowship is with the Father and with his Son, Jesus Christ.'

Add-Test `
  'WEBUS (default)' `
  'search Jesus weep stemming' `
  @('search', 'Jesus', 'weep') `
  "Matthew 26:75 Peter remembered the word which Jesus had said to him, $([char]0x201C)Before the rooster crows, you will deny me three times.$([char]0x201D) Then he went out and wept bitterly."

# --- Limit ---
Add-Test `
  'Limit' `
  'search Jesus Christ limit 1' `
  @('search', 'Jesus Christ', 'limit', '1') `
  'Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.'

Add-Test `
  'Limit' `
  'search Jesus Christ in kjv limit 1' `
  @('search', 'Jesus Christ', 'in', 'kjv', 'limit', '1') `
  'Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.'

Add-Test `
  'Limit' `
  'search Jesus Christ in romans limit 1' `
  @('search', 'Jesus Christ', 'in', 'romans', 'limit', '1') `
  'Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,'

Add-Test `
  'Limit' `
  'search Jesus Christ in romans 3 limit 1' `
  @('search', 'Jesus Christ', 'in', 'romans', '3', 'limit', '1') `
  'Romans 3:22 even the righteousness of God through faith in Jesus Christ to all and on all those who believe. For there is no distinction,'

Add-Test `
  'Limit' `
  'search Jesus Christ in romans 5-12 limit 1' `
  @('search', 'Jesus Christ', 'in', 'romans', '5-12', 'limit', '1') `
  'Romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;'

Add-Test `
  'Limit' `
  'search Jesus Christ in romans 5-12 in kjv limit 1' `
  @('search', 'Jesus Christ', 'in', 'romans', '5-12', 'in', 'kjv', 'limit', '1') `
  'Romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:'

# --- KJV ---
foreach ($t in @('Jesus Christ', 'Jesus', 'Christ')) {
  Add-Test `
    'KJV' `
    "search $t in kjv" `
    @('search', $t, 'in', 'kjv') `
    'Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.'
}

Add-Test `
  'KJV' `
  'search "Jesus wept" in kjv exact' `
  @('search', 'Jesus wept', 'in', 'kjv') `
  'John 11:35 Jesus wept.'

Add-Test `
  'KJV' `
  'search Jesus wept in kjv' `
  @('search', 'Jesus', 'wept', 'in', 'kjv') `
  'Matthew 26:75 And Peter remembered the word of Jesus, which said unto him, Before the cock crow, thou shalt deny me thrice. And he went out, and wept bitterly.'

Add-Test `
  'KJV' `
  'search Jesus Christ in kjv in romans' `
  @('search', 'Jesus Christ', 'in', 'kjv', 'in', 'romans') `
  'Romans 1:1 Paul, a servant of Jesus Christ, called [to be] an apostle, separated unto the gospel of God,'

Add-Test `
  'KJV' `
  'search Jesus Christ in "johns letters" in kjv' `
  @('search', 'Jesus Christ', 'in', 'johns letters', 'in', 'kjv') `
  '1 John 1:3 That which we have seen and heard declare we unto you, that ye also may have fellowship with us: and truly our fellowship [is] with the Father, and with his Son Jesus Christ.'

Add-Test `
  'KJV' `
  'search Jesus weep in kjv stemming' `
  @('search', 'Jesus', 'weep', 'in', 'kjv') `
  'Matthew 26:75 And Peter remembered the word of Jesus, which said unto him, Before the cock crow, thou shalt deny me thrice. And he went out, and wept bitterly.'

Add-Test `
  'KJV' `
  'search Olivet in kjv jc krv compares translations' `
  @('search', 'Olivet', 'in', 'kjv', 'jc', 'krv') `
  '2 Samuel 15:30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up.' `
  @'
2 Samuel 15:30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up.
サムエル記下 15:30 ダビデはオリブ山の坂道を登ったが、登る時に泣き、その頭をおおい、はだしで行った。彼と共にいる民もみな頭をおおって登り、泣きながら登った。
사무엘하 15:30 다윗이 감람산 길로 올라갈 때에 머리를 가리우고 맨발로 울며 행하고 저와 함께 가는 백성들도 각각 그 머리를 가리우고 울며 올라가니라
Acts 1:12 Then returned they unto Jerusalem from the mount called Olivet, which is from Jerusalem a sabbath day's journey.
使徒行伝 1:12 それから彼らは、オリブという山を下ってエルサレムに帰った。この山はエルサレムに近く、安息日に許されている距離のところにある。
사도행전 1:12 제자들이 감람원이라 하는 산으로부터 예루살렘에 돌아오니 이 산은 예루살렘에서 가까와 안식일에 가기 알맞은 길이라
'@

# --- RVR09 ---
$JES = "Jesús"

foreach ($t in @('Jesucristo', $JES, 'Cristo')) {
  Add-Test `
    'RVR09' `
    "search $t in rvr09" `
    @('search', $t, 'in', 'rvr09') `
    'Mateo 1:1 LIBRO de la generación de Jesucristo, hijo de David, hijo de Abraham.'
}

Add-Test `
  'RVR09' `
  'search Jesucristo in rvr09 in romans' `
  @('search', 'Jesucristo', 'in', 'rvr09', 'in', 'romans') `
  'Romanos 1:1 PABLO, siervo de Jesucristo, llamado á ser apóstol, apartado para el evangelio de Dios,'

Add-Test `
  'RVR09' `
  'search Jesucristo in rvr09 in romans 2' `
  @('search', 'Jesucristo', 'in', 'rvr09', 'in', 'romans', '2') `
  'Romanos 2:16 En el día que juzgará el Señor lo encubierto de los hombres, conforme á mi evangelio, por Jesucristo.'

Add-Test `
  'RVR09' `
  'search Jesucristo in rvr09 in romans 3-5' `
  @('search', 'Jesucristo', 'in', 'rvr09', 'in', 'romans', '3-5') `
  'Romanos 3:22 La justicia de Dios por la fe de Jesucristo, para todos los que creen en él: porque no hay diferencia;'

Add-Test `
  'RVR09' `
  'search Jesucristo in "johns letters" in rvr09' `
  @('search', 'Jesucristo', 'in', 'johns letters', 'in', 'rvr09') `
  '1 Juan 1:3 Lo que hemos visto y oído, eso os anunciamos, para que también vosotros tengáis comunión con nosotros: y nuestra comunión verdaderamente es con el Padre, y con su Hijo Jesucristo.'

# --- TB ---
foreach ($t in @('Jesus Cristo', 'Jesus', 'Cristo')) {
  Add-Test `
    'TB' `
    "search $t in tb" `
    @('search', $t, 'in', 'tb') `
    'Mateus 1:1 Livro da geração de Jesus Cristo, filho de Davi, filho de Abraão.'
}

Add-Test `
  'TB' `
  'search Jesus Cristo in tb in romans' `
  @('search', 'Jesus Cristo', 'in', 'tb', 'in', 'romans') `
  'Romanos 1:4 e que foi com poder declarado Filho de Deus, quanto ao espírito de santidade, pela ressurreição dos mortos), Jesus Cristo, nosso Senhor,'

Add-Test `
  'TB' `
  'search Jesus Cristo in tb in romans 3-5' `
  @('search', 'Jesus Cristo', 'in', 'tb', 'in', 'romans', '3-5') `
  'Romanos 3:22 a saber, a justiça de Deus mediante a fé em Jesus Cristo, para com todos os que creem. Pois não há distinção,'

Add-Test `
  'TB' `
  'search Jesus Cristo in tb in "johns letters"' `
  @('search', 'Jesus Cristo', 'in', 'tb', 'in', 'johns letters') `
  '1 João 1:3 o que temos visto e ouvido também vo-lo anunciamos, para que vós também tenhais comunhão conosco. A nossa comunhão é com o Pai e com seu Filho, Jesus Cristo.'

# --- DELUT ---
foreach ($t in @('Jesu Christi', 'Jesu', 'Christi')) {
  Add-Test `
    'DELUT' `
    "search $t in delut" `
    @('search', $t, 'in', 'delut') `
    @'
Matthäus 1:1 Dies ist das Buch von der Geburt Jesu Christi, der da ist ein Sohn Davids, des Sohnes Abrahams.
'@
}

Add-Test `
  'DELUT' `
  'search Jesu Christi in delut in romans' `
  @('search', 'Jesu Christi', 'in', 'delut', 'in', 'romans') `
  @'
Römer 1:1 Paulus, ein Knecht Jesu Christi, berufen zum Apostel, ausgesondert, zu predigen das Evangelium Gottes,
'@

Add-Test `
  'DELUT' `
  'search Jesu Christi in delut in romans 2' `
  @('search', 'Jesu Christi', 'in', 'delut', 'in', 'romans', '2') `
  @'
Römer 2:16 auf den Tag, da Gott das Verborgene der Menschen durch Jesus Christus richten wird laut meines Evangeliums.
'@

Add-Test `
  'DELUT' `
  'search Jesu Christi in delut in romans 3-5' `
  @('search', 'Jesu Christi', 'in', 'delut', 'in', 'romans', '3-5') `
  @'
Römer 3:22 Ich sage aber von solcher Gerechtigkeit vor Gott, die da kommt durch den Glauben an Jesum Christum zu allen und auf alle, die da glauben.
'@

Add-Test `
  'DELUT' `
  'search Jesu Christi in delut in "johns letters"' `
  @('search', 'Jesu Christi', 'in', 'delut', 'in', 'johns letters') `
  @'
1. Johannes 1:3 was wir gesehen und gehört haben, das verkündigen wir euch, auf daß ihr mit uns Gemeinschaft habt; und unsre Gemeinschaft ist mit dem Vater und mit seinem Sohn Jesus Christus.
'@

# --- LSG ---
foreach ($t in @('Jésus-Christ', 'Jésus', 'Christ')) {
  Add-Test `
    'LSG' `
    "search $t in lsg" `
    @('search', $t, 'in', 'lsg') `
    @'
Matthieu 1:1 Généalogie de Jésus-Christ, fils de David, fils d’Abraham.
'@
}

Add-Test `
  'LSG' `
  'search Jésus-Christ in lsg in romans' `
  @('search', 'Jésus-Christ', 'in', 'lsg', 'in', 'romans') `
  @'
Romains 1:1 Paul, serviteur de Jésus-Christ, appelé à être apôtre, mis à part pour annoncer l’Évangile de Dieu,
'@

Add-Test `
  'LSG' `
  'search Jésus-Christ in lsg in romans 2' `
  @('search', 'Jésus-Christ', 'in', 'lsg', 'in', 'romans', '2') `
  @'
Romains 2:16 C’est ce qui paraîtra au jour où, selon mon Évangile, Dieu jugera par Jésus-Christ les actions secrètes des hommes.
'@

Add-Test `
  'LSG' `
  'search Jésus-Christ in lsg in romans 3-5' `
  @('search', 'Jésus-Christ', 'in', 'lsg', 'in', 'romans', '3-5') `
  @'
Romains 3:22 justice de Dieu par la foi en Jésus-Christ pour tous ceux qui croient. Il n’y a point de distinction.
'@

Add-Test `
  'LSG' `
  'search Jésus-Christ in lsg in "johns letters"' `
  @('search', 'Jésus-Christ', 'in', 'lsg', 'in', 'johns letters') `
  @'
1 Jean 1:3 ce que nous avons vu et entendu, nous vous l’annonçons, à vous aussi, afin que vous aussi vous soyez en communion avec nous. Or, notre communion est avec le Père et avec son Fils Jésus-Christ.
'@

# --- SINOD ---
foreach ($t in @('Иисуса Христа', 'Иисуса', 'Христа')) {
  Add-Test `
    'SINOD' `
    "search $t in sinod" `
    @('search', $t, 'in', 'sinod') `
    @'
От Матфея святое благовествование 1:1 Родословие Иисуса Христа, Сына Давидова, Сына Авраамова.
'@
}

Add-Test `
  'SINOD' `
  'search Иисуса Христа in sinod in romans' `
  @('search', 'Иисуса Христа', 'in', 'sinod', 'in', 'romans') `
  @'
Послание к Римлянам 1:1 Павел, раб Иисуса Христа, призванный Апостол, избранный к благовестию Божию,
'@

Add-Test `
  'SINOD' `
  'search Иисуса Христа in sinod in romans 2' `
  @('search', 'Иисуса Христа', 'in', 'sinod', 'in', 'romans', '2') `
  @'
Послание к Римлянам 2:16 в день, когда, по благовествованию моему, Бог будет судить тайные дела человеков через Иисуса Христа.
'@

Add-Test `
  'SINOD' `
  'search Иисуса Христа in sinod in romans 3-5' `
  @('search', 'Иисуса Христа', 'in', 'sinod', 'in', 'romans', '3-5') `
  @'
Послание к Римлянам 3:22 правда Божия через веру в Иисуса Христа во всех и на всех верующих, ибо нет различия,
'@

Add-Test `
  'SINOD' `
  'search Иисуса Христа in sinod in "johns letters"' `
  @('search', 'Иисуса Христа', 'in', 'sinod', 'in', 'johns letters') `
  @'
Первое послание Иоанна 1:3 о том, что мы видели и слышали, возвещаем вам, чтобы и вы имели общение с нами: а наше общение — с Отцем и Сыном Его, Иисусом Христом.
'@

# --- SVRJ ---
foreach ($t in @('JEZUS CHRISTUS', 'JEZUS', 'CHRISTUS')) {
  Add-Test `
    'SVRJ' `
    "search $t in svrj" `
    @('search', $t, 'in', 'svrj') `
    @'
MATTHEÜS 1:1 Het boek des geslachts van JEZUS CHRISTUS, den Zoon van David, den zoon van Abraham.
'@
}

Add-Test `
  'SVRJ' `
  'search JEZUS CHRISTUS in svrj in romans' `
  @('search', 'JEZUS CHRISTUS', 'in', 'svrj', 'in', 'romans') `
  @'
ROMEINEN 1:1 Paulus, een dienstknecht van Jezus Christus, een geroepen apostel, afgezonderd tot het Evangelie van God,
'@

Add-Test `
  'SVRJ' `
  'search JEZUS CHRISTUS in svrj in romans 2' `
  @('search', 'JEZUS CHRISTUS', 'in', 'svrj', 'in', 'romans', '2') `
  @'
ROMEINEN 2:16 In den dag wanneer God de verborgene dingen der mensen zal oordelen door Jezus Christus, naar mijn Evangelie.
'@

Add-Test `
  'SVRJ' `
  'search JEZUS CHRISTUS in svrj in romans 3-5' `
  @('search', 'JEZUS CHRISTUS', 'in', 'svrj', 'in', 'romans', '3-5') `
  @'
ROMEINEN 3:22 Namelijk de rechtvaardigheid Gods door het geloof van Jezus Christus, tot allen, en over allen, die geloven; want er is geen onderscheid.
'@

Add-Test `
  'SVRJ' `
  'search JEZUS CHRISTUS in svrj in "johns letters"' `
  @('search', 'JEZUS CHRISTUS', 'in', 'svrj', 'in', 'johns letters') `
  @'
1 JOHANNES 1:3 Hetgeen wij dan gezien en gehoord hebben, dat verkondigen wij u, opdat ook gij met ons gemeenschap zoudt hebben, en deze onze gemeenschap ook zij met den Vader, en met Zijn Zoon Jezus Christus.
'@

# --- RDV24 ---
foreach ($t in @('Gesù Cristo', 'Gesù', 'Cristo')) {
  Add-Test `
    'RDV24' `
    "search $t in rdv24" `
    @('search', $t, 'in', 'rdv24') `
    @'
Matteo 1:1 Genealogia di Gesù Cristo figliuolo di Davide, figliuolo d'Abramo.
'@
}

Add-Test `
  'RDV24' `
  'search Gesù Cristo in rdv24 in romans' `
  @('search', 'Gesù Cristo', 'in', 'rdv24', 'in', 'romans') `
  @'
EPISTOLE DI S. PAOLO AI~ROMANI 1:4 nato dal seme di Davide secondo la carne, dichiarato Figliuolo di Dio con potenza secondo lo spirito di santità mediante la sua risurrezione dai morti; cioè Gesù Cristo nostro Signore,
'@

Add-Test `
  'RDV24' `
  'search Gesù Cristo in rdv24 in romans 2' `
  @('search', 'Gesù Cristo', 'in', 'rdv24', 'in', 'romans', '2') `
  @'
EPISTOLE DI S. PAOLO AI~ROMANI 2:16 Tutto ciò si vedrà nel giorno in cui Dio giudicherà i segreti degli uomini per mezzo di Gesù Cristo, secondo il mio Evangelo.
'@

Add-Test `
  'RDV24' `
  'search Gesù Cristo in rdv24 in romans 3-5' `
  @('search', 'Gesù Cristo', 'in', 'rdv24', 'in', 'romans', '3-5') `
  @'
EPISTOLE DI S. PAOLO AI~ROMANI 3:22 vale a dire la giustizia di Dio mediante la fede in Gesù Cristo, per tutti i credenti; poiché non v'è distinzione;
'@

Add-Test `
  'RDV24' `
  'search Gesù Cristo in rdv24 in "johns letters"' `
  @('search', 'Gesù Cristo', 'in', 'rdv24', 'in', 'johns letters') `
  @'
EPISTOLA I DI S. GIOVANNI 1:3 quello, dico, che abbiamo veduto e udito, noi l'annunziamo anche a voi, affinché voi pure abbiate comunione con noi, e la nostra comunione è col Padre e col suo Figliuolo, Gesù Cristo.
'@

# --- UBG ---
foreach ($t in @('Jezusa Chrystusa', 'Jezusa', 'Chrystusa')) {
  Add-Test `
    'UBG' `
    "search $t in ubg" `
    @('search', $t, 'in', 'ubg') `
    @'
Mateusza 1:1 Księga rodu Jezusa Chrystusa, syna Dawida, syna Abrahama.
'@
}

Add-Test `
  'UBG' `
  'search Jezusa Chrystusa in ubg in romans' `
  @('search', 'Jezusa Chrystusa', 'in', 'ubg', 'in', 'romans') `
  @'
Rzymian 1:1 Paweł, sługa Jezusa Chrystusa, powołany apostoł, odłączony do głoszenia ewangelii Boga;
'@

Add-Test `
  'UBG' `
  'search Jezusa Chrystusa in ubg in romans 2' `
  @('search', 'Jezusa Chrystusa', 'in', 'ubg', 'in', 'romans', '2') `
  @'
Rzymian 2:16 W dniu, w którym Bóg przez Jezusa Chrystusa będzie sądził skryte sprawy ludzkie według mojej ewangelii.
'@

Add-Test `
  'UBG' `
  'search Jezusa Chrystusa in ubg in romans 3-5' `
  @('search', 'Jezusa Chrystusa', 'in', 'ubg', 'in', 'romans', '3-5') `
  @'
Rzymian 3:22 Jest to sprawiedliwość Boga przez wiarę Jezusa Chrystusa dla wszystkich i na wszystkich wierzących. Nie ma bowiem różnicy.
'@

Add-Test `
  'UBG' `
  'search Jezusa Chrystusa in ubg in "johns letters"' `
  @('search', 'Jezusa Chrystusa', 'in', 'ubg', 'in', 'johns letters') `
  @'
I Jana 1:3 To, co widzieliśmy i słyszeliśmy, to wam zwiastujemy, abyście i wy mieli z nami społeczność, a nasza społeczność to społeczność z Ojcem i z jego Synem, Jezusem Chrystusem.
'@

# --- UBIO ---
foreach ($t in @('Ісуса Христа', 'Ісуса', 'Христа')) {
  Add-Test `
    'UBIO' `
    "search $t in ubio" `
    @('search', $t, 'in', 'ubio') `
    @'
Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:
'@
}

Add-Test `
  'UBIO' `
  'search Ісуса Христа in ubio in romans' `
  @('search', 'Ісуса Христа', 'in', 'ubio', 'in', 'romans') `
  @'
До римлян 1:1 Павло, раб Ісуса Христа, покликаний апостол, вибраний для звіщання Євангелії Божої,
'@

Add-Test `
  'UBIO' `
  'search Ісуса Христа in ubio in romans 2' `
  @('search', 'Ісуса Христа', 'in', 'ubio', 'in', 'romans', '2') `
  @'
До римлян 2:16 дня, коли Бог, згідно з моїм благовістям, буде судити таємні речі людей через Ісуса Христа.
'@

Add-Test `
  'UBIO' `
  'search Ісуса Христа in ubio in romans 3-5' `
  @('search', 'Ісуса Христа', 'in', 'ubio', 'in', 'romans', '3-5') `
  @'
До римлян 3:22 А Божа правда через віру в Ісуса Христа в усіх і на всіх, хто вірує, бо різниці немає,
'@

Add-Test `
  'UBIO' `
  'search Ісуса Христа in ubio in "johns letters"' `
  @('search', 'Ісуса Христа', 'in', 'ubio', 'in', 'johns letters') `
  @'
1-е Iвана 1:3 що ми бачили й чули про те ми звіщаємо вам, щоб і ви мали спільність із нами. Спільність же наша з Отцем і Сином Його Ісусом Христом.
'@

# --- SVEN ---
foreach ($t in @('Jesu Kristi', 'Jesu', 'Kristi')) {
  Add-Test `
    'SVEN' `
    "search $t in sven" `
    @('search', $t, 'in', 'sven') `
    @'
Matteus 1:1 Detta är Jesu Kristi, Davids sons, Abrahams sons, släkttavla.
'@
}

Add-Test `
  'SVEN' `
  'search Jesu Kristi in sven in romans' `
  @('search', 'Jesu Kristi', 'in', 'sven', 'in', 'romans') `
  @'
Romarbrevet 1:1 Paulus, Jesu Kristi tjänare, kallad till apostel, avskild till att förkunna Guds evangelium,
'@

Add-Test `
  'SVEN' `
  'search Jesu Kristi in sven in romans 3-5' `
  @('search', 'Jesu Kristi', 'in', 'sven', 'in', 'romans', '3-5') `
  @'
Romarbrevet 3:22 en rättfärdighet från Gud genom tro på Jesus Kristus, för alla dem som tro. Ty här är ingen åtskillnad.
'@

Add-Test `
  'SVEN' `
  'search Jesu Kristi in sven in "johns letters"' `
  @('search', 'Jesu Kristi', 'in', 'sven', 'in', 'johns letters') `
  @'
1 Johannesbrevet 1:3 Ja, det vi hava sett och hört, det förkunna vi ock för eder, på det att också I mån hava gemenskap med oss; och vi hava vår gemenskap med Fadern och med hans Son, Jesus Kristus.
'@

# --- CUNP ---
foreach ($t in @('耶稣基督', '耶稣', '基督')) {
  Add-Test `
    'CUNP' `
    "search $t in cunp" `
    @('search', $t, 'in', 'cunp') `
    @'
马太福音 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：
'@
}

Add-Test `
  'CUNP' `
  'search 耶稣基督 in cunp in romans' `
  @('search', '耶稣基督', 'in', 'cunp', 'in', 'romans') `
  @'
罗马书 1:1 耶稣基督的仆人 保罗 ，奉召为使徒，特派传　神的福音。
'@

Add-Test `
  'CUNP' `
  'search 耶稣基督 in cunp in romans 2' `
  @('search', '耶稣基督', 'in', 'cunp', 'in', 'romans', '2') `
  @'
罗马书 2:16 就在　神藉耶稣基督审判人隐秘事的日子，照着我的福音所言。
'@

Add-Test `
  'CUNP' `
  'search 耶稣基督 in cunp in romans 3-5' `
  @('search', '耶稣基督', 'in', 'cunp', 'in', 'romans', '3-5') `
  @'
罗马书 3:22 就是　神的义，因信耶稣基督加给一切相信的人，并没有分别。
'@

Add-Test `
  'CUNP' `
  'search 耶稣基督 in cunp in "johns letters"' `
  @('search', '耶稣基督', 'in', 'cunp', 'in', 'johns letters') `
  @'
约翰一书 1:3 我们将所看见、所听见的传给你们，使你们与我们相交。我们乃是与父并他儿子耶稣基督相交的。
'@

# --- KRV ---
foreach ($t in @('예수그리스도', '예수 그리스도', '예수', '그리스도')) {
  Add-Test `
    'KRV' `
    "search $t in krv" `
    @('search', $t, 'in', 'krv') `
    @'
마태복음 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라
'@
}

Add-Test `
  'KRV' `
  'search 예수그리스도 in krv in romans' `
  @('search', '예수그리스도', 'in', 'krv', 'in', 'romans') `
  @'
로마서 1:1 예수 그리스도의 종 바울은 사도로 부르심을 받아 하나님의 복음을 위하여 택정함을 입었으니
'@

Add-Test `
  'KRV' `
  'search 예수그리스도 in krv in romans 2' `
  @('search', '예수그리스도', 'in', 'krv', 'in', 'romans', '2') `
  @'
로마서 2:16 곧 내 복음에 이른 바와 같이 하나님이 예수 그리스도로 말미암아 사람들의 은밀한 것을 심판하시는 그날이라
'@

Add-Test `
  'KRV' `
  'search 예수그리스도 in krv in romans 3-5' `
  @('search', '예수그리스도', 'in', 'krv', 'in', 'romans', '3-5') `
  @'
로마서 3:22 곧 예수 그리스도를 믿음으로 말미암아 모든 믿는 자에게 미치는 하나님의 의니 차별이 없느니라
'@

Add-Test `
  'KRV' `
  'search 예수그리스도 in krv in "johns letters"' `
  @('search', '예수그리스도', 'in', 'krv', 'in', 'johns letters') `
  @'
요한1서 1:3 우리가 보고 들은 바를 너희에게도 전함은 너희로 우리와 사귐이 있게 하려 함이니 우리의 사귐은 아버지와 그 아들 예수 그리스도와 함께 함이라
'@

# --- JC ---
foreach ($t in @('イエス・キリスト', 'イエス', 'キリスト')) {
  Add-Test `
    'JC' `
    "search $t in jc" `
    @('search', $t, 'in', 'jc') `
    @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@
}

Add-Test `
  'JC' `
  'search イエス・キリスト in jc in romans' `
  @('search', 'イエス・キリスト', 'in', 'jc', 'in', 'romans') `
  @'
ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-
'@

Add-Test `
  'JC' `
  'search イエス・キリスト in jc in romans 2' `
  @('search', 'イエス・キリスト', 'in', 'jc', 'in', 'romans', '2') `
  @'
ローマ人への手紙 2:16 そして、これらのことは、わたしの福音によれば、神がキリスト・イエスによって人々の隠れた事がらをさばかれるその日に、明らかにされるであろう。
'@

Add-Test `
  'JC' `
  'search イエス・キリスト in jc in romans 3-5' `
  @('search', 'イエス・キリスト', 'in', 'jc', 'in', 'romans', '3-5') `
  @'
ローマ人への手紙 3:22 それは、イエス・キリストを信じる信仰による神の義であって、すべて信じる人に与えられるものである。そこにはなんらの差別もない。
'@

Add-Test `
  'JC' `
  'search イエス・キリスト in jc in "johns letters"' `
  @('search', 'イエス・キリスト', 'in', 'jc', 'in', 'johns letters') `
  @'
ヨハネの第一の手紙 1:3 すなわち、わたしたちが見たもの、聞いたものを、あなたがたにも告げ知らせる。それは、あなたがたも、わたしたちの交わりにあずかるようになるためである。わたしたちの交わりとは、父ならびに御子イエス・キリストとの交わりのことである。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl search イエス　キリスト　in jc' `
  @('search', 'イエス　キリスト　in', 'jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl search イエス　キリスト in　jc' `
  @('search', 'イエス　キリスト', 'in　jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl sarch　イエス・キリスト in jc' `
  @('search　イエス・キリスト', 'in', 'jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl search　イエス　キリスト in jc' `
  @('search　イエス　キリスト', 'in', 'jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl search　イエス　キリスト　in jc' `
  @('search　イエス　キリスト　in', 'jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl search　イエス　キリスト　in　jc' `
  @('search　イエス　キリスト　in　jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl search　イエス in jc' `
  @('search　イエス', 'in', 'jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl　search　イエス in jc' `
  @('search　イエス', 'in', 'jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl　search　イエス　in jc' `
  @('search　イエス　in', 'jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl　search　イエス　in　jc' `
  @('search　イエス　in　jc') `
  @'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
'@

Add-Test `
  'JC full-width spaces' `
  'bbl search イエス in jc in romans' `
  @('search', 'イエス', 'in', 'jc', 'in', 'romans') `
  @'
ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-
'@

Add-Test `
  'JC full-width spaces' `
  'bbl search イエス in jc　in romans' `
  @('search', 'イエス', 'in', 'jc　in', 'romans') `
  @'
ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-
'@

# --- AYT ---
foreach ($t in @('Yesus Kristus', 'Yesus', 'Kristus')) {
  Add-Test `
    'AYT' `
    "search $t in ayt" `
    @('search', $t, 'in', 'ayt') `
    @'
Matius 1:1 Kitab silsilah Yesus Kristus, anak Daud, anak Abraham.
'@
}

Add-Test `
  'AYT' `
  'search Yesus Kristus in ayt in romans' `
  @('search', 'Yesus Kristus', 'in', 'ayt', 'in', 'romans') `
  @'
Roma 1:1 Paulus, hamba Yesus Kristus, yang dipanggil menjadi rasul dan dikhususkan bagi Injil Allah;
'@

Add-Test `
  'AYT' `
  'search Yesus Kristus in ayt in romans 2' `
  @('search', 'Yesus Kristus', 'in', 'ayt', 'in', 'romans', '2') `
  @'
Roma 2:16 pada hari ketika Allah menghakimi pikiran-pikiran manusia yang tersembunyi melalui Yesus Kristus, menurut Injilku.
'@

Add-Test `
  'AYT' `
  'search Yesus Kristus in ayt in romans 3-5' `
  @('search', 'Yesus Kristus', 'in', 'ayt', 'in', 'romans', '3-5') `
  @'
Roma 3:24 dan dibenarkan dengan cuma-cuma oleh kasih karunia-Nya melalui penebusan yang ada dalam Yesus Kristus;
'@

Add-Test `
  'AYT' `
  'search Yesus Kristus in ayt in "johns letters"' `
  @('search', 'Yesus Kristus', 'in', 'ayt', 'in', 'johns letters') `
  @'
1 Yohanes 3:16 Beginilah kita mengenal kasih, yaitu bahwa Yesus Kristus telah menyerahkan hidup-Nya untuk kita. Jadi, kita juga harus menyerahkan hidup kita untuk saudara-saudara kita.
'@

# --- TH1971 ---
foreach ($t in @('พระเยซูคริสต์', 'พระเยซู', 'คริสต์')) {
  Add-Test `
    'TH1971' `
    "search $t in th1971" `
    @('search', $t, 'in', 'th1971') `
    @'
มัทธิว 1:1 หนังสือลำดับพงศ์ของพระเยซูคริสต์ ผู้เป็นเชื้อสายของดาวิด ผู้สืบตระกูลเนื่องมาจากอับราฮัม
'@
}

Add-Test `
  'TH1971' `
  'search พระเยซูคริสต์ in th1971 in romans' `
  @('search', 'พระเยซูคริสต์', 'in', 'th1971', 'in', 'romans') `
  @'
โรม 1:1 เปาโล ผู้รับใช้ของพระเยซูคริสต์ ผู้ซึ่งพระองค์ทรงเรียกให้เป็นอัครทูต และได้ทรงตั้งไว้ให้ประกาศข่าวประเสริฐของพระเจ้า
'@

Add-Test `
  'TH1971' `
  'search พระเยซูคริสต์ in th1971 in romans 2' `
  @('search', 'พระเยซูคริสต์', 'in', 'th1971', 'in', 'romans', '2') `
  @'
โรม 2:16 ในวันที่พระเจ้าทรงพิพากษาความลับของมนุษย์โดยพระเยซูคริสต์ ทั้งนี้ตามข่าวประเสริฐที่ข้าพเจ้าได้ประกาศนั้น
'@

Add-Test `
  'TH1971' `
  'search พระเยซูคริสต์ in th1971 in romans 3-5' `
  @('search', 'พระเยซูคริสต์', 'in', 'th1971', 'in', 'romans', '3-5') `
  @'
โรม 3:22 คือความชอบธรรมของพระเจ้า ซึ่งทรงประทานโดยความเชื่อในพระเยซูคริสต์ แก่ทุกคนที่เชื่อ เพราะว่าคนทั้งหลายไม่ต่างกัน
'@

Add-Test `
  'TH1971' `
  'search พระเยซูคริสต์ in th1971 in "johns letters"' `
  @('search', 'พระเยซูคริสต์', 'in', 'th1971', 'in', 'johns letters') `
  @'
1 ยอห์น 1:3 ซึ่งเราได้เห็นและได้ยินนั้น เราก็ได้ประกาศให้ท่านทั้งหลายรู้ด้วย เพื่อท่านทั้งหลายจะได้ร่วมสามัคคีธรรมกับเรา เราทั้งหลายก็ร่วมสามัคคีกับพระบิดา และกับพระเยซูคริสต์พระบุตรของพระองค์
'@

# --- IRVHIN ---
foreach ($t in @('यीशु मसीह', 'यीशु', 'मसीह')) {
  Add-Test `
    'IRVHIN' `
    "search $t in irvhin" `
    @('search', $t, 'in', 'irvhin') `
    @'
मत्ती 1:1 अब्राहम की सन्तान, दाऊद की सन्तान, यीशु मसीह की वंशावली ।
'@
}

Add-Test `
  'IRVHIN' `
  'search यीशु मसीह in irvhin in romans' `
  @('search', 'यीशु मसीह', 'in', 'irvhin', 'in', 'romans') `
  @'
रोमियों 1:1 पौलुस  की ओर से जो यीशु मसीह का दास है, और प्रेरित होने के लिये बुलाया गया, और परमेश्वर के उस सुसमाचार के लिये अलग किया गया है
'@

Add-Test `
  'IRVHIN' `
  'search यीशु मसीह in irvhin in romans 2' `
  @('search', 'यीशु मसीह', 'in', 'irvhin', 'in', 'romans', '2') `
  @'
रोमियों 2:16 जिस दिन परमेश्वर मेरे सुसमाचार के अनुसार यीशु मसीह के द्वारा मनुष्यों की गुप्त बातों का न्याय करेगा।
'@

Add-Test `
  'IRVHIN' `
  'search यीशु मसीह in irvhin in romans 3-5' `
  @('search', 'यीशु मसीह', 'in', 'irvhin', 'in', 'romans', '3-5') `
  @'
रोमियों 3:22 अर्थात् परमेश्वर की वह धार्मिकता, जो यीशु मसीह पर विश्वास करने से सब विश्वास करनेवालों के लिये है। क्योंकि कुछ भेद नहीं;
'@

Add-Test `
  'IRVHIN' `
  'search यीशु मसीह in irvhin in "johns letters"' `
  @('search', 'यीशु मसीह', 'in', 'irvhin', 'in', 'johns letters') `
  @'
1 यूहन्ना 1:3 जो कुछ हमने देखा और सुना है उसका समाचार तुम्हें भी देते हैं, इसलिए कि तुम भी हमारे साथ सहभागी हो; और हमारी यह सहभागिता पिता के साथ, और उसके पुत्र यीशु मसीह के साथ है।
'@

# --- IRVBEN ---
foreach ($t in @('যীশু খ্রীষ্ট', 'যীশু', 'খ্রীষ্ট')) {
  Add-Test `
    'IRVBEN' `
    "search $t in irvben" `
    @('search', $t, 'in', 'irvben') `
    @'
মথি 1:1 যীশু খ্রীষ্টের বংশ তালিকা, তিনি দায়ূদের সন্তান, অব্রাহামের সন্তান।
'@
}

Add-Test `
  'IRVBEN' `
  'search যীশু খ্রীষ্ট in irvben in romans' `
  @('search', 'যীশু খ্রীষ্ট', 'in', 'irvben', 'in', 'romans') `
  @'
রোমীয় 1:1 পৌল, একজন যীশু খ্রীষ্টের দাস, প্রেরিত হবার জন্য ডাকা হয়েছে এবং ঈশ্বরের সুসমাচার প্রচারের জন্য আলাদা ভাবে মনোনীত করেছেন,
'@

Add-Test `
  'IRVBEN' `
  'search যীশু খ্রীষ্ট in irvben in romans 3-5' `
  @('search', 'যীশু খ্রীষ্ট', 'in', 'irvben', 'in', 'romans', '3-5') `
  @'
রোমীয় 3:22 ঈশ্বরের সেই ধার্ম্মিকতা যীশু খ্রীষ্টে বিশ্বাসের মাধ্যমে যারা সবাই বিশ্বাস করে তাদের জন্য। কারণ সেখানে কোনো বিভেদ নেই।
'@

Add-Test `
  'IRVBEN' `
  'search যীশু খ্রীষ্ট in irvben in "johns letters"' `
  @('search', 'যীশু খ্রীষ্ট', 'in', 'irvben', 'in', 'johns letters') `
  @'
1 যোহন 1:3 আমরা যাকে দেখেছি ও শুনেছি, তার খবর তোমাদেরকেও দিচ্ছি, যেন আমাদের সঙ্গে তোমাদেরও সহভাগীতা হয়। আর আমাদের সহভাগীতা হল পিতার এবং তাঁর পুত্র যীশু খ্রীষ্টের সহভাগীতা।
'@

# --- IRVTAM ---
foreach ($t in @('இயேசுகிறிஸ்து', 'இயேசு', 'கிறிஸ்து')) {
  Add-Test `
    'IRVTAM' `
    "search $t in irvtam" `
    @('search', $t, 'in', 'irvtam') `
    @'
மத் 1:1 ஆபிரகாமின் மகனாகிய தாவீதின் குமாரனான இயேசுகிறிஸ்துவின் வம்சவரலாறு:
'@
}

Add-Test `
  'IRVTAM' `
  'search இயேசுகிறிஸ்து in irvtam in romans' `
  @('search', 'இயேசுகிறிஸ்து', 'in', 'irvtam', 'in', 'romans') `
  @'
ரோமர் 1:1 இயேசுகிறிஸ்துவின் ஊழியக்காரனும், அப்போஸ்தலனாக இருப்பதற்காக அழைக்கப்பட்டவனும், தேவனுடைய நற்செய்திக்காகப் பிரித்தெடுக்கப்பட்டவனுமாகிய பவுல்,
'@

Add-Test `
  'IRVTAM' `
  'search இயேசுகிறிஸ்து in irvtam in romans 2' `
  @('search', 'இயேசுகிறிஸ்து', 'in', 'irvtam', 'in', 'romans', '2') `
  @'
ரோமர் 2:16 என்னுடைய நற்செய்தியின்படியே, தேவன் இயேசுகிறிஸ்துவைக்கொண்டு மனிதர்களுடைய இரகசியங்களைக்குறித்து நியாயத்தீர்ப்புக்கொடுக்கும் நாளிலே இது விளங்கும்.
'@

Add-Test `
  'IRVTAM' `
  'search இயேசுகிறிஸ்து in irvtam in romans 3-5' `
  @('search', 'இயேசுகிறிஸ்து', 'in', 'irvtam', 'in', 'romans', '3-5') `
  @'
ரோமர் 3:22 அது இயேசுகிறிஸ்துவை விசுவாசிக்கும் விசுவாசத்தினாலே வரும் தேவநீதியே; விசுவாசிக்கிற எல்லோருக்குள்ளும் எவர்கள் மேலும் அது வரும், வித்தியாசமே இல்லை.
'@

Add-Test `
  'IRVTAM' `
  'search இயேசுகிறிஸ்து in irvtam in "johns letters"' `
  @('search', 'இயேசுகிறிஸ்து', 'in', 'irvtam', 'in', 'johns letters') `
  @'
1 யோவா 1:3 நீங்களும் எங்களோடு ஐக்கியம் உள்ளவர்களாகும்படி, நாங்கள் பார்த்தும் கேட்டும் இருக்கிறதை உங்களுக்கும் அறிவிக்கிறோம்; எங்களுடைய ஐக்கியம் பிதாவோடும் அவருடைய குமாரனாகிய இயேசுகிறிஸ்துவோடும் இருக்கிறது.
'@

# --- NPIULB ---
foreach ($t in @('येशू ख्रीष्‍ट', 'येशू', 'ख्रीष्‍ट')) {
  Add-Test `
    'NPIULB' `
    "search $t in npiulb" `
    @('search', $t, 'in', 'npiulb') `
    @'
मत्ती 1:1 दाऊदका पुत्र अब्राहामका पुत्र येशू ख्रीष्‍टको वंशावलीको पुस्तक ।
'@
}

Add-Test `
  'NPIULB' `
  'search येशू ख्रीष्‍ट in npiulb in romans' `
  @('search', 'येशू ख्रीष्‍ट', 'in', 'npiulb', 'in', 'romans') `
  @'
रोमी 1:1 प्रेरित हुनको निम्ति बोलाइएका र सुसमाचारको कामको निम्ति अलग गरिएका, येशू ख्रीष्‍टका दास पावल ।
'@

Add-Test `
  'NPIULB' `
  'search येशू ख्रीष्‍ट in npiulb in romans 2' `
  @('search', 'येशू ख्रीष्‍ट', 'in', 'npiulb', 'in', 'romans', '2') `
  @'
रोमी 2:16 अनि परमेश्‍वरप्रति पनि यही कुरो लागु हुन्छ । त्यो मेरो सुसमाचारअनुसार येशू ख्रीष्‍टद्वारा परमेश्‍वरले सबै मानिसहरूको गोप्य कुराहरूको इन्साफ गर्नुहुने दिन हुनेछ ।
'@

Add-Test `
  'NPIULB' `
  'search येशू ख्रीष्‍ट in npiulb in romans 3-5' `
  @('search', 'येशू ख्रीष्‍ट', 'in', 'npiulb', 'in', 'romans', '3-5') `
  @'
रोमी 3:22 अर्थात् यो विश्‍वास गर्ने सबैका निम्ति येशू ख्रीष्‍टमा विश्‍वासद्वारा आउने परमेश्‍वरको धार्मिकता हो । किनकि त्यहाँ कुनै भेदभाव छैन ।
'@

Add-Test `
  'NPIULB' `
  'search येशू ख्रीष्‍ट in npiulb in "johns letters"' `
  @('search', 'येशू ख्रीष्‍ट', 'in', 'npiulb', 'in', 'johns letters') `
  @'
१ यूहन्ना 1:3 जुन हामीले देखेका र सुनेका छौँ, हामी तिमीहरूलाई पनि घोषणा गर्छौं, ताकि हामीहरूसँग तिमीहरूको सङ्गति होस् । हाम्रो सङ्गति पिता र उहाँको पुत्र येशू ख्रीष्‍टसँग हुन्छ ।
'@


# --- ABTAG ---
foreach ($t in @('Jesucristo', 'Jesus', 'Cristo')) {
  Add-Test `
    'ABTAG' `
    "search $t in abtag" `
    @('search', $t, 'in', 'abtag') `
    'MATEO 1:1 Ang aklat ng lahi ni Jesucristo, na anak ni David, na anak ni Abraham.'
}

Add-Test `
  'ABTAG' `
  'search Jesucristo in abtag in romans' `
  @('search', 'Jesucristo', 'in', 'abtag', 'in', 'romans') `
  'MGA TAGA ROMA 1:1 Si Pablo na alipin ni Jesucristo, na tinawag na maging apostol, ibinukod sa evangelio ng Dios,'

Add-Test `
  'ABTAG' `
  'search Jesucristo in abtag in romans 2' `
  @('search', 'Jesucristo', 'in', 'abtag', 'in', 'romans', '2') `
  'MGA TAGA ROMA 2:16 Sa araw na hahatulan ng Dios ang mga lihim ng mga tao, ayon sa aking evangelio, sa pamamagitan ni Jesucristo.'

Add-Test `
  'ABTAG' `
  'search Jesucristo in abtag in romans 3-5' `
  @('search', 'Jesucristo', 'in', 'abtag', 'in', 'romans', '3-5') `
  "MGA TAGA ROMA 3:22 Sa makatuwid baga'y ang katuwiran ng Dios sa pamamagitan ng pananampalataya kay Jesucristo sa lahat ng mga nagsisisampalataya; sapagka't walang pagkakaiba;"

Add-Test `
  'ABTAG' `
  'search Jesucristo in abtag in "johns letters"' `
  @('search', 'Jesucristo', 'in', 'abtag', 'in', 'johns letters') `
  'I JUAN 1:3 Yaong aming nakita at narinig ay siya rin naming ibinabalita sa inyo, upang kayo naman ay magkaroon ng pakikisama sa amin: oo, at tayo ay may pakikisama sa Ama, at sa kaniyang Anak na si Jesucristo:'

# --- KTTV ---
Add-Test `
  'KTTV' `
  'search Đức Chúa Jêsus Christ in kttv' `
  @('search', 'Đức Chúa Jêsus Christ', 'in', 'kttv') `
  @'
Ma-thi-ơ 1:18 Vả, sự giáng-sanh của Đức Chúa Jêsus-Christ đã xảy ra như vầy: Khi Ma-ri, mẹ Ngài, đã hứa gả cho Giô-sép, song chưa ăn-ở cùng nhau, thì người đã chịu thai bởi Đức Thánh-Linh.
'@

Add-Test `
  'KTTV' `
  'search Đức Chúa Jêsus in kttv' `
  @('search', 'Đức Chúa Jêsus', 'in', 'kttv') `
  @'
Ma-thi-ơ 1:16 Gia-cốp sanh Giô-sép là chồng Ma-ri; Ma-ri là người sanh Đức Chúa Jêsus, gọi là Christ.
'@

Add-Test `
  'KTTV' `
  'search Christ in kttv' `
  @('search', 'Christ', 'in', 'kttv') `
  @'
Ma-thi-ơ 1:1 Gia-phổ Đức Chúa Jêsus-Christ, con cháu Đa-vít và con cháu Áp-ra-ham.
'@

Add-Test `
  'KTTV' `
  'search Đức Chúa Jêsus Christ in kttv in romans' `
  @('search', 'Đức Chúa Jêsus Christ', 'in', 'kttv', 'in', 'romans') `
  @'
Rô-ma 1:1 Phao-lô, tôi-tớ của Đức Chúa Jêsus-Christ, được gọi làm sứ-đồ, để riêng ra đặng giảng Tin-lành Đức Chúa Trời, —
'@

Add-Test `
  'KTTV' `
  'search Đức Chúa Jêsus Christ in kttv in romans 2' `
  @('search', 'Đức Chúa Jêsus Christ', 'in', 'kttv', 'in', 'romans', '2') `
  @'
Rô-ma 2:16 Ấy là điều sẽ hiện ra trong ngày Đức Chúa Trời bởi Đức Chúa Jêsus-Christ mà xét-đoán những việc kín-nhiệm của loài người, y theo Tin-lành tôi.
'@

Add-Test `
  'KTTV' `
  'search Đức Chúa Jêsus Christ in kttv in romans 3-5' `
  @('search', 'Đức Chúa Jêsus Christ', 'in', 'kttv', 'in', 'romans', '3-5') `
  @'
Rô-ma 3:22 tức là sự công-bình của Đức Chúa Trời, bởi sự tin đến Đức Chúa Jêsus-Christ, cho mọi người nào tin. Chẳng có phân-biệt chi hết,
'@

Add-Test `
  'KTTV' `
  'search Đức Chúa Jêsus Christ in kttv in "johns letters"' `
  @('search', 'Đức Chúa Jêsus Christ', 'in', 'kttv', 'in', 'johns letters') `
  @'
I Giăng 3:23 Vả, nầy là điều-răn của Ngài: là chúng ta phải tin đến danh Con Ngài, tức là Đức Chúa Jêsus-Christ, và chúng ta phải yêu-mến lẫn nhau như Ngài đã truyền-dạy ta.
'@

# --- IRVGUJ ---
foreach ($t in @('ઈસુ ખ્રિસ્ત', 'ઈસુ', 'ખ્રિસ્ત')) {
  Add-Test `
    'IRVGUJ' `
    "search $t in irvguj" `
    @('search', $t, 'in', 'irvguj') `
    @'
માથ. 1:1 ઈસુ ખ્રિસ્ત જે ઇબ્રાહિમનાં દીકરા, જે દાઉદના દીકરા, તેમની વંશાવળી.
'@
}

Add-Test `
  'IRVGUJ' `
  'search ઈસુ ખ્રિસ્ત in irvguj in romans' `
  @('search', 'ઈસુ ખ્રિસ્ત', 'in', 'irvguj', 'in', 'romans') `
  @'
રોમ. 1:1 પ્રેરિત થવા સારુ તેડાયેલો અને ઈશ્વરની સુવાર્તા માટે અલગ કરાયેલો ઈસુ ખ્રિસ્તનો સેવક પાઉલ, રોમમાં રહેતા, ઈશ્વરના વહાલા અને પવિત્ર થવા સારુ પસંદ કરાયેલા સર્વ લોકોને લખે છે
'@

Add-Test `
  'IRVGUJ' `
  'search ઈસુ ખ્રિસ્ત in irvguj in romans 2' `
  @('search', 'ઈસુ ખ્રિસ્ત', 'in', 'irvguj', 'in', 'romans', '2') `
  @'
રોમ. 2:16 ઈશ્વર મારી સુવાર્તા પ્રમાણે ઈસુ ખ્રિસ્તની મારફતે મનુષ્યોના ગુપ્ત કામોનો ન્યાય કરશે, તે દિવસે એમ થશે.
'@

Add-Test `
  'IRVGUJ' `
  'search ઈસુ ખ્રિસ્ત in irvguj in romans 3-5' `
  @('search', 'ઈસુ ખ્રિસ્ત', 'in', 'irvguj', 'in', 'romans', '3-5') `
  @'
રોમ. 3:22 એટલે ઈશ્વરનું ન્યાયીપણું, જે ઈસુ ખ્રિસ્ત પરના વિશ્વાસદ્વારા સર્વ વિશ્વાસ કરનારાઓને માટે છે તે; કેમ કે એમાં કંઈ પણ તફાવત નથી.
'@

Add-Test `
  'IRVGUJ' `
  'search ઈસુ ખ્રિસ્ત in irvguj in "johns letters"' `
  @('search', 'ઈસુ ખ્રિસ્ત', 'in', 'irvguj', 'in', 'johns letters') `
  @'
1 યોહ. 1:3 હા, અમારી સાથે તમારી પણ સંગત થાય, એ માટે જે અમે જોયું તથા સાંભળ્યું છે, તે તમને પણ જાહેર કરીએ છીએ; અને ખરેખર અમારી સંગત પિતાની સાથે તથા તેમના પુત્ર ઈસુ ખ્રિસ્તની સાથે છે.
'@

# --- IRVMAR ---
foreach ($t in @('येशू ख्रिस्त', 'येशू', 'ख्रिस्त')) {
  Add-Test `
    'IRVMAR' `
    "search $t in irvmar" `
    @('search', $t, 'in', 'irvmar') `
    @'
मत्त. 1:1 अब्राहामाचा पुत्र दावीद याचा पुत्र जो येशू ख्रिस्त याची वंशावळ.
'@
}

Add-Test `
  'IRVMAR' `
  'search येशू ख्रिस्त in irvmar in romans' `
  @('search', 'येशू ख्रिस्त', 'in', 'irvmar', 'in', 'romans') `
  @'
रोम. 1:1 प्रेषित होण्यास बोलावलेला, येशू ख्रिस्ताचा दास, देवाच्या सुवार्तेसाठी वेगळा केलेला, पौल ह्याजकडून;
'@

Add-Test `
  'IRVMAR' `
  'search येशू ख्रिस्त in irvmar in romans 2' `
  @('search', 'येशू ख्रिस्त', 'in', 'irvmar', 'in', 'romans', '2') `
  @'
रोम. 2:16 देव, माझ्या सुवार्तेप्रमाणे जेव्हा मनुष्यांच्या गुप्त गोष्टींचा ख्रिस्त येशूकडून न्याय करील त्यादिवशी हे दिसून येईल.
'@

Add-Test `
  'IRVMAR' `
  'search येशू ख्रिस्त in irvmar in romans 3-5' `
  @('search', 'येशू ख्रिस्त', 'in', 'irvmar', 'in', 'romans', '3-5') `
  @'
रोम. 3:22 पण हे देवाचे नीतिमत्त्व येशू ख्रिस्तावरील विश्वासाद्वारे, विश्वास ठेवणार्‍या सर्वांसाठी आहे कारण तेथे कसलाही फरक नाही.
'@

Add-Test `
  'IRVMAR' `
  'search येशू ख्रिस्त in irvmar in "johns letters"' `
  @('search', 'येशू ख्रिस्त', 'in', 'irvmar', 'in', 'johns letters') `
  @'
1 योहा. 1:3 आम्ही जे पाहिले व ऐकले आहे ते आम्ही आता तुम्हांलाही घोषित करीत आहोत, यासाठी की तुमचीही आमच्यासोबत सहभागिता असावी. आमची सहभागिता तर देवपिता व त्याचा पुत्र येशू ख्रिस्त याजबरोबर आहे.
'@

# --- IRVTEL ---
foreach ($t in @('యేసు క్రీస్తు', 'యేసు', 'క్రీస్తు')) {
  Add-Test `
    'IRVTEL' `
    "search $t in irvtel" `
    @('search', $t, 'in', 'irvtel') `
    @'
మత్తయి 1:1 అబ్రాహాము వంశం వాడైన దావీదు వంశం వాడు యేసు క్రీస్తు వంశావళి.
'@
}

Add-Test `
  'IRVTEL' `
  'search యేసు క్రీస్తు in irvtel in romans' `
  @('search', 'యేసు క్రీస్తు', 'in', 'irvtel', 'in', 'romans') `
  @'
రోమా పత్రిక 1:1 యేసు క్రీస్తు దాసుడు, అపోస్తలుడుగా పిలుపు పొందినవాడు, దేవుని సువార్త కోసం ప్రభువు ప్రత్యేకించుకున్న
'@

Add-Test `
  'IRVTEL' `
  'search యేసు క్రీస్తు in irvtel in romans 2' `
  @('search', 'యేసు క్రీస్తు', 'in', 'irvtel', 'in', 'romans', '2') `
  @'
రోమా పత్రిక 2:16 నా సువార్త ప్రకారం దేవుడు యేసు క్రీస్తు ద్వారా మానవుల రహస్యాలను విచారించే రోజున ఈ విధంగా జరుగుతుంది.
'@

Add-Test `
  'IRVTEL' `
  'search యేసు క్రీస్తు in irvtel in romans 3-5' `
  @('search', 'యేసు క్రీస్తు', 'in', 'irvtel', 'in', 'romans', '3-5') `
  @'
రోమా పత్రిక 3:22 అది యేసు క్రీస్తులో విశ్వాసమూలంగా నమ్మే వారందరికీ కలిగే దేవుని నీతి.
'@

Add-Test `
  'IRVTEL' `
  'search యేసు క్రీస్తు in irvtel in "johns letters"' `
  @('search', 'యేసు క్రీస్తు', 'in', 'irvtel', 'in', 'johns letters') `
  @'
1 యోహాను పత్రిక 1:3 మీరు కూడా మాతో సహవాసం కలిగి ఉండాలని మేము చూసిందీ, విన్నదీ మీకు ప్రకటిస్తున్నాం. నిజానికి మన సహవాసం తండ్రితోను, ఆయన కుమారుడు యేసు క్రీస్తుతోను ఉంది.
'@

# --- IRVURD ---
foreach ($t in @('ईसा मसीह', 'ईसा')) {
  Add-Test `
    'IRVURD' `
    "search $t in irvurd" `
    @('search', $t, 'in', 'irvurd') `
    @'
मत्त 1:1 ईसा मसीह इबने दाऊद इबने इब्राहीम का नसबनामा।
'@
}

Add-Test `
  'IRVURD' `
  'search मसीह in irvurd' `
  @('search', 'मसीह', 'in', 'irvurd') `
  @'
ज़बूर 2:2 ख़ुदावन्द और उसके मसीह के ख़िलाफ़ ज़मीन के बादशाह एक हो कर, और हाकिम आपस में मशवरा करके कहते हैं,
'@

Add-Test `
  'IRVURD' `
  'search ईसा मसीह in irvurd in romans' `
  @('search', 'ईसा मसीह', 'in', 'irvurd', 'in', 'romans') `
  @'
रोमि 1:1 पौलुस की तरफ़ से जो ईसा मसीह का बन्दा है और रसूल होने के लिए बुलाया गया और ख़ुदा की उस ख़ुशख़बरी के लिए अलग किया गया।
'@

Add-Test `
  'IRVURD' `
  'search ईसा मसीह in irvurd in romans 2' `
  @('search', 'ईसा मसीह', 'in', 'irvurd', 'in', 'romans', '2') `
  @'
रोमि 2:16 जिस रोज़ ख़ुदा ख़ुशख़बरी के मुताबिक़ जो मै ऐलान करता हूँ ईसा मसीह की मारिफ़त आदमियों की छुपी बातों का इन्साफ़ करेगा।
'@

Add-Test `
  'IRVURD' `
  'search ईसा मसीह in irvurd in romans 3-5' `
  @('search', 'ईसा मसीह', 'in', 'irvurd', 'in', 'romans', '3-5') `
  @'
रोमि 3:22 यानी ख़ुदा की वो रास्तबाज़ी जो ईसा मसीह पर ईमान लाने से सब ईमान लानेवालों को हासिल होती है; क्यूँकि कुछ फ़र्क़ नहीं।
'@

Add-Test `
  'IRVURD' `
  'search ईसा मसीह in irvurd in "johns letters"' `
  @('search', 'ईसा मसीह', 'in', 'irvurd', 'in', 'johns letters') `
  @'
1 यूह 1:3 जो कुछ हम ने देखा और सुना है तुम्हें भी उसकी ख़बर देते है, ताकि तुम भी हमारे शरीक हो, और हमारा मेल मिलाप बाप के साथ और उसके बेटे ईसा मसीह के साथ है।
'@

Write-Host ""
Write-Host "Running bbl install/search E2E tests"
Write-Host "bbl: $BblPath"
Write-Host "tests: $($script:tests.Count)"
Write-Host "throttle: $ThrottleLimit"

$sw = [Diagnostics.Stopwatch]::StartNew()

$serialOrders = [System.Collections.Generic.HashSet[int]]::new()
$serialTests = @(
  $script:tests | Where-Object {
    $cliArgs = @(@($_.CliArgs) | ForEach-Object { [string]$_ })
    $cliArgs.Count -eq 1 -and (
      $cliArgs[0] -eq 'search　イエス　キリスト　in　jc' -or
        $cliArgs[0] -eq 'search　イエス　in　jc'
    )
  }
)
$serialTests | ForEach-Object { [void]$serialOrders.Add([int]$_.Order) }
$parallelTests = @($script:tests | Where-Object { -not $serialOrders.Contains([int]$_.Order) })

$results = $parallelTests | ForEach-Object -Parallel {
  $test = $_
  $bbl = $Using:BblPath

  [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
  $OutputEncoding = [System.Text.Encoding]::UTF8

  $caseSw = [Diagnostics.Stopwatch]::StartNew()

  $cliArgs = @(@($test.CliArgs) | ForEach-Object { [string]$_ })
  $expected = [string]$test.ExpectedLine
  $expectedOutput = [string]$test.ExpectedOutput

  $text = ""
  $errorMessage = $null
  $passed = $false
  $exitCode = $null

  try {
    $output = & $bbl @cliArgs 2>&1
    $exitCode = $LASTEXITCODE
    $allText = $output | Out-String
    $text = ($allText -split "`r?`n" | Where-Object { $_.Trim() -ne "" } | Select-Object -First 1)

    if ($exitCode -ne 0) {
      throw "bbl exited with code $exitCode`n$allText"
    }

    if ($text -ne $expected) {
      throw "expected first line:`n$expected`nactual first line:`n$text"
    }

    $passed = $true
  } catch {
    $errorMessage = $_.ToString()
  } finally {
    $caseSw.Stop()
  }

  [pscustomobject]@{
    Order     = [int]$test.Order
    Group     = [string]$test.Group
    Name      = [string]$test.Name
    CliArgs   = [string[]]$cliArgs
    Passed    = [bool]$passed
    Error     = $errorMessage
    ExitCode  = $exitCode
    ElapsedMs = [int64]$caseSw.ElapsedMilliseconds
  }
} -ThrottleLimit $ThrottleLimit

$serialResults = foreach ($test in $serialTests) {
  [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
  $OutputEncoding = [System.Text.Encoding]::UTF8

  $caseSw = [Diagnostics.Stopwatch]::StartNew()

  $cliArgs = @(@($test.CliArgs) | ForEach-Object { [string]$_ })
  $expected = [string]$test.ExpectedLine
  $expectedOutput = [string]$test.ExpectedOutput

  $errorMessage = $null
  $passed = $false
  $exitCode = $null

  try {
    if ($cliArgs.Count -eq 1) {
      $stdoutFile = New-TemporaryFile
      $stderrFile = New-TemporaryFile
      try {
        $process = Start-Process `
          -FilePath $BblPath `
          -ArgumentList $cliArgs[0] `
          -NoNewWindow `
          -Wait `
          -PassThru `
          -RedirectStandardOutput $stdoutFile `
          -RedirectStandardError $stderrFile
        $exitCode = $process.ExitCode
        $stdoutText = Get-Content -LiteralPath $stdoutFile -Raw -Encoding UTF8
        $stderrText = Get-Content -LiteralPath $stderrFile -Raw -Encoding UTF8
        $allText = $stdoutText + $stderrText
      } finally {
        Remove-Item -LiteralPath $stdoutFile, $stderrFile -Force -ErrorAction SilentlyContinue
      }
    } else {
      $output = & $BblPath @cliArgs 2>&1
      $exitCode = $LASTEXITCODE
      $allText = $output | Out-String
    }
    $text = ($allText -split "`r?`n" | Where-Object { $_.Trim() -ne "" } | Select-Object -First 1)

    if ($exitCode -ne 0) {
      throw "bbl exited with code $exitCode`n$allText"
    }

    if ($expectedOutput) {
      $actualOutput = (($allText -split "`r?`n" | Where-Object { $_.Trim() -ne "" }) -join "`n")
      $normalizedExpectedOutput = (($expectedOutput -split "`r?`n" | Where-Object { $_.Trim() -ne "" }) -join "`n")
      if ($actualOutput -ne $normalizedExpectedOutput) {
        throw "expected output:`n$normalizedExpectedOutput`nactual output:`n$actualOutput"
      }
    } elseif ($text -ne $expected) {
      throw "expected first line:`n$expected`nactual first line:`n$text"
    }

    $passed = $true
  } catch {
    $errorMessage = $_.ToString()
  } finally {
    $caseSw.Stop()
  }

  [pscustomobject]@{
    Order     = [int]$test.Order
    Group     = [string]$test.Group
    Name      = [string]$test.Name
    CliArgs   = [string[]]$cliArgs
    Passed    = [bool]$passed
    Error     = $errorMessage
    ExitCode  = $exitCode
    ElapsedMs = [int64]$caseSw.ElapsedMilliseconds
  }
}

$sw.Stop()

$orderedResults = @($results) + @($serialResults) | Sort-Object Order

$currentGroup = $null

foreach ($result in $orderedResults) {
  if ($result.Group -ne $currentGroup) {
    $currentGroup = $result.Group
    Write-Host ""
    Write-Host $currentGroup
  }

  $seconds = [Math]::Round($result.ElapsedMs / 1000.0, 2)

  if ($result.Passed) {
    Write-Host "  [PASS] $($result.Name) (${seconds}s)"
  } else {
    Write-Host "  [FAIL] $($result.Name) (${seconds}s)"
  }
}

$passedCount = @($orderedResults | Where-Object { $_.Passed }).Count
$failedCount = @($orderedResults | Where-Object { -not $_.Passed }).Count

$total = [int]$sw.Elapsed.TotalSeconds

$timeStr = if ($total -lt 60) {
  "${total}s"
} else {
  $min = [Math]::Floor($total / 60)
  $sec = $total % 60
  "${min}m ${sec}s"
}

Write-Host ""
Write-Host "Test Summary: $passedCount successful, $failedCount failures"
Write-Host "Elapsed: $timeStr"

if ($failedCount -gt 0) {
  Write-Host ""
  Write-Host "Failures:"

  foreach ($failure in ($orderedResults | Where-Object { -not $_.Passed })) {
    Write-Host ""
    Write-Host "  [FAIL] $($failure.Name)"
    Write-Host "         Group: $($failure.Group)"
    Write-Host "         Args: $($failure.CliArgs -join ' ')"

    if ($null -ne $failure.ExitCode) {
      Write-Host "         ExitCode: $($failure.ExitCode)"
    }

    Write-Host "         $($failure.Error)"
  }

  exit 1
}

exit 0

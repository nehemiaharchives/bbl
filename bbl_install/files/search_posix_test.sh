#!/usr/bin/env bash
# bbl search E2E tests for macOS/Linux
# One-to-one Bash equivalent of bbl_install/test/integration/search_script/test_search.ps1

set -u

BblPath=""
ThrottleLimit=4
SearchTimeoutSeconds=60
RetryTimeoutFailures=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    -BblPath|--bbl-path)
      if [[ $# -lt 2 ]]; then
        echo "ERROR: $1 requires a value."
        exit 1
      fi
      BblPath="$2"
      shift 2
      ;;
    -ThrottleLimit|--throttle-limit)
      if [[ $# -lt 2 ]]; then
        echo "ERROR: $1 requires a value."
        exit 1
      fi
      ThrottleLimit="$2"
      shift 2
      ;;
    -SearchTimeoutSeconds|--search-timeout-seconds)
      if [[ $# -lt 2 ]]; then
        echo "ERROR: $1 requires a value."
        exit 1
      fi
      SearchTimeoutSeconds="$2"
      shift 2
      ;;
    -RetryTimeoutFailures|--retry-timeout-failures)
      if [[ $# -lt 2 ]]; then
        echo "ERROR: $1 requires a value."
        exit 1
      fi
      RetryTimeoutFailures="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [-BblPath /path/to/bbl] [-ThrottleLimit N] [-SearchTimeoutSeconds N] [-RetryTimeoutFailures N]"
      exit 0
      ;;
    *)
      if [[ -z "$BblPath" ]]; then
        BblPath="$1"
      else
        echo "ERROR: unknown argument: $1"
        exit 1
      fi
      shift
      ;;
  esac
done

if ! [[ "$ThrottleLimit" =~ ^[0-9]+$ ]] || [[ "$ThrottleLimit" -lt 1 ]]; then
  echo "ERROR: -ThrottleLimit must be 1 or greater."
  exit 1
fi

if ! [[ "$SearchTimeoutSeconds" =~ ^[0-9]+$ ]] || [[ "$SearchTimeoutSeconds" -lt 1 ]]; then
  echo "ERROR: -SearchTimeoutSeconds must be 1 or greater."
  exit 1
fi

if ! [[ "$RetryTimeoutFailures" =~ ^[0-9]+$ ]]; then
  echo "ERROR: -RetryTimeoutFailures must be 0 or greater."
  exit 1
fi

if [[ -z "$BblPath" ]]; then
  candidates=(
    "$HOME/.bbl/bin/bbl"
    "$HOME/.bbl/bbl"
    "/usr/local/bin/bbl"
    "/opt/homebrew/bin/bbl"
  )

  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate" || -f "$candidate" ]]; then
      BblPath="$candidate"
      break
    fi
  done

  if [[ -z "$BblPath" ]] && command -v bbl >/dev/null 2>&1; then
    BblPath="$(command -v bbl)"
  fi

  if [[ -z "$BblPath" ]]; then
    echo "ERROR: bbl not found. Provide -BblPath or ensure it is installed."
    exit 1
  fi
fi

if [[ ! -f "$BblPath" && ! -x "$BblPath" ]]; then
  echo "ERROR: bbl not found at: $BblPath"
  exit 1
fi

TEST_GROUPS=()
NAMES=()
EXPECTED_LINES=()
EXPECTED_OUTPUTS=()
CLI_ARGS_JOINED=()
ORDER=0
US=$'\037'

join_args() {
  local first=1
  local arg
  for arg in "$@"; do
    if [[ $first -eq 1 ]]; then
      printf '%s' "$arg"
      first=0
    else
      printf '%s%s' "$US" "$arg"
    fi
  done
}

Add_Test() {
  local group="$1"
  local name="$2"
  local expected="$3"
  shift 3

  TEST_GROUPS+=("$group")
  NAMES+=("$name")
  EXPECTED_LINES+=("$expected")
  EXPECTED_OUTPUTS+=("")
  CLI_ARGS_JOINED+=("$(join_args "$@")")
  ORDER=$((ORDER + 1))
}

Add_Test_With_Output() {
  local group="$1"
  local name="$2"
  local expected="$3"
  local expected_output="$4"
  shift 4

  TEST_GROUPS+=("$group")
  NAMES+=("$name")
  EXPECTED_LINES+=("$expected")
  EXPECTED_OUTPUTS+=("$expected_output")
  CLI_ARGS_JOINED+=("$(join_args "$@")")
  ORDER=$((ORDER + 1))
}

# --- WEBUS (default translation) ---
for t in 'Jesus Christ' 'Jesus' 'Christ'; do
  Add_Test 'WEBUS (default)' "search $t" \
    'Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.' \
    'search' "$t"
done

Add_Test 'WEBUS (default)' 'search "Jesus wept" exact' \
  'John 11:35 Jesus wept.' \
  'search' 'Jesus wept'

Add_Test 'WEBUS (default)' 'search Jesus wept unquoted' \
  'Matthew 26:75 Peter remembered the word which Jesus had said to him, “Before the rooster crows, you will deny me three times.” Then he went out and wept bitterly.' \
  'search' 'Jesus' 'wept'

Add_Test 'WEBUS (default)' 'search Jesus Christ in romans' \
  'Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,' \
  'search' 'Jesus Christ' 'in' 'romans'

Add_Test 'WEBUS (default)' 'search Jesus Christ in "johns letters"' \
  '1 John 1:3 that which we have seen and heard we declare to you, that you also may have fellowship with us. Yes, and our fellowship is with the Father and with his Son, Jesus Christ.' \
  'search' 'Jesus Christ' 'in' 'johns letters'

Add_Test 'WEBUS (default)' 'search Jesus weep stemming' \
  'Matthew 26:75 Peter remembered the word which Jesus had said to him, “Before the rooster crows, you will deny me three times.” Then he went out and wept bitterly.' \
  'search' 'Jesus' 'weep'

# --- KJV ---
for t in 'Jesus Christ' 'Jesus' 'Christ'; do
  Add_Test 'KJV' "search $t in kjv" \
    'Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.' \
    'search' "$t" 'in' 'kjv'
done

Add_Test 'KJV' 'search Jesus Christ --translation kjv' \
  'Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.' \
  'search' 'Jesus Christ' '--translation' 'kjv'

Add_Test 'KJV' 'search "Jesus wept" in kjv exact' \
  'John 11:35 Jesus wept.' \
  'search' 'Jesus wept' 'in' 'kjv'

Add_Test 'KJV' 'search Jesus wept in kjv' \
  'Matthew 26:75 And Peter remembered the word of Jesus, which said unto him, Before the cock crow, thou shalt deny me thrice. And he went out, and wept bitterly.' \
  'search' 'Jesus' 'wept' 'in' 'kjv'

Add_Test 'KJV' 'search Jesus Christ in kjv in romans' \
  'Romans 1:1 Paul, a servant of Jesus Christ, called [to be] an apostle, separated unto the gospel of God,' \
  'search' 'Jesus Christ' 'in' 'kjv' 'in' 'romans'

Add_Test 'KJV' 'search Jesus Christ in "johns letters" in kjv' \
  '1 John 1:3 That which we have seen and heard declare we unto you, that ye also may have fellowship with us: and truly our fellowship [is] with the Father, and with his Son Jesus Christ.' \
  'search' 'Jesus Christ' 'in' 'johns letters' 'in' 'kjv'

Add_Test 'KJV' 'search Jesus weep in kjv stemming' \
  'Matthew 26:75 And Peter remembered the word of Jesus, which said unto him, Before the cock crow, thou shalt deny me thrice. And he went out, and wept bitterly.' \
  'search' 'Jesus' 'weep' 'in' 'kjv'

Add_Test_With_Output 'KJV' 'search Olivet in kjv jc krv compares translations' \
  '2 Samuel 15:30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up.' "$(cat <<'EOT'
2 Samuel 15:30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up.
サムエル記下 15:30 ダビデはオリブ山の坂道を登ったが、登る時に泣き、その頭をおおい、はだしで行った。彼と共にいる民もみな頭をおおって登り、泣きながら登った。
사무엘하 15:30 다윗이 감람산 길로 올라갈 때에 머리를 가리우고 맨발로 울며 행하고 저와 함께 가는 백성들도 각각 그 머리를 가리우고 울며 올라가니라
EOT
)" 'search' 'Olivet' 'in' 'kjv' 'jc' 'krv'

# --- RVR09 ---
for t in 'Jesucristo' 'Jesús' 'Cristo'; do
  Add_Test 'RVR09' "search $t in rvr09" \
    'Mateo 1:1 LIBRO de la generación de Jesucristo, hijo de David, hijo de Abraham.' \
    'search' "$t" 'in' 'rvr09'
done

Add_Test 'RVR09' 'search Jesucristo in rvr09 in romans' \
  'Romanos 1:1 PABLO, siervo de Jesucristo, llamado á ser apóstol, apartado para el evangelio de Dios,' \
  'search' 'Jesucristo' 'in' 'rvr09' 'in' 'romans'

Add_Test 'RVR09' 'search Jesucristo in rvr09 in romans 2' \
  'Romanos 2:16 En el día que juzgará el Señor lo encubierto de los hombres, conforme á mi evangelio, por Jesucristo.' \
  'search' 'Jesucristo' 'in' 'rvr09' 'in' 'romans' '2'

Add_Test 'RVR09' 'search Jesucristo in rvr09 in romans 3-5' \
  'Romanos 3:22 La justicia de Dios por la fe de Jesucristo, para todos los que creen en él: porque no hay diferencia;' \
  'search' 'Jesucristo' 'in' 'rvr09' 'in' 'romans' '3-5'

Add_Test 'RVR09' 'search Jesucristo in "johns letters" in rvr09' \
  '1 Juan 1:3 Lo que hemos visto y oído, eso os anunciamos, para que también vosotros tengáis comunión con nosotros: y nuestra comunión verdaderamente es con el Padre, y con su Hijo Jesucristo.' \
  'search' 'Jesucristo' 'in' 'johns letters' 'in' 'rvr09'

# --- TB ---
for t in 'Jesus Cristo' 'Jesus' 'Cristo'; do
  Add_Test 'TB' "search $t in tb" \
    'Mateus 1:1 Livro da geração de Jesus Cristo, filho de Davi, filho de Abraão.' \
    'search' "$t" 'in' 'tb'
done

Add_Test 'TB' 'search Jesus Cristo in tb in romans' \
  'Romanos 1:4 e que foi com poder declarado Filho de Deus, quanto ao espírito de santidade, pela ressurreição dos mortos), Jesus Cristo, nosso Senhor,' \
  'search' 'Jesus Cristo' 'in' 'tb' 'in' 'romans'

Add_Test 'TB' 'search Jesus Cristo in tb in romans 3-5' \
  'Romanos 3:22 a saber, a justiça de Deus mediante a fé em Jesus Cristo, para com todos os que creem. Pois não há distinção,' \
  'search' 'Jesus Cristo' 'in' 'tb' 'in' 'romans' '3-5'

Add_Test 'TB' 'search Jesus Cristo in tb in "johns letters"' \
  '1 João 1:3 o que temos visto e ouvido também vo-lo anunciamos, para que vós também tenhais comunhão conosco. A nossa comunhão é com o Pai e com seu Filho, Jesus Cristo.' \
  'search' 'Jesus Cristo' 'in' 'tb' 'in' 'johns letters'

# --- DELUT ---
for t in 'Jesu Christi' 'Jesu' 'Christi'; do
  Add_Test 'DELUT' "search $t in delut" "$(cat <<'EOT'
Matthäus 1:1 Dies ist das Buch von der Geburt Jesu Christi, der da ist ein Sohn Davids, des Sohnes Abrahams.
EOT
)" 'search' "$t" 'in' 'delut'
done

Add_Test 'DELUT' 'search Jesu Christi in delut in romans' "$(cat <<'EOT'
Römer 1:1 Paulus, ein Knecht Jesu Christi, berufen zum Apostel, ausgesondert, zu predigen das Evangelium Gottes,
EOT
)" 'search' 'Jesu Christi' 'in' 'delut' 'in' 'romans'

Add_Test 'DELUT' 'search Jesu Christi in delut in romans 2' "$(cat <<'EOT'
Römer 2:16 auf den Tag, da Gott das Verborgene der Menschen durch Jesus Christus richten wird laut meines Evangeliums.
EOT
)" 'search' 'Jesu Christi' 'in' 'delut' 'in' 'romans' '2'

Add_Test 'DELUT' 'search Jesu Christi in delut in romans 3-5' "$(cat <<'EOT'
Römer 3:22 Ich sage aber von solcher Gerechtigkeit vor Gott, die da kommt durch den Glauben an Jesum Christum zu allen und auf alle, die da glauben.
EOT
)" 'search' 'Jesu Christi' 'in' 'delut' 'in' 'romans' '3-5'

Add_Test 'DELUT' 'search Jesu Christi in delut in "johns letters"' "$(cat <<'EOT'
1. Johannes 1:3 was wir gesehen und gehört haben, das verkündigen wir euch, auf daß ihr mit uns Gemeinschaft habt; und unsre Gemeinschaft ist mit dem Vater und mit seinem Sohn Jesus Christus.
EOT
)" 'search' 'Jesu Christi' 'in' 'delut' 'in' 'johns letters'

# --- LSG ---
for t in 'Jésus-Christ' 'Jésus' 'Christ'; do
  Add_Test 'LSG' "search $t in lsg" "$(cat <<'EOT'
Matthieu 1:1 Généalogie de Jésus-Christ, fils de David, fils d’Abraham.
EOT
)" 'search' "$t" 'in' 'lsg'
done

Add_Test 'LSG' 'search Jésus-Christ in lsg in romans' "$(cat <<'EOT'
Romains 1:1 Paul, serviteur de Jésus-Christ, appelé à être apôtre, mis à part pour annoncer l’Évangile de Dieu,
EOT
)" 'search' 'Jésus-Christ' 'in' 'lsg' 'in' 'romans'

Add_Test 'LSG' 'search Jésus-Christ in lsg in romans 2' "$(cat <<'EOT'
Romains 2:16 C’est ce qui paraîtra au jour où, selon mon Évangile, Dieu jugera par Jésus-Christ les actions secrètes des hommes.
EOT
)" 'search' 'Jésus-Christ' 'in' 'lsg' 'in' 'romans' '2'

Add_Test 'LSG' 'search Jésus-Christ in lsg in romans 3-5' "$(cat <<'EOT'
Romains 3:22 justice de Dieu par la foi en Jésus-Christ pour tous ceux qui croient. Il n’y a point de distinction.
EOT
)" 'search' 'Jésus-Christ' 'in' 'lsg' 'in' 'romans' '3-5'

Add_Test 'LSG' 'search Jésus-Christ in lsg in "johns letters"' "$(cat <<'EOT'
1 Jean 1:3 ce que nous avons vu et entendu, nous vous l’annonçons, à vous aussi, afin que vous aussi vous soyez en communion avec nous. Or, notre communion est avec le Père et avec son Fils Jésus-Christ.
EOT
)" 'search' 'Jésus-Christ' 'in' 'lsg' 'in' 'johns letters'

# --- SINOD ---
for t in 'Иисуса Христа' 'Иисуса' 'Христа'; do
  Add_Test 'SINOD' "search $t in sinod" "$(cat <<'EOT'
От Матфея святое благовествование 1:1 Родословие Иисуса Христа, Сына Давидова, Сына Авраамова.
EOT
)" 'search' "$t" 'in' 'sinod'
done

Add_Test 'SINOD' 'search Иисуса Христа in sinod in romans' "$(cat <<'EOT'
Послание к Римлянам 1:1 Павел, раб Иисуса Христа, призванный Апостол, избранный к благовестию Божию,
EOT
)" 'search' 'Иисуса Христа' 'in' 'sinod' 'in' 'romans'
Add_Test 'SINOD' 'search Иисуса Христа in sinod in romans 2' "$(cat <<'EOT'
Послание к Римлянам 2:16 в день, когда, по благовествованию моему, Бог будет судить тайные дела человеков через Иисуса Христа.
EOT
)" 'search' 'Иисуса Христа' 'in' 'sinod' 'in' 'romans' '2'
Add_Test 'SINOD' 'search Иисуса Христа in sinod in romans 3-5' "$(cat <<'EOT'
Послание к Римлянам 3:22 правда Божия через веру в Иисуса Христа во всех и на всех верующих, ибо нет различия,
EOT
)" 'search' 'Иисуса Христа' 'in' 'sinod' 'in' 'romans' '3-5'
Add_Test 'SINOD' 'search Иисуса Христа in sinod in "johns letters"' "$(cat <<'EOT'
Первое послание Иоанна 1:3 о том, что мы видели и слышали, возвещаем вам, чтобы и вы имели общение с нами: а наше общение — с Отцем и Сыном Его, Иисусом Христом.
EOT
)" 'search' 'Иисуса Христа' 'in' 'sinod' 'in' 'johns letters'

# --- SVRJ ---
for t in 'JEZUS CHRISTUS' 'JEZUS' 'CHRISTUS'; do
  Add_Test 'SVRJ' "search $t in svrj" "$(cat <<'EOT'
MATTHEÜS 1:1 Het boek des geslachts van JEZUS CHRISTUS, den Zoon van David, den zoon van Abraham.
EOT
)" 'search' "$t" 'in' 'svrj'
done
Add_Test 'SVRJ' 'search JEZUS CHRISTUS in svrj in romans' "$(cat <<'EOT'
ROMEINEN 1:1 Paulus, een dienstknecht van Jezus Christus, een geroepen apostel, afgezonderd tot het Evangelie van God,
EOT
)" 'search' 'JEZUS CHRISTUS' 'in' 'svrj' 'in' 'romans'
Add_Test 'SVRJ' 'search JEZUS CHRISTUS in svrj in romans 2' "$(cat <<'EOT'
ROMEINEN 2:16 In den dag wanneer God de verborgene dingen der mensen zal oordelen door Jezus Christus, naar mijn Evangelie.
EOT
)" 'search' 'JEZUS CHRISTUS' 'in' 'svrj' 'in' 'romans' '2'
Add_Test 'SVRJ' 'search JEZUS CHRISTUS in svrj in romans 3-5' "$(cat <<'EOT'
ROMEINEN 3:22 Namelijk de rechtvaardigheid Gods door het geloof van Jezus Christus, tot allen, en over allen, die geloven; want er is geen onderscheid.
EOT
)" 'search' 'JEZUS CHRISTUS' 'in' 'svrj' 'in' 'romans' '3-5'
Add_Test 'SVRJ' 'search JEZUS CHRISTUS in svrj in "johns letters"' "$(cat <<'EOT'
1 JOHANNES 1:3 Hetgeen wij dan gezien en gehoord hebben, dat verkondigen wij u, opdat ook gij met ons gemeenschap zoudt hebben, en deze onze gemeenschap ook zij met den Vader, en met Zijn Zoon Jezus Christus.
EOT
)" 'search' 'JEZUS CHRISTUS' 'in' 'svrj' 'in' 'johns letters'

# --- RDV24 ---
for t in 'Gesù Cristo' 'Gesù' 'Cristo'; do
  Add_Test 'RDV24' "search $t in rdv24" "Matteo 1:1 Genealogia di Gesù Cristo figliuolo di Davide, figliuolo d'Abramo." 'search' "$t" 'in' 'rdv24'
done
Add_Test 'RDV24' 'search Gesù Cristo in rdv24 in romans' "$(cat <<'EOT'
EPISTOLE DI S. PAOLO AI~ROMANI 1:4 nato dal seme di Davide secondo la carne, dichiarato Figliuolo di Dio con potenza secondo lo spirito di santità mediante la sua risurrezione dai morti; cioè Gesù Cristo nostro Signore,
EOT
)" 'search' 'Gesù Cristo' 'in' 'rdv24' 'in' 'romans'
Add_Test 'RDV24' 'search Gesù Cristo in rdv24 in romans 2' "$(cat <<'EOT'
EPISTOLE DI S. PAOLO AI~ROMANI 2:16 Tutto ciò si vedrà nel giorno in cui Dio giudicherà i segreti degli uomini per mezzo di Gesù Cristo, secondo il mio Evangelo.
EOT
)" 'search' 'Gesù Cristo' 'in' 'rdv24' 'in' 'romans' '2'
Add_Test 'RDV24' 'search Gesù Cristo in rdv24 in romans 3-5' "EPISTOLE DI S. PAOLO AI~ROMANI 3:22 vale a dire la giustizia di Dio mediante la fede in Gesù Cristo, per tutti i credenti; poiché non v'è distinzione;" 'search' 'Gesù Cristo' 'in' 'rdv24' 'in' 'romans' '3-5'
Add_Test 'RDV24' 'search Gesù Cristo in rdv24 in "johns letters"' "EPISTOLA I DI S. GIOVANNI 1:3 quello, dico, che abbiamo veduto e udito, noi l'annunziamo anche a voi, affinché voi pure abbiate comunione con noi, e la nostra comunione è col Padre e col suo Figliuolo, Gesù Cristo." 'search' 'Gesù Cristo' 'in' 'rdv24' 'in' 'johns letters'

# --- UBG ---
for t in 'Jezusa Chrystusa' 'Jezusa' 'Chrystusa'; do
  Add_Test 'UBG' "search $t in ubg" "$(cat <<'EOT'
Mateusza 1:1 Księga rodu Jezusa Chrystusa, syna Dawida, syna Abrahama.
EOT
)" 'search' "$t" 'in' 'ubg'
done
Add_Test 'UBG' 'search Jezusa Chrystusa in ubg in romans' "$(cat <<'EOT'
Rzymian 1:1 Paweł, sługa Jezusa Chrystusa, powołany apostoł, odłączony do głoszenia ewangelii Boga;
EOT
)" 'search' 'Jezusa Chrystusa' 'in' 'ubg' 'in' 'romans'
Add_Test 'UBG' 'search Jezusa Chrystusa in ubg in romans 2' "$(cat <<'EOT'
Rzymian 2:16 W dniu, w którym Bóg przez Jezusa Chrystusa będzie sądził skryte sprawy ludzkie według mojej ewangelii.
EOT
)" 'search' 'Jezusa Chrystusa' 'in' 'ubg' 'in' 'romans' '2'
Add_Test 'UBG' 'search Jezusa Chrystusa in ubg in romans 3-5' "$(cat <<'EOT'
Rzymian 3:22 Jest to sprawiedliwość Boga przez wiarę Jezusa Chrystusa dla wszystkich i na wszystkich wierzących. Nie ma bowiem różnicy.
EOT
)" 'search' 'Jezusa Chrystusa' 'in' 'ubg' 'in' 'romans' '3-5'
Add_Test 'UBG' 'search Jezusa Chrystusa in ubg in "johns letters"' "$(cat <<'EOT'
I Jana 1:3 To, co widzieliśmy i słyszeliśmy, to wam zwiastujemy, abyście i wy mieli z nami społeczność, a nasza społeczność to społeczność z Ojcem i z jego Synem, Jezusem Chrystusem.
EOT
)" 'search' 'Jezusa Chrystusa' 'in' 'ubg' 'in' 'johns letters'

# --- UBIO ---
for t in 'Ісуса Христа' 'Ісуса' 'Христа'; do
  Add_Test 'UBIO' "search $t in ubio" "$(cat <<'EOT'
Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:
EOT
)" 'search' "$t" 'in' 'ubio'
done
Add_Test 'UBIO' 'search Ісуса Христа in ubio in romans' "$(cat <<'EOT'
До римлян 1:1 Павло, раб Ісуса Христа, покликаний апостол, вибраний для звіщання Євангелії Божої,
EOT
)" 'search' 'Ісуса Христа' 'in' 'ubio' 'in' 'romans'
Add_Test 'UBIO' 'search Ісуса Христа in ubio in romans 2' "$(cat <<'EOT'
До римлян 2:16 дня, коли Бог, згідно з моїм благовістям, буде судити таємні речі людей через Ісуса Христа.
EOT
)" 'search' 'Ісуса Христа' 'in' 'ubio' 'in' 'romans' '2'
Add_Test 'UBIO' 'search Ісуса Христа in ubio in romans 3-5' "$(cat <<'EOT'
До римлян 3:22 А Божа правда через віру в Ісуса Христа в усіх і на всіх, хто вірує, бо різниці немає,
EOT
)" 'search' 'Ісуса Христа' 'in' 'ubio' 'in' 'romans' '3-5'
Add_Test 'UBIO' 'search Ісуса Христа in ubio in "johns letters"' "$(cat <<'EOT'
1-е Iвана 1:3 що ми бачили й чули про те ми звіщаємо вам, щоб і ви мали спільність із нами. Спільність же наша з Отцем і Сином Його Ісусом Христом.
EOT
)" 'search' 'Ісуса Христа' 'in' 'ubio' 'in' 'johns letters'

# --- SVEN ---
for t in 'Jesu Kristi' 'Jesu' 'Kristi'; do
  Add_Test 'SVEN' "search $t in sven" "$(cat <<'EOT'
Matteus 1:1 Detta är Jesu Kristi, Davids sons, Abrahams sons, släkttavla.
EOT
)" 'search' "$t" 'in' 'sven'
done
Add_Test 'SVEN' 'search Jesu Kristi in sven in romans' "$(cat <<'EOT'
Romarbrevet 1:1 Paulus, Jesu Kristi tjänare, kallad till apostel, avskild till att förkunna Guds evangelium,
EOT
)" 'search' 'Jesu Kristi' 'in' 'sven' 'in' 'romans'
Add_Test 'SVEN' 'search Jesu Kristi in sven in romans 3-5' "$(cat <<'EOT'
Romarbrevet 3:22 en rättfärdighet från Gud genom tro på Jesus Kristus, för alla dem som tro. Ty här är ingen åtskillnad.
EOT
)" 'search' 'Jesu Kristi' 'in' 'sven' 'in' 'romans' '3-5'
Add_Test 'SVEN' 'search Jesu Kristi in sven in "johns letters"' "$(cat <<'EOT'
1 Johannesbrevet 1:3 Ja, det vi hava sett och hört, det förkunna vi ock för eder, på det att också I mån hava gemenskap med oss; och vi hava vår gemenskap med Fadern och med hans Son, Jesus Kristus.
EOT
)" 'search' 'Jesu Kristi' 'in' 'sven' 'in' 'johns letters'

# --- CUNP ---
for t in '耶稣基督' '耶稣' '基督'; do
  Add_Test 'CUNP' "search $t in cunp" "$(cat <<'EOT'
马太福音 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：
EOT
)" 'search' "$t" 'in' 'cunp'
done
Add_Test 'CUNP' 'search 耶稣基督 in cunp in romans' "$(cat <<'EOT'
罗马书 1:1 耶稣基督的仆人 保罗 ，奉召为使徒，特派传　神的福音。
EOT
)" 'search' '耶稣基督' 'in' 'cunp' 'in' 'romans'
Add_Test 'CUNP' 'search 耶稣基督 in cunp in romans 2' "$(cat <<'EOT'
罗马书 2:16 就在　神藉耶稣基督审判人隐秘事的日子，照着我的福音所言。
EOT
)" 'search' '耶稣基督' 'in' 'cunp' 'in' 'romans' '2'
Add_Test 'CUNP' 'search 耶稣基督 in cunp in romans 3-5' "$(cat <<'EOT'
罗马书 3:22 就是　神的义，因信耶稣基督加给一切相信的人，并没有分别。
EOT
)" 'search' '耶稣基督' 'in' 'cunp' 'in' 'romans' '3-5'
Add_Test 'CUNP' 'search 耶稣基督 in cunp in "johns letters"' "$(cat <<'EOT'
约翰一书 1:3 我们将所看见、所听见的传给你们，使你们与我们相交。我们乃是与父并他儿子耶稣基督相交的。
EOT
)" 'search' '耶稣基督' 'in' 'cunp' 'in' 'johns letters'

# --- KRV ---
for t in '예수그리스도' '예수 그리스도' '예수' '그리스도'; do
  Add_Test 'KRV' "search $t in krv" "$(cat <<'EOT'
마태복음 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라
EOT
)" 'search' "$t" 'in' 'krv'
done
Add_Test 'KRV' 'search 예수그리스도 in krv in romans' "$(cat <<'EOT'
로마서 1:1 예수 그리스도의 종 바울은 사도로 부르심을 받아 하나님의 복음을 위하여 택정함을 입었으니
EOT
)" 'search' '예수그리스도' 'in' 'krv' 'in' 'romans'
Add_Test 'KRV' 'search 예수그리스도 in krv in romans 2' "$(cat <<'EOT'
로마서 2:16 곧 내 복음에 이른 바와 같이 하나님이 예수 그리스도로 말미암아 사람들의 은밀한 것을 심판하시는 그날이라
EOT
)" 'search' '예수그리스도' 'in' 'krv' 'in' 'romans' '2'
Add_Test 'KRV' 'search 예수그리스도 in krv in romans 3-5' "$(cat <<'EOT'
로마서 3:22 곧 예수 그리스도를 믿음으로 말미암아 모든 믿는 자에게 미치는 하나님의 의니 차별이 없느니라
EOT
)" 'search' '예수그리스도' 'in' 'krv' 'in' 'romans' '3-5'
Add_Test 'KRV' 'search 예수그리스도 in krv in "johns letters"' "$(cat <<'EOT'
요한1서 1:3 우리가 보고 들은 바를 너희에게도 전함은 너희로 우리와 사귐이 있게 하려 함이니 우리의 사귐은 아버지와 그 아들 예수 그리스도와 함께 함이라
EOT
)" 'search' '예수그리스도' 'in' 'krv' 'in' 'johns letters'

# --- JC ---
for t in 'イエス・キリスト' 'イエス' 'キリスト'; do
  Add_Test 'JC' "search $t in jc" "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search' "$t" 'in' 'jc'
done
Add_Test 'JC' 'search イエス・キリスト in jc in romans' "$(cat <<'EOT'
ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-
EOT
)" 'search' 'イエス・キリスト' 'in' 'jc' 'in' 'romans'
Add_Test 'JC' 'search イエス・キリスト in jc in romans 2' "$(cat <<'EOT'
ローマ人への手紙 2:16 そして、これらのことは、わたしの福音によれば、神がキリスト・イエスによって人々の隠れた事がらをさばかれるその日に、明らかにされるであろう。
EOT
)" 'search' 'イエス・キリスト' 'in' 'jc' 'in' 'romans' '2'
Add_Test 'JC' 'search イエス・キリスト in jc in romans 3-5' "$(cat <<'EOT'
ローマ人への手紙 3:22 それは、イエス・キリストを信じる信仰による神の義であって、すべて信じる人に与えられるものである。そこにはなんらの差別もない。
EOT
)" 'search' 'イエス・キリスト' 'in' 'jc' 'in' 'romans' '3-5'
Add_Test 'JC' 'search イエス・キリスト in jc in "johns letters"' "$(cat <<'EOT'
ヨハネの第一の手紙 1:3 すなわち、わたしたちが見たもの、聞いたものを、あなたがたにも告げ知らせる。それは、あなたがたも、わたしたちの交わりにあずかるようになるためである。わたしたちの交わりとは、父ならびに御子イエス・キリストとの交わりのことである。
EOT
)" 'search' 'イエス・キリスト' 'in' 'jc' 'in' 'johns letters'

Add_Test 'JC full-width spaces' 'bbl search イエス　キリスト　in jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search' 'イエス　キリスト　in' 'jc'
Add_Test 'JC full-width spaces' 'bbl search イエス　キリスト in　jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search' 'イエス　キリスト' 'in　jc'
Add_Test 'JC full-width spaces' 'bbl sarch　イエス・キリスト in jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search　イエス・キリスト' 'in' 'jc'
Add_Test 'JC full-width spaces' 'bbl search　イエス　キリスト in jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search　イエス　キリスト' 'in' 'jc'
Add_Test 'JC full-width spaces' 'bbl search　イエス　キリスト　in jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search　イエス　キリスト　in' 'jc'
Add_Test 'JC full-width spaces' 'bbl search　イエス　キリスト　in　jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search　イエス　キリスト　in　jc'
Add_Test 'JC full-width spaces' 'bbl search　イエス in jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search　イエス' 'in' 'jc'
Add_Test 'JC full-width spaces' 'bbl　search　イエス in jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search　イエス' 'in' 'jc'
Add_Test 'JC full-width spaces' 'bbl　search　イエス　in jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search　イエス　in' 'jc'
Add_Test 'JC full-width spaces' 'bbl　search　イエス　in　jc' "$(cat <<'EOT'
マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。
EOT
)" 'search　イエス　in　jc'
Add_Test 'JC full-width spaces' 'bbl search イエス in jc in romans' "$(cat <<'EOT'
ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-
EOT
)" 'search' 'イエス' 'in' 'jc' 'in' 'romans'
Add_Test 'JC full-width spaces' 'bbl search イエス in jc　in romans' "$(cat <<'EOT'
ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-
EOT
)" 'search' 'イエス' 'in' 'jc　in' 'romans'

# --- AYT ---
for t in 'Yesus Kristus' 'Yesus' 'Kristus'; do
  Add_Test 'AYT' "search $t in ayt" "$(cat <<'EOT'
Matius 1:1 Kitab silsilah Yesus Kristus, anak Daud, anak Abraham.
EOT
)" 'search' "$t" 'in' 'ayt'
done
Add_Test 'AYT' 'search Yesus Kristus in ayt in romans' "$(cat <<'EOT'
Roma 1:1 Paulus, hamba Yesus Kristus, yang dipanggil menjadi rasul dan dikhususkan bagi Injil Allah;
EOT
)" 'search' 'Yesus Kristus' 'in' 'ayt' 'in' 'romans'
Add_Test 'AYT' 'search Yesus Kristus in ayt in romans 2' "$(cat <<'EOT'
Roma 2:16 pada hari ketika Allah menghakimi pikiran-pikiran manusia yang tersembunyi melalui Yesus Kristus, menurut Injilku.
EOT
)" 'search' 'Yesus Kristus' 'in' 'ayt' 'in' 'romans' '2'
Add_Test 'AYT' 'search Yesus Kristus in ayt in romans 3-5' "$(cat <<'EOT'
Roma 3:24 dan dibenarkan dengan cuma-cuma oleh kasih karunia-Nya melalui penebusan yang ada dalam Yesus Kristus;
EOT
)" 'search' 'Yesus Kristus' 'in' 'ayt' 'in' 'romans' '3-5'
Add_Test 'AYT' 'search Yesus Kristus in ayt in "johns letters"' "$(cat <<'EOT'
1 Yohanes 3:16 Beginilah kita mengenal kasih, yaitu bahwa Yesus Kristus telah menyerahkan hidup-Nya untuk kita. Jadi, kita juga harus menyerahkan hidup kita untuk saudara-saudara kita.
EOT
)" 'search' 'Yesus Kristus' 'in' 'ayt' 'in' 'johns letters'

# --- TH1971 ---
for t in 'พระเยซูคริสต์' 'พระเยซู' 'คริสต์'; do
  Add_Test 'TH1971' "search $t in th1971" "$(cat <<'EOT'
มัทธิว 1:1 หนังสือลำดับพงศ์ของพระเยซูคริสต์ ผู้เป็นเชื้อสายของดาวิด ผู้สืบตระกูลเนื่องมาจากอับราฮัม
EOT
)" 'search' "$t" 'in' 'th1971'
done
Add_Test 'TH1971' 'search พระเยซูคริสต์ in th1971 in romans' "$(cat <<'EOT'
โรม 1:1 เปาโล ผู้รับใช้ของพระเยซูคริสต์ ผู้ซึ่งพระองค์ทรงเรียกให้เป็นอัครทูต และได้ทรงตั้งไว้ให้ประกาศข่าวประเสริฐของพระเจ้า
EOT
)" 'search' 'พระเยซูคริสต์' 'in' 'th1971' 'in' 'romans'
Add_Test 'TH1971' 'search พระเยซูคริสต์ in th1971 in romans 2' "$(cat <<'EOT'
โรม 2:16 ในวันที่พระเจ้าทรงพิพากษาความลับของมนุษย์โดยพระเยซูคริสต์ ทั้งนี้ตามข่าวประเสริฐที่ข้าพเจ้าได้ประกาศนั้น
EOT
)" 'search' 'พระเยซูคริสต์' 'in' 'th1971' 'in' 'romans' '2'
Add_Test 'TH1971' 'search พระเยซูคริสต์ in th1971 in romans 3-5' "$(cat <<'EOT'
โรม 3:22 คือความชอบธรรมของพระเจ้า ซึ่งทรงประทานโดยความเชื่อในพระเยซูคริสต์ แก่ทุกคนที่เชื่อ เพราะว่าคนทั้งหลายไม่ต่างกัน
EOT
)" 'search' 'พระเยซูคริสต์' 'in' 'th1971' 'in' 'romans' '3-5'
Add_Test 'TH1971' 'search พระเยซูคริสต์ in th1971 in "johns letters"' "$(cat <<'EOT'
1 ยอห์น 1:3 ซึ่งเราได้เห็นและได้ยินนั้น เราก็ได้ประกาศให้ท่านทั้งหลายรู้ด้วย เพื่อท่านทั้งหลายจะได้ร่วมสามัคคีธรรมกับเรา เราทั้งหลายก็ร่วมสามัคคีกับพระบิดา และกับพระเยซูคริสต์พระบุตรของพระองค์
EOT
)" 'search' 'พระเยซูคริสต์' 'in' 'th1971' 'in' 'johns letters'

# --- IRVHIN ---
for t in 'यीशु मसीह' 'यीशु' 'मसीह'; do
  Add_Test 'IRVHIN' "search $t in irvhin" "$(cat <<'EOT'
मत्ती 1:1 अब्राहम की सन्तान, दाऊद की सन्तान, यीशु मसीह की वंशावली ।
EOT
)" 'search' "$t" 'in' 'irvhin'
done
Add_Test 'IRVHIN' 'search यीशु मसीह in irvhin in romans' "$(cat <<'EOT'
रोमियों 1:1 पौलुस  की ओर से जो यीशु मसीह का दास है, और प्रेरित होने के लिये बुलाया गया, और परमेश्वर के उस सुसमाचार के लिये अलग किया गया है
EOT
)" 'search' 'यीशु मसीह' 'in' 'irvhin' 'in' 'romans'
Add_Test 'IRVHIN' 'search यीशु मसीह in irvhin in romans 2' "$(cat <<'EOT'
रोमियों 2:16 जिस दिन परमेश्वर मेरे सुसमाचार के अनुसार यीशु मसीह के द्वारा मनुष्यों की गुप्त बातों का न्याय करेगा।
EOT
)" 'search' 'यीशु मसीह' 'in' 'irvhin' 'in' 'romans' '2'
Add_Test 'IRVHIN' 'search यीशु मसीह in irvhin in romans 3-5' "$(cat <<'EOT'
रोमियों 3:22 अर्थात् परमेश्वर की वह धार्मिकता, जो यीशु मसीह पर विश्वास करने से सब विश्वास करनेवालों के लिये है। क्योंकि कुछ भेद नहीं;
EOT
)" 'search' 'यीशु मसीह' 'in' 'irvhin' 'in' 'romans' '3-5'
Add_Test 'IRVHIN' 'search यीशु मसीह in irvhin in "johns letters"' "$(cat <<'EOT'
1 यूहन्ना 1:3 जो कुछ हमने देखा और सुना है उसका समाचार तुम्हें भी देते हैं, इसलिए कि तुम भी हमारे साथ सहभागी हो; और हमारी यह सहभागिता पिता के साथ, और उसके पुत्र यीशु मसीह के साथ है।
EOT
)" 'search' 'यीशु मसीह' 'in' 'irvhin' 'in' 'johns letters'

# --- IRVBEN ---
for t in 'যীশু খ্রীষ্ট' 'যীশু' 'খ্রীষ্ট'; do
  Add_Test 'IRVBEN' "search $t in irvben" "$(cat <<'EOT'
মথি 1:1 যীশু খ্রীষ্টের বংশ তালিকা, তিনি দায়ূদের সন্তান, অব্রাহামের সন্তান।
EOT
)" 'search' "$t" 'in' 'irvben'
done
Add_Test 'IRVBEN' 'search যীশু খ্রীষ্ট in irvben in romans' "$(cat <<'EOT'
রোমীয় 1:1 পৌল, একজন যীশু খ্রীষ্টের দাস, প্রেরিত হবার জন্য ডাকা হয়েছে এবং ঈশ্বরের সুসমাচার প্রচারের জন্য আলাদা ভাবে মনোনীত করেছেন,
EOT
)" 'search' 'যীশু খ্রীষ্ট' 'in' 'irvben' 'in' 'romans'
Add_Test 'IRVBEN' 'search যীশু খ্রীষ্ট in irvben in romans 3-5' "$(cat <<'EOT'
রোমীয় 3:22 ঈশ্বরের সেই ধার্ম্মিকতা যীশু খ্রীষ্টে বিশ্বাসের মাধ্যমে যারা সবাই বিশ্বাস করে তাদের জন্য। কারণ সেখানে কোনো বিভেদ নেই।
EOT
)" 'search' 'যীশু খ্রীষ্ট' 'in' 'irvben' 'in' 'romans' '3-5'
Add_Test 'IRVBEN' 'search যীশু খ্রীষ্ট in irvben in "johns letters"' "$(cat <<'EOT'
1 যোহন 1:3 আমরা যাকে দেখেছি ও শুনেছি, তার খবর তোমাদেরকেও দিচ্ছি, যেন আমাদের সঙ্গে তোমাদেরও সহভাগীতা হয়। আর আমাদের সহভাগীতা হল পিতার এবং তাঁর পুত্র যীশু খ্রীষ্টের সহভাগীতা।
EOT
)" 'search' 'যীশু খ্রীষ্ট' 'in' 'irvben' 'in' 'johns letters'

# --- IRVTAM ---
for t in 'இயேசுகிறிஸ்து' 'இயேசு' 'கிறிஸ்து'; do
  Add_Test 'IRVTAM' "search $t in irvtam" "$(cat <<'EOT'
மத் 1:1 ஆபிரகாமின் மகனாகிய தாவீதின் குமாரனான இயேசுகிறிஸ்துவின் வம்சவரலாறு:
EOT
)" 'search' "$t" 'in' 'irvtam'
done
Add_Test 'IRVTAM' 'search இயேசுகிறிஸ்து in irvtam in romans' "$(cat <<'EOT'
ரோமர் 1:1 இயேசுகிறிஸ்துவின் ஊழியக்காரனும், அப்போஸ்தலனாக இருப்பதற்காக அழைக்கப்பட்டவனும், தேவனுடைய நற்செய்திக்காகப் பிரித்தெடுக்கப்பட்டவனுமாகிய பவுல்,
EOT
)" 'search' 'இயேசுகிறிஸ்து' 'in' 'irvtam' 'in' 'romans'
Add_Test 'IRVTAM' 'search இயேசுகிறிஸ்து in irvtam in romans 2' "$(cat <<'EOT'
ரோமர் 2:16 என்னுடைய நற்செய்தியின்படியே, தேவன் இயேசுகிறிஸ்துவைக்கொண்டு மனிதர்களுடைய இரகசியங்களைக்குறித்து நியாயத்தீர்ப்புக்கொடுக்கும் நாளிலே இது விளங்கும்.
EOT
)" 'search' 'இயேசுகிறிஸ்து' 'in' 'irvtam' 'in' 'romans' '2'
Add_Test 'IRVTAM' 'search இயேசுகிறிஸ்து in irvtam in romans 3-5' "$(cat <<'EOT'
ரோமர் 3:22 அது இயேசுகிறிஸ்துவை விசுவாசிக்கும் விசுவாசத்தினாலே வரும் தேவநீதியே; விசுவாசிக்கிற எல்லோருக்குள்ளும் எவர்கள் மேலும் அது வரும், வித்தியாசமே இல்லை.
EOT
)" 'search' 'இயேசுகிறிஸ்து' 'in' 'irvtam' 'in' 'romans' '3-5'
Add_Test 'IRVTAM' 'search இயேசுகிறிஸ்து in irvtam in "johns letters"' "$(cat <<'EOT'
1 யோவா 1:3 நீங்களும் எங்களோடு ஐக்கியம் உள்ளவர்களாகும்படி, நாங்கள் பார்த்தும் கேட்டும் இருக்கிறதை உங்களுக்கும் அறிவிக்கிறோம்; எங்களுடைய ஐக்கியம் பிதாவோடும் அவருடைய குமாரனாகிய இயேசுகிறிஸ்துவோடும் இருக்கிறது.
EOT
)" 'search' 'இயேசுகிறிஸ்து' 'in' 'irvtam' 'in' 'johns letters'

# --- NPIULB ---
for t in 'येशू ख्रीष्‍ट' 'येशू' 'ख्रीष्‍ट'; do
  Add_Test 'NPIULB' "search $t in npiulb" "$(cat <<'EOT'
मत्ती 1:1 दाऊदका पुत्र अब्राहामका पुत्र येशू ख्रीष्‍टको वंशावलीको पुस्तक ।
EOT
)" 'search' "$t" 'in' 'npiulb'
done
Add_Test 'NPIULB' 'search येशू ख्रीष्‍ट in npiulb in romans' "$(cat <<'EOT'
रोमी 1:1 प्रेरित हुनको निम्ति बोलाइएका र सुसमाचारको कामको निम्ति अलग गरिएका, येशू ख्रीष्‍टका दास पावल ।
EOT
)" 'search' 'येशू ख्रीष्‍ट' 'in' 'npiulb' 'in' 'romans'
Add_Test 'NPIULB' 'search येशू ख्रीष्‍ट in npiulb in romans 2' "$(cat <<'EOT'
रोमी 2:16 अनि परमेश्‍वरप्रति पनि यही कुरो लागु हुन्छ । त्यो मेरो सुसमाचारअनुसार येशू ख्रीष्‍टद्वारा परमेश्‍वरले सबै मानिसहरूको गोप्य कुराहरूको इन्साफ गर्नुहुने दिन हुनेछ ।
EOT
)" 'search' 'येशू ख्रीष्‍ट' 'in' 'npiulb' 'in' 'romans' '2'
Add_Test 'NPIULB' 'search येशू ख्रीष्‍ट in npiulb in romans 3-5' "$(cat <<'EOT'
रोमी 3:22 अर्थात् यो विश्‍वास गर्ने सबैका निम्ति येशू ख्रीष्‍टमा विश्‍वासद्वारा आउने परमेश्‍वरको धार्मिकता हो । किनकि त्यहाँ कुनै भेदभाव छैन ।
EOT
)" 'search' 'येशू ख्रीष्‍ट' 'in' 'npiulb' 'in' 'romans' '3-5'
Add_Test 'NPIULB' 'search येशू ख्रीष्‍ट in npiulb in "johns letters"' "$(cat <<'EOT'
१ यूहन्ना 1:3 जुन हामीले देखेका र सुनेका छौँ, हामी तिमीहरूलाई पनि घोषणा गर्छौं, ताकि हामीहरूसँग तिमीहरूको सङ्गति होस् । हाम्रो सङ्गति पिता र उहाँको पुत्र येशू ख्रीष्‍टसँग हुन्छ ।
EOT
)" 'search' 'येशू ख्रीष्‍ट' 'in' 'npiulb' 'in' 'johns letters'

# --- ABTAG ---
for t in 'Jesucristo' 'Jesus' 'Cristo'; do
  Add_Test 'ABTAG' "search $t in abtag" \
    'MATEO 1:1 Ang aklat ng lahi ni Jesucristo, na anak ni David, na anak ni Abraham.' \
    'search' "$t" 'in' 'abtag'
done
Add_Test 'ABTAG' 'search Jesucristo in abtag in romans' \
  'MGA TAGA ROMA 1:1 Si Pablo na alipin ni Jesucristo, na tinawag na maging apostol, ibinukod sa evangelio ng Dios,' \
  'search' 'Jesucristo' 'in' 'abtag' 'in' 'romans'
Add_Test 'ABTAG' 'search Jesucristo in abtag in romans 2' \
  'MGA TAGA ROMA 2:16 Sa araw na hahatulan ng Dios ang mga lihim ng mga tao, ayon sa aking evangelio, sa pamamagitan ni Jesucristo.' \
  'search' 'Jesucristo' 'in' 'abtag' 'in' 'romans' '2'
Add_Test 'ABTAG' 'search Jesucristo in abtag in romans 3-5' \
  "MGA TAGA ROMA 3:22 Sa makatuwid baga'y ang katuwiran ng Dios sa pamamagitan ng pananampalataya kay Jesucristo sa lahat ng mga nagsisisampalataya; sapagka't walang pagkakaiba;" \
  'search' 'Jesucristo' 'in' 'abtag' 'in' 'romans' '3-5'
Add_Test 'ABTAG' 'search Jesucristo in abtag in "johns letters"' \
  'I JUAN 1:3 Yaong aming nakita at narinig ay siya rin naming ibinabalita sa inyo, upang kayo naman ay magkaroon ng pakikisama sa amin: oo, at tayo ay may pakikisama sa Ama, at sa kaniyang Anak na si Jesucristo:' \
  'search' 'Jesucristo' 'in' 'abtag' 'in' 'johns letters'

# --- KTTV ---
Add_Test 'KTTV' 'search Đức Chúa Jêsus Christ in kttv' "$(cat <<'EOT'
Ma-thi-ơ 1:18 Vả, sự giáng-sanh của Đức Chúa Jêsus-Christ đã xảy ra như vầy: Khi Ma-ri, mẹ Ngài, đã hứa gả cho Giô-sép, song chưa ăn-ở cùng nhau, thì người đã chịu thai bởi Đức Thánh-Linh.
EOT
)" 'search' 'Đức Chúa Jêsus Christ' 'in' 'kttv'
Add_Test 'KTTV' 'search Đức Chúa Jêsus in kttv' "$(cat <<'EOT'
Ma-thi-ơ 1:16 Gia-cốp sanh Giô-sép là chồng Ma-ri; Ma-ri là người sanh Đức Chúa Jêsus, gọi là Christ.
EOT
)" 'search' 'Đức Chúa Jêsus' 'in' 'kttv'
Add_Test 'KTTV' 'search Christ in kttv' "$(cat <<'EOT'
Ma-thi-ơ 1:1 Gia-phổ Đức Chúa Jêsus-Christ, con cháu Đa-vít và con cháu Áp-ra-ham.
EOT
)" 'search' 'Christ' 'in' 'kttv'
Add_Test 'KTTV' 'search Đức Chúa Jêsus Christ in kttv in romans' "$(cat <<'EOT'
Rô-ma 1:1 Phao-lô, tôi-tớ của Đức Chúa Jêsus-Christ, được gọi làm sứ-đồ, để riêng ra đặng giảng Tin-lành Đức Chúa Trời, —
EOT
)" 'search' 'Đức Chúa Jêsus Christ' 'in' 'kttv' 'in' 'romans'
Add_Test 'KTTV' 'search Đức Chúa Jêsus Christ in kttv in romans 2' "$(cat <<'EOT'
Rô-ma 2:16 Ấy là điều sẽ hiện ra trong ngày Đức Chúa Trời bởi Đức Chúa Jêsus-Christ mà xét-đoán những việc kín-nhiệm của loài người, y theo Tin-lành tôi.
EOT
)" 'search' 'Đức Chúa Jêsus Christ' 'in' 'kttv' 'in' 'romans' '2'
Add_Test 'KTTV' 'search Đức Chúa Jêsus Christ in kttv in romans 3-5' "$(cat <<'EOT'
Rô-ma 3:22 tức là sự công-bình của Đức Chúa Trời, bởi sự tin đến Đức Chúa Jêsus-Christ, cho mọi người nào tin. Chẳng có phân-biệt chi hết,
EOT
)" 'search' 'Đức Chúa Jêsus Christ' 'in' 'kttv' 'in' 'romans' '3-5'
Add_Test 'KTTV' 'search Đức Chúa Jêsus Christ in kttv in "johns letters"' "$(cat <<'EOT'
I Giăng 3:23 Vả, nầy là điều-răn của Ngài: là chúng ta phải tin đến danh Con Ngài, tức là Đức Chúa Jêsus-Christ, và chúng ta phải yêu-mến lẫn nhau như Ngài đã truyền-dạy ta.
EOT
)" 'search' 'Đức Chúa Jêsus Christ' 'in' 'kttv' 'in' 'johns letters'

# --- IRVGUJ ---
for t in 'ઈસુ ખ્રિસ્ત' 'ઈસુ' 'ખ્રિસ્ત'; do
  Add_Test 'IRVGUJ' "search $t in irvguj" "$(cat <<'EOT'
માથ. 1:1 ઈસુ ખ્રિસ્ત જે ઇબ્રાહિમનાં દીકરા, જે દાઉદના દીકરા, તેમની વંશાવળી.
EOT
)" 'search' "$t" 'in' 'irvguj'
done
Add_Test 'IRVGUJ' 'search ઈસુ ખ્રિસ્ત in irvguj in romans' "$(cat <<'EOT'
રોમ. 1:1 પ્રેરિત થવા સારુ તેડાયેલો અને ઈશ્વરની સુવાર્તા માટે અલગ કરાયેલો ઈસુ ખ્રિસ્તનો સેવક પાઉલ, રોમમાં રહેતા, ઈશ્વરના વહાલા અને પવિત્ર થવા સારુ પસંદ કરાયેલા સર્વ લોકોને લખે છે
EOT
)" 'search' 'ઈસુ ખ્રિસ્ત' 'in' 'irvguj' 'in' 'romans'
Add_Test 'IRVGUJ' 'search ઈસુ ખ્રિસ્ત in irvguj in romans 2' "$(cat <<'EOT'
રોમ. 2:16 ઈશ્વર મારી સુવાર્તા પ્રમાણે ઈસુ ખ્રિસ્તની મારફતે મનુષ્યોના ગુપ્ત કામોનો ન્યાય કરશે, તે દિવસે એમ થશે.
EOT
)" 'search' 'ઈસુ ખ્રિસ્ત' 'in' 'irvguj' 'in' 'romans' '2'
Add_Test 'IRVGUJ' 'search ઈસુ ખ્રિસ્ત in irvguj in romans 3-5' "$(cat <<'EOT'
રોમ. 3:22 એટલે ઈશ્વરનું ન્યાયીપણું, જે ઈસુ ખ્રિસ્ત પરના વિશ્વાસદ્વારા સર્વ વિશ્વાસ કરનારાઓને માટે છે તે; કેમ કે એમાં કંઈ પણ તફાવત નથી.
EOT
)" 'search' 'ઈસુ ખ્રિસ્ત' 'in' 'irvguj' 'in' 'romans' '3-5'
Add_Test 'IRVGUJ' 'search ઈસુ ખ્રિસ્ત in irvguj in "johns letters"' "$(cat <<'EOT'
1 યોહ. 1:3 હા, અમારી સાથે તમારી પણ સંગત થાય, એ માટે જે અમે જોયું તથા સાંભળ્યું છે, તે તમને પણ જાહેર કરીએ છીએ; અને ખરેખર અમારી સંગત પિતાની સાથે તથા તેમના પુત્ર ઈસુ ખ્રિસ્તની સાથે છે.
EOT
)" 'search' 'ઈસુ ખ્રિસ્ત' 'in' 'irvguj' 'in' 'johns letters'

# --- IRVMAR ---
for t in 'येशू ख्रिस्त' 'येशू' 'ख्रिस्त'; do
  Add_Test 'IRVMAR' "search $t in irvmar" "$(cat <<'EOT'
मत्त. 1:1 अब्राहामाचा पुत्र दावीद याचा पुत्र जो येशू ख्रिस्त याची वंशावळ.
EOT
)" 'search' "$t" 'in' 'irvmar'
done
Add_Test 'IRVMAR' 'search येशू ख्रिस्त in irvmar in romans' "$(cat <<'EOT'
रोम. 1:1 प्रेषित होण्यास बोलावलेला, येशू ख्रिस्ताचा दास, देवाच्या सुवार्तेसाठी वेगळा केलेला, पौल ह्याजकडून;
EOT
)" 'search' 'येशू ख्रिस्त' 'in' 'irvmar' 'in' 'romans'
Add_Test 'IRVMAR' 'search येशू ख्रिस्त in irvmar in romans 2' "$(cat <<'EOT'
रोम. 2:16 देव, माझ्या सुवार्तेप्रमाणे जेव्हा मनुष्यांच्या गुप्त गोष्टींचा ख्रिस्त येशूकडून न्याय करील त्यादिवशी हे दिसून येईल.
EOT
)" 'search' 'येशू ख्रिस्त' 'in' 'irvmar' 'in' 'romans' '2'
Add_Test 'IRVMAR' 'search येशू ख्रिस्त in irvmar in romans 3-5' "$(cat <<'EOT'
रोम. 3:22 पण हे देवाचे नीतिमत्त्व येशू ख्रिस्तावरील विश्वासाद्वारे, विश्वास ठेवणार्‍या सर्वांसाठी आहे कारण तेथे कसलाही फरक नाही.
EOT
)" 'search' 'येशू ख्रिस्त' 'in' 'irvmar' 'in' 'romans' '3-5'
Add_Test 'IRVMAR' 'search येशू ख्रिस्त in irvmar in "johns letters"' "$(cat <<'EOT'
1 योहा. 1:3 आम्ही जे पाहिले व ऐकले आहे ते आम्ही आता तुम्हांलाही घोषित करीत आहोत, यासाठी की तुमचीही आमच्यासोबत सहभागिता असावी. आमची सहभागिता तर देवपिता व त्याचा पुत्र येशू ख्रिस्त याजबरोबर आहे.
EOT
)" 'search' 'येशू ख्रिस्त' 'in' 'irvmar' 'in' 'johns letters'

# --- IRVTEL ---
for t in 'యేసు క్రీస్తు' 'యేసు' 'క్రీస్తు'; do
  Add_Test 'IRVTEL' "search $t in irvtel" "$(cat <<'EOT'
మత్తయి 1:1 అబ్రాహాము వంశం వాడైన దావీదు వంశం వాడు యేసు క్రీస్తు వంశావళి.
EOT
)" 'search' "$t" 'in' 'irvtel'
done
Add_Test 'IRVTEL' 'search యేసు క్రీస్తు in irvtel in romans' "$(cat <<'EOT'
రోమా పత్రిక 1:1 యేసు క్రీస్తు దాసుడు, అపోస్తలుడుగా పిలుపు పొందినవాడు, దేవుని సువార్త కోసం ప్రభువు ప్రత్యేకించుకున్న
EOT
)" 'search' 'యేసు క్రీస్తు' 'in' 'irvtel' 'in' 'romans'
Add_Test 'IRVTEL' 'search యేసు క్రీస్తు in irvtel in romans 2' "$(cat <<'EOT'
రోమా పత్రిక 2:16 నా సువార్త ప్రకారం దేవుడు యేసు క్రీస్తు ద్వారా మానవుల రహస్యాలను విచారించే రోజున ఈ విధంగా జరుగుతుంది.
EOT
)" 'search' 'యేసు క్రీస్తు' 'in' 'irvtel' 'in' 'romans' '2'
Add_Test 'IRVTEL' 'search యేసు క్రీస్తు in irvtel in romans 3-5' "$(cat <<'EOT'
రోమా పత్రిక 3:22 అది యేసు క్రీస్తులో విశ్వాసమూలంగా నమ్మే వారందరికీ కలిగే దేవుని నీతి.
EOT
)" 'search' 'యేసు క్రీస్తు' 'in' 'irvtel' 'in' 'romans' '3-5'
Add_Test 'IRVTEL' 'search యేసు క్రీస్తు in irvtel in "johns letters"' "$(cat <<'EOT'
1 యోహాను పత్రిక 1:3 మీరు కూడా మాతో సహవాసం కలిగి ఉండాలని మేము చూసిందీ, విన్నదీ మీకు ప్రకటిస్తున్నాం. నిజానికి మన సహవాసం తండ్రితోను, ఆయన కుమారుడు యేసు క్రీస్తుతోను ఉంది.
EOT
)" 'search' 'యేసు క్రీస్తు' 'in' 'irvtel' 'in' 'johns letters'

# --- IRVURD ---
for t in 'ईसा मसीह' 'ईसा'; do
  Add_Test 'IRVURD' "search $t in irvurd" "$(cat <<'EOT'
मत्त 1:1 ईसा मसीह इबने दाऊद इबने इब्राहीम का नसबनामा।
EOT
)" 'search' "$t" 'in' 'irvurd'
done
Add_Test 'IRVURD' 'search मसीह in irvurd' "$(cat <<'EOT'
ज़बूर 2:2 ख़ुदावन्द और उसके मसीह के ख़िलाफ़ ज़मीन के बादशाह एक हो कर, और हाकिम आपस में मशवरा करके कहते हैं,
EOT
)" 'search' 'मसीह' 'in' 'irvurd'
Add_Test 'IRVURD' 'search ईसा मसीह in irvurd in romans' "$(cat <<'EOT'
रोमि 1:1 पौलुस की तरफ़ से जो ईसा मसीह का बन्दा है और रसूल होने के लिए बुलाया गया और ख़ुदा की उस ख़ुशख़बरी के लिए अलग किया गया।
EOT
)" 'search' 'ईसा मसीह' 'in' 'irvurd' 'in' 'romans'
Add_Test 'IRVURD' 'search ईसा मसीह in irvurd in romans 2' "$(cat <<'EOT'
रोमि 2:16 जिस रोज़ ख़ुदा ख़ुशख़बरी के मुताबिक़ जो मै ऐलान करता हूँ ईसा मसीह की मारिफ़त आदमियों की छुपी बातों का इन्साफ़ करेगा।
EOT
)" 'search' 'ईसा मसीह' 'in' 'irvurd' 'in' 'romans' '2'
Add_Test 'IRVURD' 'search ईसा मसीह in irvurd in romans 3-5' "$(cat <<'EOT'
रोमि 3:22 यानी ख़ुदा की वो रास्तबाज़ी जो ईसा मसीह पर ईमान लाने से सब ईमान लानेवालों को हासिल होती है; क्यूँकि कुछ फ़र्क़ नहीं।
EOT
)" 'search' 'ईसा मसीह' 'in' 'irvurd' 'in' 'romans' '3-5'
Add_Test 'IRVURD' 'search ईसा मसीह in irvurd in "johns letters"' "$(cat <<'EOT'
1 यूह 1:3 जो कुछ हम ने देखा और सुना है तुम्हें भी उसकी ख़बर देते है, ताकि तुम भी हमारे शरीक हो, और हमारा मेल मिलाप बाप के साथ और उसके बेटे ईसा मसीह के साथ है।
EOT
)" 'search' 'ईसा मसीह' 'in' 'irvurd' 'in' 'johns letters'

now_ms() {
  perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000' 2>/dev/null || date +%s000
}

declare -a pids=()
RESULT_DIR="$(mktemp -d 2>/dev/null || mktemp -d -t bbl-search-tests)"
BBL_PID_DIR="$RESULT_DIR/bbl-pids"
mkdir -p "$BBL_PID_DIR"

terminate_process_group() {
  local pid="$1"
  [[ -n "$pid" ]] || return 0
  kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
  sleep 1
  kill -KILL -- "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null || true
}

cleanup() {
  local pid pid_file
  for pid_file in "$BBL_PID_DIR"/*.pid; do
    [[ -e "$pid_file" ]] || continue
    pid="$(cat "$pid_file" 2>/dev/null || true)"
    terminate_process_group "$pid"
  done
  if [[ ${#pids[@]} -gt 0 ]]; then
    for pid in "${pids[@]}"; do
      kill "$pid" 2>/dev/null || true
    done
  fi
  wait 2>/dev/null || true
  rm -rf "$RESULT_DIR"
}

trap cleanup EXIT INT TERM

run_one_test() {
  local index="$1"
  local meta_file="$RESULT_DIR/$index.meta"
  local error_file="$RESULT_DIR/$index.error"
  local output_file="$RESULT_DIR/$index.output"
  local start_ms end_ms elapsed_ms exit_code first_line expected expected_output actual_output normalized_expected_output timed_out cmd_pid now_s deadline_s
  local attempt max_attempts
  local -a cli_args

  IFS="$US" read -r -a cli_args <<< "${CLI_ARGS_JOINED[$index]}"
  expected="${EXPECTED_LINES[$index]}"
  expected_output="${EXPECTED_OUTPUTS[$index]}"

  printf '[RUN] %03d/%03d %s\n' "$((index + 1))" "${#NAMES[@]}" "${NAMES[$index]}" >&2

  start_ms="$(now_ms)"
  max_attempts=$((RetryTimeoutFailures + 1))
  attempt=1

  while true; do
    if [[ "$attempt" -gt 1 ]]; then
      printf '[RETRY] %03d/%03d %s after timeout, attempt %d/%d\n' \
        "$((index + 1))" "${#NAMES[@]}" "${NAMES[$index]}" "$attempt" "$max_attempts" >&2
    fi

    perl -e 'setpgrp(0, 0); exec @ARGV; die "exec failed: $!\n"' "$BblPath" "${cli_args[@]}" --verses 1 > "$output_file" 2>&1 &
    cmd_pid="$!"
    printf '%s\n' "$cmd_pid" > "$BBL_PID_DIR/$index.pid"
    timed_out=0
    deadline_s=$(($(date +%s) + SearchTimeoutSeconds))

    while kill -0 "$cmd_pid" 2>/dev/null; do
      now_s="$(date +%s)"
      if [[ "$now_s" -ge "$deadline_s" ]]; then
        timed_out=1
        terminate_process_group "$cmd_pid"
        break
      fi
      sleep 0.1
    done

    wait "$cmd_pid" 2>/dev/null
    exit_code=$?
    rm -f "$BBL_PID_DIR/$index.pid"
    if [[ "$timed_out" -eq 1 ]]; then
      exit_code=124
    fi

    if [[ "$exit_code" -ne 124 || "$attempt" -ge "$max_attempts" ]]; then
      break
    fi

    attempt=$((attempt + 1))
  done
  end_ms="$(now_ms)"
  elapsed_ms=$((end_ms - start_ms))

  first_line="$(awk '{ sub(/\r$/, ""); if (NF) { print; exit } }' "$output_file")"

  if [[ $exit_code -ne 0 ]]; then
    {
      if [[ "$timed_out" -eq 1 ]]; then
        printf 'bbl timed out after %s seconds on %s attempt(s)\n' "$SearchTimeoutSeconds" "$attempt"
      else
        printf 'bbl exited with code %s\n' "$exit_code"
      fi
      cat "$output_file"
    } > "$error_file"
    printf 'Passed=0\nExitCode=%s\nElapsedMs=%s\n' "$exit_code" "$elapsed_ms" > "$meta_file"
    return 0
  fi

  if [[ -n "$expected_output" ]]; then
    actual_output="$(awk '{ sub(/\r$/, ""); if (NF) print }' "$output_file")"
    normalized_expected_output="$(printf '%s\n' "$expected_output" | awk '{ sub(/\r$/, ""); if (NF) print }')"
    if [[ "$actual_output" != "$normalized_expected_output" ]]; then
      {
        printf 'expected output:\n%s\n' "$normalized_expected_output"
        printf 'actual output:\n%s\n' "$actual_output"
      } > "$error_file"
      printf 'Passed=0\nExitCode=%s\nElapsedMs=%s\n' "$exit_code" "$elapsed_ms" > "$meta_file"
      return 0
    fi
  elif [[ "$first_line" != "$expected" ]]; then
    {
      printf 'expected first line:\n%s\n' "$expected"
      printf 'actual first line:\n%s\n' "$first_line"
    } > "$error_file"
    printf 'Passed=0\nExitCode=%s\nElapsedMs=%s\n' "$exit_code" "$elapsed_ms" > "$meta_file"
    return 0
  fi

  : > "$error_file"
  printf 'Passed=1\nExitCode=%s\nElapsedMs=%s\n' "$exit_code" "$elapsed_ms" > "$meta_file"
}

echo ""
echo "Running bbl install/search E2E tests"
echo "bbl: $BblPath"
echo "tests: ${#NAMES[@]}"
echo "throttle: $ThrottleLimit"
echo "search timeout: ${SearchTimeoutSeconds}s"
echo "timeout retries: $RetryTimeoutFailures"

total_start_ms="$(now_ms)"

if [[ "$ThrottleLimit" -eq 1 ]]; then
  for ((i = 0; i < ${#NAMES[@]}; i++)); do
    run_one_test "$i"
  done
else
  for ((i = 0; i < ${#NAMES[@]}; i++)); do
    run_one_test "$i" &
    pids+=("$!")

    if [[ ${#pids[@]} -ge $ThrottleLimit ]]; then
      wait "${pids[0]}" || true
      if [[ ${#pids[@]} -gt 1 ]]; then
        pids=("${pids[@]:1}")
      else
        pids=()
      fi
    fi
  done

  if [[ ${#pids[@]} -gt 0 ]]; then
    for pid in "${pids[@]}"; do
      wait "$pid" || true
    done
  fi
fi

total_end_ms="$(now_ms)"
total_seconds=$(((total_end_ms - total_start_ms) / 1000))

current_group=""
passed_count=0
failed_count=0

get_meta_value() {
  local file="$1"
  local key="$2"
  awk -F= -v k="$key" '$1 == k { print substr($0, index($0, "=") + 1); exit }' "$file"
}

for ((i = 0; i < ${#NAMES[@]}; i++)); do
  meta_file="$RESULT_DIR/$i.meta"
  passed="$(get_meta_value "$meta_file" 'Passed')"
  elapsed_ms="$(get_meta_value "$meta_file" 'ElapsedMs')"

  if [[ "${TEST_GROUPS[$i]}" != "$current_group" ]]; then
    current_group="${TEST_GROUPS[$i]}"
    echo ""
    echo "$current_group"
  fi

  seconds="$(awk -v ms="$elapsed_ms" 'BEGIN { printf "%.2f", ms / 1000.0 }')"

  if [[ "$passed" == "1" ]]; then
    passed_count=$((passed_count + 1))
    echo "  [PASS] ${NAMES[$i]} (${seconds}s)"
  else
    failed_count=$((failed_count + 1))
    echo "  [FAIL] ${NAMES[$i]} (${seconds}s)"
  fi
done

if [[ $total_seconds -lt 60 ]]; then
  time_str="${total_seconds}s"
else
  min=$((total_seconds / 60))
  sec=$((total_seconds % 60))
  time_str="${min}m ${sec}s"
fi

echo ""
echo "Test Summary: $passed_count successful, $failed_count failures"
echo "Elapsed: $time_str"

if [[ $failed_count -gt 0 ]]; then
  echo ""
  echo "Failures:"

  for ((i = 0; i < ${#NAMES[@]}; i++)); do
    meta_file="$RESULT_DIR/$i.meta"
    passed="$(get_meta_value "$meta_file" 'Passed')"

    if [[ "$passed" != "1" ]]; then
      exit_code="$(get_meta_value "$meta_file" 'ExitCode')"
      IFS="$US" read -r -a cli_args <<< "${CLI_ARGS_JOINED[$i]}"

      echo ""
      echo "  [FAIL] ${NAMES[$i]}"
      echo "         Group: ${TEST_GROUPS[$i]}"
      echo "         Args: ${cli_args[*]}"
      echo "         ExitCode: $exit_code"
      sed 's/^/         /' "$RESULT_DIR/$i.error"
    fi
  done

  exit 1
fi

exit 0

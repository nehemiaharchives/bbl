# bbl
A command line tool to read Holy Bible

![screenshot](bbl700px-screenshot.png)

## Usage (TL;DR)

```
bbl
bbl gen 1
bbl john 3:16
bbl john 3:16 in kjv
bbl search Jesus Christ
bbl search Jesus Christ in kjv
bbl search Jesus Christ in romans
bbl search Jesus Christ in romans 5-12
bbl search Jesus Christ in romans 5-12 in kjv
bbl rand
bbl rand g
bbl rand nt
bbl rand ot
bbl list books
bbl list bibles
```

Syntax
```
bbl [BOOK] [CHAPTERVERSE] in [TRANSLATION]
bbl search [KEY WORDS] in [BOOK] [CHAPTERVERSE] in [TRANSLATION]
bbl rand [GOSPEL, OT, NT]
bbl list [BOOKS, BIBLES]
```
some example for BOOK: ```gen, ex, lev, num, josh, jg, ru, 1sm, 2sm, 1k 2k, 1ch, 2ch, ez, ne, job, ps, pr, ec, so, is, je, la, ezk, da, ho, jl, am, ob, jnh, mic, na, hb, zp, hg, zc, mal, matt, mk, lk, jn, act, rom, 1co, 2co, gal, eph, phil, col, 1th, 2th, 1tim, 2tim, tit, phm, heb, jm, 1pt, 2pt, 1jn, 2jn, 3jn, jd, rev```

For full list of available BOOK, run ```bbl list books```

available TRANSLATION: ```webus, kjv, rvr09, tb, delut, lsg, sinod, svrj, rdv24, ubg, ubio, sven, cunp, krv, jc```

For full descriptions of those Bible translations, run ```bbl list```

## Usage

bbl, with no argument/option defaults to output Genesis chapter 1 in World English Bible.

```
joel@JOEL-LAPTOP:~$ bbl
1 In the beginning, God created the heavens and the earth.
2 The earth was formless and empty. Darkness was on the surface of the deep and God’s Spirit was hovering over the su...
3 God said, “Let there be light,” and there was light.
...
31 God saw everything that he had made, and, behold, it was very good. There was evening and there was morning, a six...
```

bbl expects to specify a book and a chapter for reading Bible:
```
joel@JOEL-LAPTOP:~$ bbl ex 1
1 Now these are the names of the sons of Israel, who came into Egypt (every man and his household came with Jacob):
2 Reuben, Simeon, Levi, and Judah,
3 Issachar, Zebulun, and Benjamin,
...
22 Pharaoh commanded all his people, saying, “You shall cast every son who is born into the river, and every daughter...
```
bbl allows to specify a verse:
```
joel@JOEL-LAPTOP:~$ bbl john 3:16
16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but h...
```

or a range of verses
```
joel@JOEL-LAPTOP:~$ bbl matt 28:18-20
18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.
19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spi...
20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age...
```

bbl also let you specify other translation of Bibles such as King James Version by supplying "in {translation}" subcommand.
```
joel@JOEL-LAPTOP:~$ bbl genesis 1 in kjv
1 In the beginning God created the heaven and the earth.
2 And the earth was without form, and void; and darkness [was] upon the face of the deep. And the Spirit of God moved...
3 And God said, Let there be light: and there was light.
...
31 And God saw every thing that he had made, and, behold, [it was] very good. And the evening and the morning were th...
```

## Bible book names
bbl accepts following abbreviation as command argument to specify book as ```bbl list books``` shows:
```
    genesis, gen, ge, gn
    exodus, ex, exod, exo
    leviticus, lev, le, lv
    numbers, num, nu, nm, nb
    deuteronomy, deut, de, dt
    joshua, josh, jos, jsh
    judges, judg, jdg, jg, jdgs
    ruth, rth, ru
    1st samuel, 1 sam, 1sam, 1sm, 1sa, 1s, 1 samuel, 1samuel, 1st sam, first samuel, first sam
    2nd samuel, 2 sam, 2sam, 2sm, 2sa, 2s, 2 samuel, 2ndsam, 2nd sam, second samuel, second sam
    1st kings, 1kings, 1 kings, 1kgs, 1 kgs, 1ki, 1k, 1stkgs, first kings, first kgs
    2nd kings, 2kings, 2 kings, 2kgs, 2 kgs, 2ki, 2k, 2ndkgs, second kings, second kgs
    1st chronicles, 1chronicles, 1 chronicles, 1chr, 1 chr, 1ch, 1stchr, 1st chr, first chronicles, first chr
    2nd chronicles, 2chronicles, 2 chronicles, 2chr, 2 chr, 2ch, 2ndchr, 2nd chr, second chronicles, second chr
    ezra, ezr, ez
    nehemiah, neh, ne
    esther, est, esth, es
    job, jb
    psalms, ps, psalm, pslm, psa, psm, pss
    proverbs, prov, pro, prv, pr
    ecclesiastes, eccles, eccle, ecc, ec, qoh
    song of solomon, song, song of songs, sos, so, canticle of canticles, canticles, cant
    isaiah, isa, is
    jeremiah, jer, je, jr
    lamentations, lam, la
    ezekiel, ezek, eze, ezk
    daniel, dan, da, dn
    hosea, hos, ho
    joel, jl
    amos, am
    obadiah, obad, ob
    jonah, jnh, jon
    micah, mic, mc
    nahum, nah, na
    habakkuk, hab, hb
    zephaniah, zeph, zep, zp
    haggai, hag, hg
    zechariah, zech, zec, zc
    malachi, mal, ml
    matthew, matt, mt
    mark, mrk, mar, mk, mr
    luke, luk, lk
    john, joh, jhn, jn
    acts, act, ac
    romans, rom, ro, rm
    1 corinthians, 1corinthians, 1 cor, 1cor, 1 co, 1co, 1st corinthians, first corinthians
    2 corinthians, 2corinthians, 2 cor, 2cor, 2 co, 2co, 2nd corinthians, second corinthians
    galatians, gal, ga
    ephesians, eph, ephes
    philippians, phil, php, pp
    colossians, col, co
    1 thessalonians, 1thessalonians, 1 thess, 1thess, 1 thes, 1thes, 1 th, 1th, 1st thessalonians, 1st thess, first thessalonians, first thess
    2 thessalonians, 2thessalonians, 2 thess, 2thess, 2 thes, 2thes, 2 th, 2th, 2nd thessalonians, 2nd thess, second thessalonians, second thess
    1 timothy, 1timothy, 1 tim, 1tim, 1 ti, 1ti, 1st timothy, 1st tim, first timothy, first tim
    2 timothy, 2timothy, 2 tim, 2tim, 2 ti, 2ti, 2nd timothy, 2nd tim, second timothy, second tim
    titus, tit, ti
    philemon, philem, phm, pm
    hebrews, heb
    james, jas, jm
    1 peter, 1peter, 1 pet, 1pet, 1 pe, 1pe, 1 pt, 1pt, 1p, 1st peter, first peter
    2 peter, 2peter, 2 pet, 2pet, 2 pe, 2pe, 2 pt, 2pt, 2p, 2nd peter, second peter
    1 john, 1john, 1 jhn, 1jhn, 1 jn, 1jn, 1j, 1st john, first john
    2 john, 2john, 2 jhn, 2jhn, 2 jn, 2jn, 2j, 2nd john, second john
    3 john, 3john, 3 jhn, 3jhn, 3 jn, 3jn, 3j, 3rd  john, third john
    jude, jud, jd
    revelation, rev, re, the revelation
```

## Bibles translations
available bibles are following as ```bbl list``` shows:
```
WEBUS | World English Bible                        | World English Bible              | English    | 2000
KJV   | King James Version                         | King James Version               | English    | 1611
RVR09 | Reina-Valera                               | Reina-Valera                     | Spanish    | 1909
TB    | Brazilian Translation                      | Tradução Brasileira              | Portuguese | 1917
DELUT | Luther Bible                               | Lutherbibel                      | German     | 1912
LSG   | Louis Segond                               | Bible Segond                     | French     | 1910
SINOD | Russian Synodal Bible                      | Синодальный перевод              | Russian    | 1876
SVRJ  | Statenvertaling Jongbloed edition          | Statenvertaling Jongbloed-editie | Dutch      | 1888
RDV24 | Revised Diodati Version                    | Versione Diodati Riveduta        | Italian    | 1924
UBG   | Updated Gdansk Bible                       | Uwspółcześniona Biblia gdańska   | Polish     | 2017
UBIO  | Ukrainian Bible, Ivan Ogienko              | Біблія в пер. Івана Огієнка      | Ukrainian  | 1962
SVEN  | Svenska 1917                               | 1917 års kyrkobibel              | Swedish    | 1917
CUNP  | Chinese Union Version with New Punctuation | 新標點和合本                     | Chinese    | 1919
KRV   | Korean Revised Version                     | 개역한글                         | Korean     | 1961
JC    | Japanese Colloquial Bible                  | 口語訳                           | Japanese   | 1955
```

## Search
```bbl search {keywords and phrases}``` finds related verses and sorts in order they appear in the Bible.
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ
matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.
matthew 1:16 Jacob became the father of Joseph, the husband of Mary, from whom was born Jesus, who is called Christ.
matthew 1:17 So all the generations from Abraham to David are fourteen generations; from David to the exile to Babylo...
```

bbl search can specify a book
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in romans
romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,
romans 1:4 who was declared to be the Son of God with power according to the Spirit of holiness, by the resurrection ...
romans 1:6 among whom you are also called to belong to Jesus Christ;
```

a chapter
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in romans 3
romans 3:22 even the righteousness of God through faith in Jesus Christ to all and on all those who believe. For ther...
romans 3:24 being justified freely by his grace through the redemption that is in Christ Jesus,
romans 3:26 to demonstrate his righteousness at this present time, that he might himself be just and the justifier of...
```

or a range of chapters
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in romans 4-6
romans 4:24 but for our sake also, to whom it will be accounted, who believe in him who raised Jesus our Lord from th...
romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;
romans 5:6 For while we were yet weak, at the right time Christ died for the ungodly.
```

translation can be specified too
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in kjv
matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.
matthew 1:16 And Jacob begat Joseph the husband of Mary, of whom was born Jesus, who is called Christ.
matthew 1:17 So all the generations from Abraham to David [are] fourteen generations; and from David until the carryi...
```

book and chapter(s) can be specified together with a translation
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in romans 4-6 in kjv
romans 4:24 But for us also, to whom it shall be imputed, if we believe on him that raised up Jesus our Lord from the...
romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:
romans 5:6 For when we were yet without strength, in due time Christ died for the ungodly.
```

translation can be specified first before book (and chapter(s))
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in kjv in romans 7-9
romans 7:4 Wherefore, my brethren, ye also are become dead to the law by the body of Christ; that ye should be marrie...
romans 7:25 I thank God through Jesus Christ our Lord. So then with the mind I myself serve the law of God; but with ...

romans 8:1 [There is] therefore now no condemnation to them which are in Christ Jesus, who walk not after the flesh, ...
```

number of search result can be adjusted by command option ```-r 3``` or setting ```"searchResult": 3``` in ```config.json```
```
joel@JOEL-DESKTOP:~$ bbl search Jesus -r 3
matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.
matthew 1:16 Jacob became the father of Joseph, the husband of Mary, from whom was born Jesus, who is called Christ.
matthew 1:18 Now the birth of Jesus Christ was like this: After his mother, Mary, was engaged to Joseph, before they ...
```

## Random Verses
```bbl rand {options}``` randomly shows a verse or a chapter from entire bible, Old Testament, New Testament, or Gospels

default behavior without option first chooses a random book, then from the book chooses a chapter, then a verse from the chapter
```
joel@JOEL-DESKTOP:~$ bbl rand
1 Corinthians 4:17
Because of this I have sent Timothy to you, who is my beloved and faithful child in the Lord, who will remind you of ...
```

random verse from OT
```
joel@JOEL-DESKTOP:~$ bbl rand ot
Ezra 4:5
They hired counselors against them to frustrate their purpose all the days of Cyrus king of Persia, even until the re...
```

random verse from NT
```
joel@JOEL-DESKTOP:~$ bbl rand nt
Romans 12:19
Don’t seek revenge yourselves, beloved, but give place to God’s wrath. For it is written, “Vengeance belongs to me; I...
```

random verse from Gospels (Matthew, Mark, Luke or John)
```
joel@JOEL-DESKTOP:~$ bbl rand g
John 14:11
Believe me that I am in the Father, and the Father in me; or else believe me for the very works’ sake.
```

show random chapter, without specifying chapter, you can always show a whole random chapter by setting ```"randomlyShow": "chapter"``` in ```config.json``` 
```
joel@JOEL-DESKTOP:~$ bbl rand chapter
Romans 12
1 Therefore I urge you, brothers, by the mercies of God, to present your bodies a living sacrifice, holy, acceptable ...
2 Don’t be conformed to this world, but be transformed by the renewing of your mind, so that you may prove what is th...
3 For I say through the grace that was given me, to everyone who is among you, not to think of yourself more highly t...
...
21 Don’t be overcome by evil, but overcome evil with good.
```

## Configure custom default behavior
bbl, assuming that the username of the computer is "joel", tries to look for ```config.json``` at
* ```C:\Users\joel\.bbl\config.json``` in Windows
* ```/Users/joel/.bbl/config.json``` in MacOS
* ```/home/joel/.bbl/config.json``` in Linux

for now, translation and number of search result verses can be configured by ```config.json``` as following:
```json
    { 
        "translation": "kjv",
        "searchResult": 10,
        "randomlyShow": "chapter"
    }
```

## Installation
MacOS Installation
1. Download [bbl-1.4.pkg](https://github.com/nehemiaharchives/bbl/releases/download/v1.4/bbl-1.4.pkg)
2. Click install button, then ```bbl``` command should be available from within ```/usr/local/bin/bbl```

Linux Installation (Debian based only for now): 
1. Download [bbl_1.4-1_amd64.deb](https://github.com/nehemiaharchives/bbl/releases/download/v1.4/bbl_1.4-1_amd64.deb)
2. Run ```sudo apt install ./bbl_1.3-1_amd64.deb
 -y``` then ```bbl``` command should be available from within ```/usr/sbin/bbl``` (usually in ```$PATH```)

Windows Installation:
1. Download [bbl-1.4.msi](https://github.com/nehemiaharchives/bbl/releases/download/v1.4/bbl-1.4.msi), choose where to install, and click Install button,.
2. Add folder where you put ```bbl.exe``` to your ```%PATH%``` by editing environmental variable setting, then```bbl``` command should be available.

## Powered by
bbl was made available thanks to following:
* God the Father, Jesus Christ the Son and The Holy Spirit who encouraged me to make this software.
* Command functionality is powered by [Clikt](https://github.com/ajalt/clikt). It validates the input of the number of chapters of a book, emits error when you request more chapter than the book has.
* Search is powered by [Apache Lucene](https://github.com/apache/lucene)
* The code is written in [Kotlin](https://kotlinlang.org/) Programming Language

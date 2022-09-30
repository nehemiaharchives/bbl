# bbl
A command line tool to read Holy Bible

![screenshot](bbl700px-screenshot.png)

## Usage

bbl, with no argument/option will defaults to output Genesis chapter 1 in World English Bible.

```
joel@JOEL-LAPTOP:~$ bbl
1 In the beginning, God created the heavens and the earth.
2 The earth was formless and empty. Darkness was on the surface of the deep and God’s Spirit was hovering over the surface of the waters.
3 God said, “Let there be light,” and there was light.
...
31 God saw everything that he had made, and, behold, it was very good. There was evening and there was morning, a sixth day.
```

bbl is expected to use specifying a book and a chapter for reading Bible like this:
```
joel@JOEL-LAPTOP:~$ bbl ex 1
1 Now these are the names of the sons of Israel, who came into Egypt (every man and his household came with Jacob):
2 Reuben, Simeon, Levi, and Judah,
3 Issachar, Zebulun, and Benjamin,
...
22 Pharaoh commanded all his people, saying, “You shall cast every son who is born into the river, and every daughter you shall save alive.”
```
bbl allows to specify a verse:
```
joel@JOEL-LAPTOP:~$ bbl john 3:16
16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life.
```

or a range of verses
```
joel@JOEL-LAPTOP:~$ bbl matt 28:18-20
18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.
19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,
20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.
```

bbl also let you specify other translation of Bibles such as King James Version by supplying "in {translation}" subcommand.
```
joel@JOEL-LAPTOP:~$ bbl genesis 1 in kjv
1 In the beginning God created the heaven and the earth.
2 And the earth was without form, and void; and darkness [was] upon the face of the deep. And the Spirit of God moved upon the face of the waters.
3 And God said, Let there be light: and there was light.
...
31 And God saw every thing that he had made, and, behold, [it was] very good. And the evening and the morning were the sixth day.
```

## Translations embedded in bbl
Following abbreviations are supported to switch bible translations: 

* ```webus``` World English Bible
* ```kjv``` King James Version
* ```cunp``` Chinese Union Version with New Punctuation
* ```krv``` Korean Revised Version
* ```jc``` Japanese Colloquial

## Configure custom default behavior
bbl, assuming that the username of the computer is "joel", tries to look for ```config.json``` at 
* ```C:\Users\joel\.bbl\config.json``` in Windows
* ```/Users/joel/.bbl/config.json``` in MacOS
* ```/home/joel/.bbl/config.json``` in Linux

for now, translation and number of search result verses can be configured by ```config.json``` as following: 
```json
    { 
        "translation": "kjv",
        "searchResult": 10
    }
``` 

## Bible book names
bbl acceptes following abbreviation as command argument to specify book:
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

## Search
```bbl search {keywords and phrases}``` coomand finds related verses and sorts in order they appear in the Bible.
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ
matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.
matthew 1:16 Jacob became the father of Joseph, the husband of Mary, from whom was born Jesus, who is called Christ.
matthew 1:17 So all the generations from Abraham to David are fourteen generations; from David to the exile to Babylon fourteen generations; and from the carrying away to Babylon to the Christ, fourteen generations.
```

bbl search can specify a book
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in romans
romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,
romans 1:4 who was declared to be the Son of God with power according to the Spirit of holiness, by the resurrection from the dead, Jesus Christ our Lord,
romans 1:6 among whom you are also called to belong to Jesus Christ;
```

a chapter
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in romans 3
romans 3:22 even the righteousness of God through faith in Jesus Christ to all and on all those who believe. For there is no distinction,
romans 3:24 being justified freely by his grace through the redemption that is in Christ Jesus,
romans 3:26 to demonstrate his righteousness at this present time, that he might himself be just and the justifier of him who has faith in Jesus.
```

or a range of chapters
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in romans 4-6
romans 4:24 but for our sake also, to whom it will be accounted, who believe in him who raised Jesus our Lord from the dead,
romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;
romans 5:6 For while we were yet weak, at the right time Christ died for the ungodly.
```

translation can be specified too
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in kjv
matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.
matthew 1:16 And Jacob begat Joseph the husband of Mary, of whom was born Jesus, who is called Christ.
matthew 1:17 So all the generations from Abraham to David [are] fourteen generations; and from David until the carrying away into Babylon [are] fourteen generations; and from the carrying away into Babylon unto Christ [are] fourteen generations.
```

book and chapter(s) can be specified together with a translation
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in romans 4-6 in kjv
romans 4:24 But for us also, to whom it shall be imputed, if we believe on him that raised up Jesus our Lord from the dead;
romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:
romans 5:6 For when we were yet without strength, in due time Christ died for the ungodly.
```

translation can be specified first before book (and chapter(s))
```
joel@JOEL-LAPTOP:~$ bbl search Jesus Christ in kjv in romans 7-9
romans 7:4 Wherefore, my brethren, ye also are become dead to the law by the body of Christ; that ye should be married to another, [even] to him who is raised from the dead, that we should bring forth fruit unto God.
romans 7:25 I thank God through Jesus Christ our Lord. So then with the mind I myself serve the law of God; but with the flesh the law of sin.

romans 8:1 [There is] therefore now no condemnation to them which are in Christ Jesus, who walk not after the flesh, but after the Spirit.
```

## Installation
MacOS Installation
1. Download [bbl-1.1.pkg](https://github.com/nehemiaharchives/bbl/releases/download/v1.1/bbl-1.1.pkg)
2. Click install button, then ```bbl``` command should be available from within ```/usr/local/bin/bbl```

Linux Installation (Debian based only for now): 
1. Download [bbl_1.1_amd64.deb](https://github.com/nehemiaharchives/bbl/releases/download/v1.1/bbl_1.1_amd64.deb)
2. Run ```sudo apt install ./bbl_1.1_amd64.deb
 -y``` then ```bbl``` command should be available from within ```/usr/sbin/bbl``` (usually in ```$PATH```)

Windows Installation:
1. Download [bbl-1.1.msi](https://github.com/nehemiaharchives/bbl/releases/download/v1.1/bbl-1.1.msi), choose where to install, and click Install button,.
2. Add folder where you put ```bbl.exe``` to your ```%PATH%``` by editing environmental variable setting, then```bbl``` command should be available.

## Powered by
bbl was made available thanks to following:
* God the Father, Jesus Christ the Son and The Holy Spirit who encouraged me to make this software.
* Command functionality is powered by [Clikt](https://github.com/ajalt/clikt). It validates the input of the number of chapters of a book, emits error when you request more chapter than the book has.
* Search is powered by [Apache Lucene](https://github.com/apache/lucene)
* The code is written in [Kotlin](https://kotlinlang.org/) Programming Language

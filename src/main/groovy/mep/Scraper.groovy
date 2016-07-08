package mep

import org.cyberneko.html.parsers.SAXParser

class Scraper {
    static main(args) {
        System.setProperty("http.agent", "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)");
        println()

        // Extract the list of MEPs from the xml
        def listOfMeps = new XmlParser().parse('http://www.europarl.europa.eu/meps/en/xml.html?query=full&filter=all')
            .mep
            .collect { mep ->
                [name:mep.fullName.text(), country:mep.country.text(), group:mep.politicalGroup.text(), id:mep.id.text()]
            }

        // For each of them, parse the email link, and un-obfuscate it
        listOfMeps.take(20).indexed().collect { idx, map ->
                if(idx % 20 == 0) println "${idx} of ${listOfMeps.size()}"

                def txt = new URL("http://www.europarl.europa.eu/meps/en/$map.id/home_home.html").text
                def addr = new XmlParser(new SAXParser())
                        .parseText(txt)
                        .'**'
                        .A
                        .find { it.@id == 'email-0' }?.@href
                if(addr) {
                    map << [email: addr.replaceAll(/\[dot]/, '.')
                            .replaceAll(/\[at]/, '@')
                            .minus('mailto:')
                            .reverse()]
                }
                else {
                    println "Cannot find email for $map.name"
                    map << [email: '']
                }
                Thread.sleep(500)
            }

        // Then write our maps out to a CSV 'meps.csv'
        def columns = listOfMeps*.keySet().flatten().unique()
        new File('meps.csv').withWriter { w ->
            w.writeLine columns.collect { c -> $/"$c"/$ }.join( ',' )
            w.writeLine listOfMeps.collect { row ->
                // A row at a time
                columns.collect { colName -> $/"${row[colName]}"/$ }.join( ',' )
            }.join( '\n' )
        }
    }
}

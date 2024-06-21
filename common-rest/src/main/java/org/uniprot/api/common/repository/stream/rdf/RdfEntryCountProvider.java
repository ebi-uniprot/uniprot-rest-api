package org.uniprot.api.common.repository.stream.rdf;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class RdfEntryCountProvider {
    private static final Map<String, Map<String, Pattern>> regexMap =
            Map.of(
                    "uniprotkb",
                    Map.of(
                            "rdf",
                            Pattern.compile(
                                    "<rdf:Description rdf:about=\"+([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?+\">"),
                            "ttl",
                            Pattern.compile(
                                    "<([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?> rdf:type up:Protein ;"),
                            "nt",
                            Pattern.compile(
                                    "<http://purl.uniprot.org/uniprot/([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?> <http://purl.uniprot.org/core/reviewed>")),
                    "uniparc",
                    Map.of(
                            "rdf",
                            Pattern.compile("<rdf:Description rdf:about=\"+UPI[\\w]{10}+\">"),
                            "ttl",
                            Pattern.compile("<UPI[\\w]{10}> rdf:type up:Protein ;"),
                            "nt",
                            Pattern.compile(
                                    "<http://purl.uniprot.org/uniprot/UPI[\\w]{10}> <http://purl.uniprot.org/core/reviewed>")),
                    "uniref",
                    Map.of(
                            "rdf",
                            Pattern.compile(
                                    "<rdf:Description rdf:about=\"+(UniRef100|UniRef90|UniRef50)_([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?+\">"),
                            "ttl",
                            Pattern.compile(
                                    "<(UniRef100|UniRef90|UniRef50)_([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?> rdf:type up:Protein ;"),
                            "nt",
                            Pattern.compile(
                                    "<http://purl.uniprot.org/uniprot/(UniRef100|UniRef90|UniRef50)_([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?> <http://purl.uniprot.org/core/reviewed>")));

    public int getEntryCount(String response, String dataType, String format) {
        return (int)
                Arrays.stream(response.split("\n"))
                        .map(String::trim)
                        .filter(trimmed -> isAMatch(trimmed, dataType, format))
                        .count();
    }

    private boolean isAMatch(String line, String dataType, String format) {
        return getMatchingPattern(dataType, format).matcher(line).matches();
    }

    private Pattern getMatchingPattern(String dataType, String format) {
        return regexMap.get(dataType).get(format);
    }
}

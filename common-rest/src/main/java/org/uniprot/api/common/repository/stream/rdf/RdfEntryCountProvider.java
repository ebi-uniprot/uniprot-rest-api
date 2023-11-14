package org.uniprot.api.common.repository.stream.rdf;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class RdfEntryCountProvider {
    private static final Map<String, Map<String, String>> regexMap = Map.of(
            "uniprotkb", Map.of(
                    "rdf", "<rdf:Description rdf:about=\"+([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?+\">",
                    "ttl", "<([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?> rdf:type up:Protein ;",
                    "nt", "<http://purl.uniprot.org/uniprot/([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?> <http://purl.uniprot.org/core/reviewed>"
            ),
            "uniparc", Map.of(
                    "rdf", "<rdf:Description rdf:about=\"+UPI[\\w]{10}+\">",
                    "ttl", "<UPI[\\w]{10}> rdf:type up:Protein ;",
                    "nt", "<http://purl.uniprot.org/uniprot/UPI[\\w]{10}> <http://purl.uniprot.org/core/reviewed>"

            ),
            "uniref", Map.of(
                    "rdf", "<rdf:Description rdf:about=\"+(UniRef100|UniRef90|UniRef50)_([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?+\">",
                    "ttl", "<(UniRef100|UniRef90|UniRef50)_([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?> rdf:type up:Protein ;",
                    "nt", "<http://purl.uniprot.org/uniprot/(UniRef100|UniRef90|UniRef50)_([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?> <http://purl.uniprot.org/core/reviewed>"

            )
    );

    int getEntryCount(String response, String dataType, String format) {
        return (int) Arrays.stream(response.split("\n")).map(String::trim).filter(trimmed -> isAMatch(trimmed, dataType, format)).count();
    }

    private boolean isAMatch(String response, String dataType, String format) {
        return response.matches(getMatchingRegex(dataType, format));
    }

    private String getMatchingRegex(String dataType, String format) {
        return regexMap.get(dataType).get(format);
    }

}

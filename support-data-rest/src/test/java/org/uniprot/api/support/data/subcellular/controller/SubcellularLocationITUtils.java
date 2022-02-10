package org.uniprot.api.support.data.subcellular.controller;

import java.util.Collections;

import org.uniprot.core.cv.go.impl.GoTermBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.cv.subcell.SubcellLocationCategory;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.cv.subcell.impl.SubcellularLocationEntryBuilder;
import org.uniprot.core.impl.StatisticsBuilder;
import org.uniprot.core.json.parser.subcell.SubcellularLocationJsonConfig;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author sahmad
 * @created 22/01/2021
 */
public class SubcellularLocationITUtils {
    public static SubcellularLocationDocument createSolrDoc(String accession) {
        SubcellularLocationEntry subcellularLocationEntry =
                new SubcellularLocationEntryBuilder()
                        .name("Name value " + accession)
                        .id(accession)
                        .category(SubcellLocationCategory.LOCATION)
                        .definition("Definition value " + accession)
                        .synonymsAdd("syn value")
                        .content("content value")
                        .note("note value")
                        .referencesAdd("reference value")
                        .linksAdd("link value")
                        .isAAdd(
                                new SubcellularLocationEntryBuilder()
                                        .name("is a id")
                                        .id("SL-0002")
                                        .build())
                        .partOfAdd(
                                (new SubcellularLocationEntryBuilder()
                                        .name("part id id")
                                        .id("SL-0003")
                                        .build()))
                        .geneOntologiesAdd(new GoTermBuilder().id("goId").name("goName").build())
                        .keyword(new KeywordIdBuilder().id("kaccession").name("kid").build())
                        .statistics(new StatisticsBuilder().build())
                        .build();

        SubcellularLocationDocument document =
                SubcellularLocationDocument.builder()
                        .id(accession)
                        .name("Name value " + accession)
                        .category(SubcellLocationCategory.LOCATION.getName())
                        .definition("definition sample text")
                        .synonyms(Collections.singletonList("synonym value " + accession))
                        .subcellularlocationObj(
                                getSubcellularLocationBinary(subcellularLocationEntry))
                        .build();

        return document;
    }

    private static byte[] getSubcellularLocationBinary(SubcellularLocationEntry entry) {
        try {
            return SubcellularLocationJsonConfig.getInstance()
                    .getFullObjectMapper()
                    .writeValueAsBytes(entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Unable to parse SubcellularLocationEntry to binary json: ", e);
        }
    }
}

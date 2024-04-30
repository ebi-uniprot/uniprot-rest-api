package org.uniprot.api.support.data.disease;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.uniprot.core.Statistics;
import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.disease.impl.DiseaseCrossReferenceBuilder;
import org.uniprot.core.cv.disease.impl.DiseaseEntryBuilder;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.impl.StatisticsBuilder;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.search.document.DocumentConversionException;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiseaseSolrDocumentHelper {
    public static DiseaseDocument constructSolrDocument(String accession, long suffix) {
        DiseaseEntryBuilder diseaseBuilder = new DiseaseEntryBuilder();
        KeywordId keyword =
                new KeywordIdBuilder()
                        .name("Mental retardation" + suffix)
                        .id("KW-0991" + suffix)
                        .build();
        DiseaseCrossReference xref1 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MIM" + suffix)
                        .id("617140" + suffix)
                        .propertiesAdd("phenotype" + suffix)
                        .build();
        DiseaseCrossReference xref2 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MedGen" + suffix)
                        .id("CN238690" + suffix)
                        .build();
        DiseaseCrossReference xref3 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MeSH" + suffix)
                        .id("D000015" + suffix)
                        .build();
        DiseaseCrossReference xref4 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MeSH" + suffix)
                        .id("D008607" + suffix)
                        .build();

        Statistics statistics =
                new StatisticsBuilder()
                        .reviewedProteinCount(suffix)
                        .unreviewedProteinCount(suffix)
                        .build();

        DiseaseEntry diseaseEntry =
                diseaseBuilder
                        .name("ZTTK syndrome" + suffix)
                        .id(accession)
                        .acronym("ZTTKS" + suffix)
                        .definition(
                                "An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                        .alternativeNamesSet(
                                Arrays.asList(
                                        "Zhu-Tokita-Takenouchi-Kim syndrome",
                                        "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                        .crossReferencesSet(Arrays.asList(xref1, xref2, xref3, xref4))
                        .keywordsAdd(keyword)
                        .statistics(statistics)
                        .build();

        List<String> kwIds;
        if (diseaseEntry.getKeywords() != null) {
            kwIds =
                    diseaseEntry.getKeywords().stream()
                            .map(KeywordId::getName)
                            .collect(Collectors.toList());
        } else {
            kwIds = new ArrayList<>();
        }
        // name is a combination of id, acronym, definition, synonyms, keywords
        List<String> name =
                Stream.concat(
                                Stream.concat(
                                        Stream.of(
                                                diseaseEntry.getName(),
                                                diseaseEntry.getAcronym(),
                                                diseaseEntry.getDefinition()),
                                        kwIds.stream()),
                                diseaseEntry.getAlternativeNames().stream())
                        .collect(Collectors.toList());
        return DiseaseDocument.builder()
                .id(accession)
                .name(name)
                .diseaseObj(getDiseaseBinary(diseaseEntry))
                .build();
    }

    private static ByteBuffer getDiseaseBinary(DiseaseEntry entry) {
        try {
            return ByteBuffer.wrap(
                    DiseaseJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new DocumentConversionException(
                    "Unable to parse DiseaseEntry entry to binary json: ", e);
        }
    }
}

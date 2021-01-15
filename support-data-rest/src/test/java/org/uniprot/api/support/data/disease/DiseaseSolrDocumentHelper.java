package org.uniprot.api.support.data.disease;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.disease.impl.DiseaseCrossReferenceBuilder;
import org.uniprot.core.cv.disease.impl.DiseaseEntryBuilder;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
public class DiseaseSolrDocumentHelper {
    public static void createDiseaseDocuments(DataStoreManager storeManager, int documentCount) {
        Set<String> accessionBag = new HashSet<>();
        int batchSize = 100;
        if (documentCount <= batchSize) {
            batchSize = documentCount;
        }
        // create solr docs in batches
        for (int i = 0, j = 1; i < documentCount; i += batchSize, j++) {
            List<DiseaseDocument> diseaseDocuments = new ArrayList<>();
            IntStream.rangeClosed(1, batchSize)
                    .forEach(
                            index -> {
                                String accession = getNextUniqueAccession(accessionBag);
                                DiseaseDocument diseaseDocument =
                                        createDiseaseDocument(accession, index);
                                diseaseDocuments.add(diseaseDocument);
                            });
            log.info("Creating docs of batch {} with size {}", j, diseaseDocuments.size());
            storeManager.saveDocs(DataStoreManager.StoreType.DISEASE, diseaseDocuments);
            log.info("Total Docs Created: {}", accessionBag.size());
        }
    }

    public static void createDiseaseDocuments(
            DataStoreManager storeManager, String accession, long suffix) {
        DiseaseDocument diseaseDoc = createDiseaseDocument(accession, suffix);
        storeManager.saveDocs(DataStoreManager.StoreType.DISEASE, diseaseDoc);
    }

    private static String getNextUniqueAccession(Set<String> accessionBag) {
        String accPrefix = "DI-";
        long num = ThreadLocalRandom.current().nextLong(10000, 99999);
        String accession = accPrefix + num;
        if (accessionBag.contains(accession)) {
            return getNextUniqueAccession(accessionBag);
        }
        accessionBag.add(accession);
        return accession;
    }

    private static DiseaseDocument createDiseaseDocument(String accession, long suffix) {
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
                        .databaseType("MedGen")
                        .id("CN238690" + suffix)
                        .build();
        DiseaseCrossReference xref3 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MeSH")
                        .id("D000015" + suffix)
                        .build();
        DiseaseCrossReference xref4 =
                new DiseaseCrossReferenceBuilder().databaseType("MeSH").id("D008607").build();
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
                        .reviewedProteinCount(suffix)
                        .unreviewedProteinCount(suffix)
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
        DiseaseDocument document =
                DiseaseDocument.builder()
                        .id(accession)
                        .name(name)
                        .diseaseObj(getDiseaseBinary(diseaseEntry))
                        .build();

        return document;
    }

    private static ByteBuffer getDiseaseBinary(DiseaseEntry entry) {
        try {
            return ByteBuffer.wrap(
                    DiseaseJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse DiseaseEntry entry to binary json: ", e);
        }
    }
}

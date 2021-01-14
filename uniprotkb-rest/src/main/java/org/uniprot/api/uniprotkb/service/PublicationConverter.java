package org.uniprot.api.uniprotkb.service;

import static org.uniprot.core.util.Utils.addOrIgnoreNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.json.parser.publication.MappedPublicationsJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.publication.MappedPublications;
import org.uniprot.core.publication.MappedReference;
import org.uniprot.core.publication.UniProtKBMappedReference;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.document.publication.PublicationDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created 07/01/2021
 *
 * @author Edd
 */
@Component
@Slf4j
public class PublicationConverter
        implements BiFunction<PublicationDocument, Map<Long, LiteratureEntry>, PublicationEntry> {
    private static final ObjectMapper OBJECT_MAPPER =
            MappedPublicationsJsonConfig.getInstance().getFullObjectMapper();

    @Override
    public PublicationEntry apply(
            PublicationDocument pubDocument, Map<Long, LiteratureEntry> pubmedLiteratureEntryMap) {
        PublicationEntry.PublicationEntryBuilder pubEntryBuilder = PublicationEntry.builder();
        // pubmed present => get Citation from map; otherwise, use the citation in the binary
        if (Utils.notNullNotEmpty(pubDocument.getPubMedId())) {
            LiteratureEntry literatureEntry =
                    pubmedLiteratureEntryMap.get(Long.parseLong(pubDocument.getPubMedId()));

            pubEntryBuilder.citation(literatureEntry.getCitation());
            pubEntryBuilder.statistics(literatureEntry.getStatistics());
        } else {
            extractObject(pubDocument)
                    .ifPresent(
                            mappedPublications -> {
                                boolean citationWasAdded =
                                        addCitationIfPresent(
                                                mappedPublications::getReviewedMappedReference,
                                                pubEntryBuilder);
                                if (!citationWasAdded) {
                                    addCitationIfPresent(
                                            mappedPublications::getUnreviewedMappedReference,
                                            pubEntryBuilder);
                                }
                            });
        }

        extractObject(pubDocument)
                .ifPresent(
                        mappedPubs -> {
                            List<MappedReference> mappedRefs = new ArrayList<>();

                            addOrIgnoreNull(mappedPubs.getReviewedMappedReference(), mappedRefs);
                            addOrIgnoreNull(mappedPubs.getUnreviewedMappedReference(), mappedRefs);

                            addIfPresent(mappedPubs.getComputationalMappedReferences(), mappedRefs);
                            addIfPresent(mappedPubs.getCommunityMappedReferences(), mappedRefs);

                            pubEntryBuilder.references(mappedRefs);
                        });

        return pubEntryBuilder.build();
    }

    private boolean addCitationIfPresent(
            Supplier<UniProtKBMappedReference> referenceSupplier,
            PublicationEntry.PublicationEntryBuilder pubEntryBuilder) {
        UniProtKBMappedReference reference = referenceSupplier.get();
        if (reference != null && reference.getCitation() != null) {
            pubEntryBuilder.citation(reference.getCitation());
            return true;
        }
        return false;
    }

    static Optional<MappedPublications> extractObject(PublicationDocument document) {
        try {
            return Optional.of(
                    OBJECT_MAPPER.readValue(
                            document.getPublicationMappedReferences(), MappedPublications.class));
        } catch (Exception e) {
            log.error("Could not deserialise PublicationDocument's object", e);
        }
        return Optional.empty();
    }

    private <T extends MappedReference> void addIfPresent(
            List<T> referencesToAdd, List<MappedReference> mappedReferences) {
        if (!referencesToAdd.isEmpty()) {
            mappedReferences.addAll(referencesToAdd);
        }
    }
}

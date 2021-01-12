package org.uniprot.api.uniprotkb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.json.parser.publication.MappedPublicationsJsonConfig;
import org.uniprot.core.publication.MappedPublications;
import org.uniprot.core.publication.MappedReference;
import org.uniprot.core.publication.UniProtKBMappedReference;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.document.publication.PublicationDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static org.uniprot.core.util.Utils.addOrIgnoreNull;

/**
 * Created 07/01/2021
 *
 * @author Edd
 */
@Component
@Slf4j
public class PublicationConverter
        implements BiFunction<PublicationDocument, Map<Long, Citation>, PublicationEntry> {
    private static final ObjectMapper OBJECT_MAPPER =
            MappedPublicationsJsonConfig.getInstance().getFullObjectMapper();

    @Override
    public PublicationEntry apply(
            PublicationDocument pubDocument, Map<Long, Citation> citationMap) {
        PublicationEntry.PublicationEntryBuilder pubEntryBuilder = PublicationEntry.builder();
        // pubmed present => get Citation from map; otherwise, use the citation in the binary
        if (Utils.notNullNotEmpty(pubDocument.getPubMedId())) {
            pubEntryBuilder.citation(citationMap.get(Long.parseLong(pubDocument.getPubMedId())));
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

        PublicationEntry.Statistics stats =
                PublicationEntry.Statistics.builder()
                        .communityMappedProteinCount(
                                extractCount(pubDocument::getCommunityMappedProteinCount))
                        .computationalMappedProteinCount(
                                extractCount(pubDocument::getComputationalMappedProteinCount))
                        .reviewedMappedProteinCount(
                                extractCount(pubDocument::getReviewedMappedProteinCount))
                        .unreviewedMappedProteinCount(
                                extractCount(pubDocument::getUnreviewedMappedProteinCount))
                        .build();
        pubEntryBuilder.statistics(stats);

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

    static Long extractCount(LongSupplier countSupplier) {
        try {
            return countSupplier.getAsLong();
        } catch (NullPointerException e) {
            return 0L;
        }
    }
}

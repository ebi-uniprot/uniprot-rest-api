package org.uniprot.api.uniprotkb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.uniprot.api.uniprotkb.model.PublicationEntry2;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.json.parser.publication.MappedPublicationsJsonConfig;
import org.uniprot.core.publication.MappedPublications;
import org.uniprot.core.publication.MappedReference;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.document.publication.PublicationDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;
import java.util.function.ObjLongConsumer;

/**
 * Created 07/01/2021
 *
 * @author Edd
 */
@Component
@Slf4j
public class PublicationConverter
        implements BiFunction<PublicationDocument, Map<Long, Citation>, PublicationEntry2> {
    private static final ObjectMapper OBJECT_MAPPER =
            MappedPublicationsJsonConfig.getInstance().getFullObjectMapper();

    public static Optional<MappedPublications> extractObject(PublicationDocument document) {
        try {
            return Optional.of(
                    OBJECT_MAPPER.readValue(
                            document.getPublicationMappedReferences(), MappedPublications.class));
        } catch (Exception e) {
            log.error("Could not deserialise PublicationDocument's object", e);
        }
        return Optional.empty();
    }

    @Override
    public PublicationEntry2 apply(
            PublicationDocument pubDocument, Map<Long, Citation> citationMap) {
        PublicationEntry2.PublicationEntry2Builder pubEntryBuilder = PublicationEntry2.builder();
        if (Utils.notNullNotEmpty(pubDocument.getPubMedId())) {
            pubEntryBuilder.citation(citationMap.get(Long.parseLong(pubDocument.getPubMedId())));
        } else {
            extractObject(pubDocument)
                    .ifPresent(
                            mappedPublications -> {
                                Citation unreviewedReference =
                                        mappedPublications
                                                .getReviewedMappedReference()
                                                .getCitation();
                                Citation reviewedReference =
                                        mappedPublications
                                                .getReviewedMappedReference()
                                                .getCitation();
                                Citation realReference = null;
                                if (Utils.notNull(unreviewedReference)) {
                                    realReference = unreviewedReference;
                                }
                                if (Utils.notNull(reviewedReference)) {
                                    realReference = reviewedReference;
                                }

                                pubEntryBuilder.citation(realReference);
                            });
        }

        PublicationEntry2.Statistics statistics =
                PublicationEntry2.Statistics.builder()
                        .communityMappedProteinCount(
                                extractCount(pubDocument::getCommunityMappedProteinCount))
                        .computationalMappedProteinCount(
                                extractCount(pubDocument::getComputationalMappedProteinCount))
                        .reviewedMappedProteinCount(
                                extractCount(pubDocument::getReviewedMappedProteinCount))
                        .unreviewedMappedProteinCount(
                                extractCount(pubDocument::getUnreviewedMappedProteinCount))
                        .build();
        pubEntryBuilder.statistics(statistics);

        extractObject(pubDocument)
                .ifPresent(
                        mappedPublications -> {
                            List<MappedReference> mappedReferences = new ArrayList<>();

                            Utils.addOrIgnoreNull(
                                    mappedPublications.getReviewedMappedReference(),
                                    mappedReferences);
                            Utils.addOrIgnoreNull(
                                    mappedPublications.getUnreviewedMappedReference(),
                                    mappedReferences);
                            if (!mappedPublications.getComputationalMappedReferences().isEmpty()) {
                                mappedReferences.addAll(
                                        mappedPublications.getComputationalMappedReferences());
                            }
                            if (!mappedPublications.getCommunityMappedReferences().isEmpty()) {
                                mappedReferences.addAll(
                                        mappedPublications.getCommunityMappedReferences());
                            }
                            pubEntryBuilder.references(mappedReferences);
                        });

        return pubEntryBuilder.build();
    }

    private Long extractCount(LongSupplier countSupplier) {
        try {
            return countSupplier.getAsLong();
        } catch (NullPointerException e) {
            return 0L;
        }
    }

}

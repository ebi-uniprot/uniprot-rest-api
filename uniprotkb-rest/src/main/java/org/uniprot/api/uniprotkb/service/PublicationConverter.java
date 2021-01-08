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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

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

    public static MappedPublications extractObject(PublicationDocument document)
            throws IOException {
        return OBJECT_MAPPER.readValue(
                document.getPublicationMappedReferences(), MappedPublications.class);
    }

    @Override
    public PublicationEntry2 apply(
            PublicationDocument publicationDocument, Map<Long, Citation> citationMap) {
        PublicationEntry2.PublicationEntry2Builder pubEntryBuilder = PublicationEntry2.builder();
        if (Utils.notNullNotEmpty(publicationDocument.getPubMedId())) {
            pubEntryBuilder.citation(
                    citationMap.get(Long.parseLong(publicationDocument.getPubMedId())));
        } else {
            // TODO: 08/01/2021 need to create submissino if no pubmedid?
        }

        try {
            PublicationEntry2.Statistics statistics =
                    PublicationEntry2.Statistics.builder()
                            .communityMappedProteinCount(
                                    publicationDocument.getCommunityMappedProteinCount())
                            .computationalMappedProteinCount(
                                    publicationDocument.getComputationalMappedProteinCount())
                            .reviewedMappedProteinCount(
                                    publicationDocument.getReviewedMappedProteinCount())
                            .unreviewedMappedProteinCount(
                                    publicationDocument.getUnreviewedMappedProteinCount())
                            .build();
            pubEntryBuilder.statistics(statistics);
        } catch (Exception e) {
            log.warn("Please ensure the publication statistics job has been run");
        }

        try {
            MappedPublications mappedPublications = extractObject(publicationDocument);
            List<MappedReference> mappedReferences = new ArrayList<>();

            Utils.addOrIgnoreNull(
                    mappedPublications.getReviewedMappedReference(), mappedReferences);
            Utils.addOrIgnoreNull(
                    mappedPublications.getUnreviewedMappedReference(), mappedReferences);
            if (!mappedPublications.getComputationalMappedReferences().isEmpty()) {
                mappedReferences.addAll(mappedPublications.getComputationalMappedReferences());
            }
            if (!mappedPublications.getCommunityMappedReferences().isEmpty()) {
                mappedReferences.addAll(mappedPublications.getCommunityMappedReferences());
            }

            pubEntryBuilder.references(mappedReferences);
        } catch (IOException e) {
            log.error("Could not deserialise PublicationDocument's object", e);
        }

        return pubEntryBuilder.build();
    }
}

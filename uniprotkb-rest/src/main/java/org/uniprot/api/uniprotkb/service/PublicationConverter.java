package org.uniprot.api.uniprotkb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.model.PublicationEntry2;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.json.parser.publication.MappedPublicationsJsonConfig;
import org.uniprot.core.publication.MappedPublications;
import org.uniprot.core.publication.MappedReference;
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
public class PublicationConverter implements BiFunction<PublicationDocument, Map<Long, Citation>, PublicationEntry2> {
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
        PublicationEntry2.PublicationEntry2Builder pubEntryBuilder =
                PublicationEntry2.builder();
        pubEntryBuilder.citation(
                citationMap.get(Long.parseLong(publicationDocument.getPubMedId())));

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

        try {
            MappedPublications mappedPublications = extractObject(publicationDocument);
            List<MappedReference> mappedReferences = new ArrayList<>();

            mappedReferences.add(mappedPublications.getReviewedMappedReference());
            mappedReferences.add(mappedPublications.getUnreviewedMappedReference());
            mappedReferences.addAll(mappedPublications.getComputationalMappedReferences());
            mappedReferences.addAll(mappedPublications.getCommunityMappedReferences());

            pubEntryBuilder.references(mappedReferences);
        } catch (IOException e) {
            log.error("Could not deserialise PublicationDocument's object", e);
        }

        return pubEntryBuilder.build();
    }
}

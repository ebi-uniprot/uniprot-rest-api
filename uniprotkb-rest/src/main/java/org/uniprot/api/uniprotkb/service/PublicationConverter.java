package org.uniprot.api.uniprotkb.service;

import static org.uniprot.core.util.Utils.addOrIgnoreNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.json.parser.publication.MappedPublicationsJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.publication.MappedPublications;
import org.uniprot.core.publication.MappedReference;
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
        implements BiFunction<PublicationDocument, Map<String, LiteratureEntry>, PublicationEntry> {
    private static final ObjectMapper OBJECT_MAPPER =
            MappedPublicationsJsonConfig.getInstance().getFullObjectMapper();

    @Override
    public PublicationEntry apply(
            PublicationDocument pubDocument,
            Map<String, LiteratureEntry> pubmedLiteratureEntryMap) {
        PublicationEntry.PublicationEntryBuilder pubEntryBuilder = PublicationEntry.builder();
        Optional<MappedPublications> mappedPub = extractObject(pubDocument);

        LiteratureEntry literatureEntry = pubmedLiteratureEntryMap.get(pubDocument.getCitationId());

        pubEntryBuilder.citation(literatureEntry.getCitation());
        pubEntryBuilder.statistics(literatureEntry.getStatistics());

        mappedPub.ifPresent(
                mappedPubs -> {
                    List<MappedReference> mappedRefs = new ArrayList<>();

                    addOrIgnoreNull(mappedPubs.getUniProtKBMappedReference(), mappedRefs);

                    addIfPresent(mappedPubs.getComputationallyMappedReferences(), mappedRefs);
                    addIfPresent(mappedPubs.getCommunityMappedReferences(), mappedRefs);

                    pubEntryBuilder.references(mappedRefs);
                });

        return pubEntryBuilder.build();
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

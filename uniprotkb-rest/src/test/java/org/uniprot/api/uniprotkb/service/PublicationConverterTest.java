package org.uniprot.api.uniprotkb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.citation.JournalArticle;
import org.uniprot.core.citation.Submission;
import org.uniprot.core.citation.impl.JournalArticleBuilder;
import org.uniprot.core.publication.MappedPublications;
import org.uniprot.core.publication.MappedReference;
import org.uniprot.core.publication.MappedSource;
import org.uniprot.store.indexer.uniprot.mockers.PublicationDocumentMocker;
import org.uniprot.store.search.document.publication.PublicationDocument;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.uniprot.api.uniprotkb.service.PublicationConverter.extractObject;

class PublicationConverterTest {
    private PublicationConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new PublicationConverter();
    }

    @Test
    void convertPublicationDocumentThatHasPubmedId() {
        PublicationDocument document = PublicationDocumentMocker.create(1, 1);
        Map<Long, Citation> citationMap = new HashMap<>();
        JournalArticle journalArticle = new JournalArticleBuilder().build();
        citationMap.put(1L, journalArticle);
        PublicationEntry entry = converter.apply(document, citationMap);

        // check the journal object is the one from the map
        assertSame(entry.getCitation(), journalArticle);

        // more assertions about contents of entry
        assertThat(entry.getStatistics(), is(notNullValue()));
        assertThat(entry.getReferences(), hasSize(3));

        Set<String> sources =
                entry.getReferences().stream()
                        .map(MappedReference::getSource)
                        .map(MappedSource::getName)
                        .collect(Collectors.toSet());

        assertThat(sources, containsInAnyOrder("source 1", "source 2", "source 3"));
    }

    @Test
    void convertPublicationDocumentWithoutPubMedId() {
        PublicationDocument document = PublicationDocumentMocker.createWithoutPubmed(1);
        Map<Long, Citation> emptyCitationMap = emptyMap();
        PublicationEntry entry = converter.apply(document, emptyCitationMap);

        // check the citation is a submission
        assertThat(entry.getCitation(), is(notNullValue()));
        assertThat(entry.getCitation(), instanceOf(Submission.class));
        assertThat(entry.getCitation().getTitle(), is("Submission"));

        assertThat(entry.getStatistics(), is(notNullValue()));
        assertThat(entry.getReferences(), hasSize(1));

        Set<String> sources =
                entry.getReferences().stream()
                        .map(MappedReference::getSource)
                        .map(MappedSource::getName)
                        .collect(Collectors.toSet());

        assertThat(sources, contains("source 3"));
    }

    @Test
    void validObjectIsDeserialised() {
        PublicationDocument document = PublicationDocumentMocker.create(1, 1);
        Optional<MappedPublications> mappedPublications = extractObject(document);

        assertDoesNotThrow(() -> mappedPublications.orElseThrow(IllegalStateException::new));
    }

    @Test
    void invalidObjectDeserialisationReturnsEmpty() {
        PublicationDocument document =
                PublicationDocument.builder().publicationMappedReferences(new byte[1]).build();

        Optional<MappedPublications> mappedPublications = extractObject(document);

        assertThat(mappedPublications, is(Optional.empty()));
    }

    @Test
    void canExtractDocumentWithCount() {
        long count = 1L;
        PublicationDocument document =
                PublicationDocument.builder().reviewedMappedProteinCount(count).build();
        Long extractedCount = PublicationConverter.extractCount(document::getReviewedMappedProteinCount);
        assertThat(extractedCount, is(count));
    }

    @Test
    void canExtractDocumentWithoutCount() {
        PublicationDocument document =
                PublicationDocument.builder().build();
        Long extractedCount = PublicationConverter.extractCount(document::getReviewedMappedProteinCount);
        assertThat(extractedCount, is(0L));
    }
}

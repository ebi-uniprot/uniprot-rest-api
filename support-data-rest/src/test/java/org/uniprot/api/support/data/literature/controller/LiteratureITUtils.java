package org.uniprot.api.support.data.literature.controller;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.Author;
import org.uniprot.core.citation.CitationDatabase;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.impl.AuthorBuilder;
import org.uniprot.core.citation.impl.LiteratureBuilder;
import org.uniprot.core.citation.impl.PublicationDateBuilder;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.core.literature.impl.LiteratureEntryBuilder;
import org.uniprot.core.literature.impl.LiteratureStatisticsBuilder;
import org.uniprot.core.literature.impl.LiteratureStoreEntryBuilder;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author sahmad
 * @created 22/01/2021
 */
public class LiteratureITUtils {
    public static LiteratureDocument createSolrDoc(long pubMedId, boolean facet) {
        CrossReference<CitationDatabase> pubmed =
                new CrossReferenceBuilder<CitationDatabase>()
                        .database(CitationDatabase.PUBMED)
                        .id(String.valueOf(pubMedId))
                        .build();

        CrossReference<CitationDatabase> doi =
                new CrossReferenceBuilder<CitationDatabase>()
                        .database(CitationDatabase.DOI)
                        .id("doi " + pubMedId)
                        .build();

        Literature literature =
                new LiteratureBuilder()
                        .citationCrossReferencesAdd(pubmed)
                        .citationCrossReferencesAdd(doi)
                        .authoringGroupsAdd("group value")
                        .title("title " + pubMedId)
                        .authorsAdd(new AuthorBuilder("author " + pubMedId).build())
                        .journalName("journal " + pubMedId)
                        .firstPage("firstPage value")
                        .lastPage("lastPage value")
                        .volume("volume value")
                        .literatureAbstract("literatureAbstract value")
                        .publicationDate(new PublicationDateBuilder("2019").build())
                        .build();

        LiteratureEntry entry =
                new LiteratureEntryBuilder()
                        .citation(literature)
                        .statistics(new LiteratureStatisticsBuilder().build())
                        .build();

        LiteratureStoreEntry storeEntry =
                new LiteratureStoreEntryBuilder().literatureEntry(entry).build();

        Set<String> content = new HashSet<>();
        content.add(literature.getTitle());
        content.add(String.valueOf(pubMedId));

        LiteratureDocument document =
                LiteratureDocument.builder()
                        .id(String.valueOf(pubMedId))
                        .doi(literature.getDoiId())
                        .title(literature.getTitle())
                        .author(
                                literature.getAuthors().stream()
                                        .map(Author::getValue)
                                        .collect(Collectors.toSet()))
                        .journal(literature.getJournal().getName())
                        .published(literature.getPublicationDate().getValue())
                        .citedin(facet)
                        .mappedin(facet)
                        .content(content)
                        .literatureObj(getLiteratureBinary(storeEntry))
                        .build();

        return document;
    }

    private static ByteBuffer getLiteratureBinary(LiteratureStoreEntry entry) {
        try {
            return ByteBuffer.wrap(
                    LiteratureJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse LiteratureEntry to binary json: ", e);
        }
    }
}

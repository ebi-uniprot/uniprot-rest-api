package org.uniprot.api.uniprotkb;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.uniprot.core.builder.DBCrossReferenceBuilder;
import org.uniprot.core.citation.Author;
import org.uniprot.core.citation.CitationXrefType;
import org.uniprot.core.citation.SubmissionDatabase;
import org.uniprot.core.citation.builder.JournalArticleBuilder;
import org.uniprot.core.citation.builder.SubmissionBuilder;
import org.uniprot.core.citation.impl.AuthorImpl;
import org.uniprot.core.citation.impl.PublicationDateImpl;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureMappedReference;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.core.literature.builder.LiteratureEntryBuilder;
import org.uniprot.core.literature.builder.LiteratureMappedReferenceBuilder;
import org.uniprot.core.literature.builder.LiteratureStoreEntryBuilder;
import org.uniprot.core.uniprot.UniProtAccession;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.UniProtReference;
import org.uniprot.core.uniprot.builder.UniProtAccessionBuilder;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.core.uniprot.builder.UniProtReferenceBuilder;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 2019-12-18
 */
public class UniprotKbObjectsForTests {

    public static UniProtEntry getUniprotEntryForPublication(
            String accession, String... pubmedIds) {
        return new UniProtEntryBuilder()
                .primaryAccession(new UniProtAccessionBuilder(accession).build())
                .uniProtId(null)
                .active()
                .entryType(UniProtEntryType.SWISSPROT)
                .references(getUniProtReferencesForPublication(pubmedIds))
                .build();
    }

    public static List<UniProtReference> getUniProtReferencesForPublication(String... pubmedIds) {
        List<UniProtReference> references =
                Arrays.stream(pubmedIds)
                        .map(
                                pubmedId -> {
                                    return new UniProtReferenceBuilder()
                                            .addPositions("Position MUTAGENESIS pathol " + pubmedId)
                                            .addPositions("Position INTERACTION " + pubmedId)
                                            .citation(
                                                    new JournalArticleBuilder()
                                                            .addCitationXrefs(
                                                                    new DBCrossReferenceBuilder<
                                                                                    CitationXrefType>()
                                                                            .databaseType(
                                                                                    CitationXrefType
                                                                                            .PUBMED)
                                                                            .id(pubmedId)
                                                                            .build())
                                                            .build())
                                            .build();
                                })
                        .collect(Collectors.toList());

        references.add(
                new UniProtReferenceBuilder()
                        .addPositions("Position INTERACTION ")
                        .citation(
                                new SubmissionBuilder()
                                        .title("Submission tittle")
                                        .addAuthor("Submission Author")
                                        .submittedToDatabase(SubmissionDatabase.PDB)
                                        .build())
                        .build());
        return references;
    }

    public static List<LiteratureMappedReference> getLiteratureMappedReferences(
            String... accessions) {
        return Arrays.stream(accessions)
                .map(UniprotKbObjectsForTests::getLiteratureMappedReference)
                .collect(Collectors.toList());
    }

    public static LiteratureMappedReference getLiteratureMappedReference(String accession) {
        return new LiteratureMappedReferenceBuilder()
                .uniprotAccession(accession)
                .source("source " + accession)
                .sourceId("source id " + accession)
                .addSourceCategory("function")
                .annotation("annotation " + accession)
                .build();
    }

    public static LiteratureDocument getLiteratureDocument(long pubMedId, String... accessions) {
        LiteratureStoreEntry storeEntry = getLiteratureStoreEntry(pubMedId, accessions);
        LiteratureEntry entry = storeEntry.getLiteratureEntry();
        return LiteratureDocument.builder()
                .id(String.valueOf(pubMedId))
                .doi(entry.getDoiId())
                .title(entry.getTitle())
                .author(
                        entry.getAuthors().stream()
                                .map(Author::getValue)
                                .collect(Collectors.toSet()))
                .journal(entry.getJournal().getName())
                .published(entry.getPublicationDate().getValue())
                .content(Collections.singleton(String.valueOf(pubMedId)))
                .mappedProteins(
                        storeEntry.getLiteratureMappedReferences().stream()
                                .map(LiteratureMappedReference::getUniprotAccession)
                                .map(UniProtAccession::getValue)
                                .collect(Collectors.toSet()))
                .literatureObj(UniprotKbObjectsForTests.getLiteratureBinary(storeEntry))
                .build();
    }

    public static LiteratureStoreEntry getLiteratureStoreEntry(
            long pubMedId, String... accessions) {
        return new LiteratureStoreEntryBuilder()
                .literatureEntry(getLiteratureEntry(pubMedId))
                .literatureMappedReference(
                        UniprotKbObjectsForTests.getLiteratureMappedReferences(accessions))
                .build();
    }

    public static LiteratureEntry getLiteratureEntry(long pubMedId) {
        return new LiteratureEntryBuilder()
                .pubmedId(pubMedId)
                .doiId("doi " + pubMedId)
                .title("title " + pubMedId)
                .addAuthor(new AuthorImpl("author " + pubMedId))
                .journal("journal " + pubMedId)
                .publicationDate(new PublicationDateImpl("2019"))
                .build();
    }

    public static ByteBuffer getLiteratureBinary(LiteratureStoreEntry entry) {
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

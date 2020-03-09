package org.uniprot.api.uniprotkb;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.Author;
import org.uniprot.core.citation.CitationDatabase;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.SubmissionDatabase;
import org.uniprot.core.citation.impl.*;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureMappedReference;
import org.uniprot.core.literature.LiteratureStatistics;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.core.literature.impl.LiteratureEntryBuilder;
import org.uniprot.core.literature.impl.LiteratureMappedReferenceBuilder;
import org.uniprot.core.literature.impl.LiteratureStatisticsBuilder;
import org.uniprot.core.literature.impl.LiteratureStoreEntryBuilder;
import org.uniprot.core.uniprot.UniProtAccession;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.UniProtReference;
import org.uniprot.core.uniprot.impl.UniProtEntryBuilder;
import org.uniprot.core.uniprot.impl.UniProtReferenceBuilder;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 2019-12-18
 */
public class UniprotKbObjectsForTests {

    public static UniProtEntry getUniprotEntryForPublication(
            String accession, String... pubmedIds) {
        return new UniProtEntryBuilder(accession, "ID_" + accession, UniProtEntryType.SWISSPROT)
                .referencesSet(getUniProtReferencesForPublication(pubmedIds))
                .build();
    }

    public static List<UniProtReference> getUniProtReferencesForPublication(String... pubmedIds) {
        List<UniProtReference> references =
                Arrays.stream(pubmedIds)
                        .map(
                                pubmedId -> {
                                    return new UniProtReferenceBuilder()
                                            .referencePositionsAdd(
                                                    "Position MUTAGENESIS pathol " + pubmedId)
                                            .referencePositionsAdd(
                                                    "Position INTERACTION " + pubmedId)
                                            .citation(
                                                    new JournalArticleBuilder()
                                                            .citationCrossReferencesAdd(
                                                                    new CrossReferenceBuilder<
                                                                                    CitationDatabase>()
                                                                            .database(
                                                                                    CitationDatabase
                                                                                            .PUBMED)
                                                                            .id(pubmedId)
                                                                            .build())
                                                            .build())
                                            .build();
                                })
                        .collect(Collectors.toList());

        references.add(
                new UniProtReferenceBuilder()
                        .referencePositionsAdd("Position INTERACTION ")
                        .citation(
                                new SubmissionBuilder()
                                        .title("Submission tittle")
                                        .authorsAdd("Submission Author")
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
                .sourceCategoriesAdd("function")
                .annotation("annotation " + accession)
                .build();
    }

    public static LiteratureDocument getLiteratureDocument(long pubMedId, String... accessions) {
        LiteratureStoreEntry storeEntry = getLiteratureStoreEntry(pubMedId, accessions);
        LiteratureEntry entry = storeEntry.getLiteratureEntry();
        Literature literature = (Literature) entry.getCitation();
        return LiteratureDocument.builder()
                .id(String.valueOf(pubMedId))
                .doi(literature.getDoiId())
                .title(literature.getTitle())
                .author(
                        literature.getAuthors().stream()
                                .map(Author::getValue)
                                .collect(Collectors.toSet()))
                .journal(literature.getJournal().getName())
                .published(literature.getPublicationDate().getValue())
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
                .literatureMappedReferencesSet(
                        UniprotKbObjectsForTests.getLiteratureMappedReferences(accessions))
                .build();
    }

    public static LiteratureEntry getLiteratureEntry(long pubMedId) {
        CrossReference<CitationDatabase> pubmed =
                getCitationXref(CitationDatabase.PUBMED, String.valueOf(pubMedId));
        CrossReference<CitationDatabase> doi =
                getCitationXref(CitationDatabase.DOI, "doi " + pubMedId);

        Literature literature =
                new LiteratureBuilder()
                        .citationCrossReferencesAdd(pubmed)
                        .citationCrossReferencesAdd(doi)
                        .title("title " + pubMedId)
                        .authorsAdd(new AuthorBuilder("author " + pubMedId).build())
                        .journalName("journal " + pubMedId)
                        .publicationDate(new PublicationDateBuilder("2019").build())
                        .build();

        LiteratureStatistics statistics =
                new LiteratureStatisticsBuilder()
                        .reviewedProteinCount(10)
                        .unreviewedProteinCount(20)
                        .mappedProteinCount(30)
                        .build();

        return new LiteratureEntryBuilder().citation(literature).statistics(statistics).build();
    }

    public static CrossReference<CitationDatabase> getCitationXref(
            CitationDatabase pubmed2, String s) {
        return new CrossReferenceBuilder<CitationDatabase>().database(pubmed2).id(s).build();
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

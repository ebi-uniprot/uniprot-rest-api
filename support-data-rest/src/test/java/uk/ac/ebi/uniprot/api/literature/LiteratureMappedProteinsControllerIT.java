package uk.ac.ebi.uniprot.api.literature;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.domain.citation.Author;
import uk.ac.ebi.uniprot.domain.citation.impl.AuthorImpl;
import uk.ac.ebi.uniprot.domain.citation.impl.PublicationDateImpl;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.domain.literature.LiteratureMappedReference;
import uk.ac.ebi.uniprot.domain.literature.builder.LiteratureEntryBuilder;
import uk.ac.ebi.uniprot.domain.literature.builder.LiteratureMappedReferenceBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtAccession;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.literature.LiteratureJsonConfig;
import uk.ac.ebi.uniprot.search.document.literature.LiteratureDocument;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author lgonzales
 * @since 2019-07-10
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(LiteratureController.class)
@ExtendWith(value = {SpringExtension.class})
class LiteratureMappedProteinsControllerIT {

    private static final String MAPPED_PROTEIN_PATH = "/literature/mapped/proteins/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Test
    void getMappedProteinsReturnSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310", "P12311");
        saveEntry(11, "P12310", "P12311", "P12312");
        saveEntry(12, "P12311", "P12312", "P12313");
        saveEntry(13, "P12312", "P12313", "P12314");
        saveEntry(14, "P12313", "P12314", "P12315");

        // when
        ResultActions response = mockMvc.perform(
                get(MAPPED_PROTEIN_PATH + "P12312")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.results.*.pubmedId", contains(11, 12, 13)))
                .andExpect(jsonPath("$.results.*.title", contains("title 11", "title 12", "title 13")))
                .andExpect(jsonPath("$.results.*.publicationDate", contains("2019", "2019", "2019")))
                .andExpect(jsonPath("$.results.*.literatureMappedReferences.size()", contains(1, 1, 1)))
                .andExpect(jsonPath("$.results.*.literatureMappedReferences.*.uniprotAccession", contains("P12312", "P12312", "P12312")));
    }

    @Test
    void getMappedProteinsSortedReturnSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310", "P12311");
        saveEntry(11, "P12310", "P12311", "P12312");
        saveEntry(12, "P12311", "P12312", "P12313");
        saveEntry(13, "P12312", "P12313", "P12314");
        saveEntry(14, "P12313", "P12314", "P12315");

        // when
        ResultActions response = mockMvc.perform(
                get(MAPPED_PROTEIN_PATH + "P12312")
                        .param("sort", "title desc")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.results.*.pubmedId", contains(13, 12, 11)))
                .andExpect(jsonPath("$.results.*.title", contains("title 13", "title 12", "title 11")))
                .andExpect(jsonPath("$.results.*.publicationDate", contains("2019", "2019", "2019")))
                .andExpect(jsonPath("$.results.*.literatureMappedReferences.size()", contains(1, 1, 1)))
                .andExpect(jsonPath("$.results.*.literatureMappedReferences.*.uniprotAccession", contains("P12312", "P12312", "P12312")));
    }

    @Test
    void getMappedProteinsWithReturnFieldReturnSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310", "P12311");
        saveEntry(11, "P12310", "P12311", "P12312");
        saveEntry(12, "P12311", "P12312", "P12313");
        saveEntry(13, "P12312", "P12313", "P12314");
        saveEntry(14, "P12313", "P12314", "P12315");

        // when
        ResultActions response = mockMvc.perform(
                get(MAPPED_PROTEIN_PATH + "P12312")
                        .param("fields", "title, id, mapped_references")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.results.*.pubmedId", contains(11, 12, 13)))
                .andExpect(jsonPath("$.results.*.title", contains("title 11", "title 12", "title 13")))
                .andExpect(jsonPath("$.results.*.publicationDate").doesNotExist())
                .andExpect(jsonPath("$.results.*.literatureMappedReferences.size()", contains(1, 1, 1)))
                .andExpect(jsonPath("$.results.*.literatureMappedReferences.*.uniprotAccession", contains("P12312", "P12312", "P12312")));
    }

    @Test
    void getMappedProteinsInvalidRequestParamsBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(MAPPED_PROTEIN_PATH + "P12345")
                        .param("fields", "invalid")
                        .param("sort", "invalid invalid")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.messages.*", containsInAnyOrder(
                        //"The accession parameter has invalid format. It should be a valid UniProtKB accession",
                        "Invalid sort field 'invalid'",
                        "Invalid sort field order 'invalid'. Expected asc or desc",
                        "Invalid fields parameter value 'invalid'")));

    }

    @Test
    void getMappedProteinsInvalidPathParamsBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(MAPPED_PROTEIN_PATH + "INVALID")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.messages.*", contains(
                        "The accession parameter has invalid format. It should be a valid UniProtKB accession")));

    }

    @Test
    void getMappedProteinsNotFound() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310", "P12311");

        // when
        ResultActions response = mockMvc.perform(
                get(MAPPED_PROTEIN_PATH + "P99999")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }


    @Test
    void getMappedProteinsCanPaginateOverTwoPagesResults() throws Exception {
        // given
        IntStream.rangeClosed(10, 16).forEach(i -> saveEntry(i, "P12345", "P123" + i));

        // when first page
        ResultActions response = mockMvc.perform(
                get(MAPPED_PROTEIN_PATH + "P12345")
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("size", "5"));

        // then first page
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "7"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=5")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(5)));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());

        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];
        // when last page
        response = mockMvc.perform(
                get(MAPPED_PROTEIN_PATH + "P12345")
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("cursor", cursor)
                        .param("size", "5"));

        // then last page
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "7"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(2)));
    }

    private void saveEntry(long pubMedId, String... accessions) {
        LiteratureEntry entry = new LiteratureEntryBuilder()
                .pubmedId(pubMedId)
                .doiId("doi " + pubMedId)
                .title("title " + pubMedId)
                .addAuthor(new AuthorImpl("author " + pubMedId))
                .journal("journal " + pubMedId)
                .publicationDate(new PublicationDateImpl("2019"))
                .literatureMappedReference(getLiteratureMappedReference(accessions))
                .build();
        System.out.println("Document for PUBMED_ID: " + pubMedId);
        LiteratureDocument document = LiteratureDocument.builder()
                .id(String.valueOf(pubMedId))
                .doi(entry.getDoiId())
                .title(entry.getTitle())
                .author(entry.getAuthors().stream().map(Author::getValue).collect(Collectors.toSet()))
                .journal(entry.getJournal().getName())
                .published(entry.getPublicationDate().getValue())
                .content(Collections.singleton(String.valueOf(pubMedId)))
                .mappedProteins(entry.getLiteratureMappedReferences().stream()
                        .map(LiteratureMappedReference::getUniprotAccession)
                        .map(UniProtAccession::getValue)
                        .collect(Collectors.toSet()))
                .literatureObj(getLiteratureBinary(entry))
                .build();

        storeManager.saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    private List<LiteratureMappedReference> getLiteratureMappedReference(String... accessions) {
        return Arrays.stream(accessions)
                .map(accession -> new LiteratureMappedReferenceBuilder()
                        .uniprotAccession(accession)
                        .source("source " + accession)
                        .sourceId("source id " + accession)
                        .addSourceCategory("source Category " + accession)
                        .annotation("annotation " + accession)
                        .build())
                .collect(Collectors.toList());
    }

    private ByteBuffer getLiteratureBinary(LiteratureEntry entry) {
        try {
            return ByteBuffer.wrap(LiteratureJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse LiteratureEntry to binary json: ", e);
        }
    }
}

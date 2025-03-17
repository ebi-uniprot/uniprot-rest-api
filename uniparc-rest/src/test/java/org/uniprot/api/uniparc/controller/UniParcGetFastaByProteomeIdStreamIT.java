package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.uniparc.controller.UniParcITUtils.*;

import java.util.List;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniParcGetFastaByProteomeIdStreamIT extends AbstractStreamControllerIT {

    private static final String streamByProteomeIdRequestPath = "/uniparc/proteome/{upId}/stream";
    private static final String UP_ID = "UP000005640";

    private static final String expectedResult =
            """
                >UPI0000283A01 anotherProteinName01 OS=Name 9606 OX=9606 AC=P12301 SS=WP_168893201 PC=UP000005640:chromosome
                MLMPKRTKYRA
                >UPI0000283A02 anotherProteinName02 OS=Name 9606 OX=9606 AC=P12302 SS=WP_168893202 PC=UP000005640:chromosome
                MLMPKRTKYRAA
                >UPI0000283A03 anotherProteinName03 OS=Name 9606 OX=9606 AC=P12303 SS=WP_168893203 PC=UP000005640:chromosome
                MLMPKRTKYRAAA
                >UPI0000283A04 anotherProteinName04 OS=Name 9606 OX=9606 AC=P12304 SS=WP_168893204 PC=UP000005640:chromosome
                MLMPKRTKYRAAAA
                >UPI0000283A05 anotherProteinName05 OS=Name 9606 OX=9606 AC=P12305 SS=WP_168893205 PC=UP000005640:chromosome
                MLMPKRTKYRAAAAA
                >UPI0000283A06 anotherProteinName06 OS=Name 9606 OX=9606 AC=P12306 SS=WP_168893206 PC=UP000005640:chromosome
                MLMPKRTKYRAAAAAA
                >UPI0000283A07 anotherProteinName07 OS=Name 9606 OX=9606 AC=P12307 SS=WP_168893207 PC=UP000005640:chromosome
                MLMPKRTKYRAAAAAAA
                >UPI0000283A08 anotherProteinName08 OS=Name 9606 OX=9606 AC=P12308 SS=WP_168893208 PC=UP000005640:chromosome
                MLMPKRTKYRAAAAAAAA
                >UPI0000283A09 anotherProteinName09 OS=Name 9606 OX=9606 AC=P12309 SS=WP_168893209 PC=UP000005640:chromosome
                MLMPKRTKYRAAAAAAAAA
                >UPI0000283A10 anotherProteinName10 OS=Name 9606 OX=9606 AC=P12310 SS=WP_168893210 PC=UP000005640:chromosome
                MLMPKRTKYRAAAAAAAAAA
                """;

    @Autowired private UniProtStoreClient<UniParcEntryLight> storeClient;
    @Autowired private UniParcCrossReferenceStoreClient xRefStoreClient;
    @Autowired protected MockMvc mockMvc;
    @Autowired private SolrClient solrClient;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        saveStreamEntries(xrefGroupSize, cloudSolrClient, storeClient, xRefStoreClient);

        // for the following tests, ensure the number of hits
        // for each query is less than the maximum number allowed
        // to be streamed (configured in {@link
        // org.uniprot.api.common.repository.store.StreamerConfigProperties})
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);
    }

    @Test
    void streamByProteomeIdCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID).header(ACCEPT, FASTA_MEDIA_TYPE_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(expectedResult));
    }

    @Test
    void streamByProteomeIdBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(streamByProteomeIdRequestPath, "INVALID")
                                .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE)
                                .param("download", "invalid"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("Error messages")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "The 'download' parameter has invalid format. It should be a boolean true or false.")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "The 'upid' value has invalid format. It should be a valid Proteome UPID")));
    }

    @Test
    void streamByProteomeIdyPDownloadCompressedFile() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE)
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniparc_")));
    }

    @Test
    void streamByProteomeIdDefaultSearchWithLowerCaseId() throws Exception {

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID.toLowerCase())
                        .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(expectedResult));
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypesForUniParcStreamByByProteomeId")
    void streamByProteomeIdAllContentType(MediaType mediaType) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID).header(ACCEPT, mediaType);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    private Stream<Arguments> getContentTypesForUniParcStreamByByProteomeId() {
        return super.getContentTypes(streamByProteomeIdRequestPath);
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniparc);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    private Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }
}

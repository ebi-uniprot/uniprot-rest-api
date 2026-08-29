package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.controller.ControllerITUtils;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.search.ProteomeTaxonomyRepository;
import org.uniprot.api.uniprotkb.common.repository.store.precomputed.PrecomputedAnnotationStoreClient;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.core.proteome.ProteomeType;
import org.uniprot.core.proteome.impl.ProteomeEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.precomputed.PrecomputedAnnotationDocument;
import org.uniprot.store.search.document.proteome.ProteomeDocument;
import org.uniprot.store.spark.indexer.precomputed.mapper.PrecomputedAnnotationEntryToDocumentMapper;
import org.uniprot.store.spark.indexer.proteome.mapper.ProteomeEntryToProteomeDocumentMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ContextConfiguration(classes = {UniProtKBREST.class, ErrorHandlerConfig.class})
@AutoConfigureWebClient
@WebMvcTest(PrecomputedUniProtKBController.class)
@ExtendWith(value = {SpringExtension.class, MockitoExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PrecomputedUniProtKBStreamControllerIT extends AbstractStreamControllerIT {
    private static final String streamRequestPath = "/uniprotkb/precomputed/proteome/{upId}/stream";

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    private final PrecomputedAnnotationEntryToDocumentMapper
            precomputedAnnotationEntryToDocumentMapper =
                    new PrecomputedAnnotationEntryToDocumentMapper();
    private final ProteomeEntryToProteomeDocumentMapper proteomeEntryToProteomeDocumentMapper =
            new ProteomeEntryToProteomeDocumentMapper();

    @Autowired private PrecomputedAnnotationStoreClient precomputedAnnotationStoreClient;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient solrClient;

    @Qualifier("precomputedAnnotationTupleStream")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Autowired private TaxonomyLineageRepository taxRepository;
    @Autowired private ProteomeTaxonomyRepository proteomeTaxonomyRepository;

    @Autowired private MockMvc mockMvc;

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.precomputedannotation, SolrCollection.proteome);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    @Override
    protected SolrClient getSolrClient() {
        return solrClient;
    }

    /**
     * For the following tests, ensure the number of hits for each query is less than the maximum
     * number allowed to be streamed (configured in {@link
     * org.uniprot.api.common.repository.stream.store.StreamerConfigProperties})
     */
    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        saveEntries();
    }

    @Test
    void streamByProteomeIdCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath, "UP000000002")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Content-Disposition"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "UP000000001-992",
                                        "UP000000002-992",
                                        "UP000000003-992",
                                        "UP000000004-992",
                                        "UP000000005-992",
                                        "UP000000006-992",
                                        "UP000000007-992",
                                        "UP000000008-992",
                                        "UP000000009-992",
                                        "UP000000010-992")))
                // To test that the whole entry is present in the response
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.annotationScore", everyItem(is(0.0))))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CACHE_CONTROL, ControllerITUtils.CACHE_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .stringValues(
                                        HttpHeaders.VARY,
                                        HttpHeaders.ACCEPT,
                                        HttpHeaders.ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE));
    }

    @Test
    void streamByProteomeIdCanReturnSuccessWithSortAccessionDesc() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath, "UP000000002")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .queryParam("sort", "accession desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Content-Disposition"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "UP000000010-992",
                                        "UP000000009-992",
                                        "UP000000008-992",
                                        "UP000000007-992",
                                        "UP000000006-992",
                                        "UP000000005-992",
                                        "UP000000004-992",
                                        "UP000000003-992",
                                        "UP000000002-992",
                                        "UP000000001-992")));
    }

    @Test
    void streamByProteomeIdCanReturnSuccessForListContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath, "UP000000002")
                        .header(HttpHeaders.ACCEPT, LIST_MEDIA_TYPE_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        String content =
                mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(
                                MockMvcResultMatchers.header().doesNotExist("Content-Disposition"))
                        .andExpect(
                                MockMvcResultMatchers.header()
                                        .string(
                                                HttpHeaders.CACHE_CONTROL,
                                                ControllerITUtils.CACHE_VALUE))
                        .andExpect(
                                MockMvcResultMatchers.header()
                                        .stringValues(
                                                HttpHeaders.VARY,
                                                HttpHeaders.ACCEPT,
                                                HttpHeaders.ACCEPT_ENCODING,
                                                HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                                HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        IntStream.rangeClosed(1, 10)
                .mapToObj(i -> String.format("UP%09d-992", i))
                .forEach(id -> assertThat(content, containsString(id)));
    }

    @Test
    void streamByProteomeIdDownloadCompressedFile() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath, "UP000000002")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniprotkb_")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamByProteomeIdTaxonomyNotFound() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath, "UP000000111")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                MediaType.APPLICATION_XML_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                FF_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE
            })
    void streamByProteomeIdUnsupportedAcceptContentTypes(String mediaType) throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get(streamRequestPath, "UP000000002")
                                .header(HttpHeaders.ACCEPT, mediaType))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 10; i++) {
            savePrecomputedAnnotationEntry(i, 992);
        }
        solrClient.commit(SolrCollection.precomputedannotation.name());
        saveProteomeEntry("UP000000002", 992, 991, 993);
        solrClient.commit(SolrCollection.proteome.name());
    }

    private void saveProteomeEntry(String upId, long taxId, long... taxLinageId) throws Exception {
        Taxonomy taxonomy = new TaxonomyBuilder().taxonId(taxId).build();

        ProteomeEntryBuilder builder =
                new ProteomeEntryBuilder()
                        .proteomeId(upId)
                        .taxonomy(taxonomy)
                        .proteomeType(ProteomeType.REFERENCE)
                        .annotationScore(15);

        for (long id : taxLinageId) {
            builder.taxonLineagesAdd(new TaxonomyLineageBuilder().taxonId(id).build());
        }
        builder.taxonLineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId).build());
        ProteomeEntry proteomeEntry = builder.build();
        ProteomeDocument proteomeDocument =
                proteomeEntryToProteomeDocumentMapper.call(proteomeEntry);
        solrClient.addBean(SolrCollection.proteome.name(), proteomeDocument);
    }

    private void savePrecomputedAnnotationEntry(int upId, int taxId) throws Exception {
        UniProtKBEntryBuilder uniProtKBEntryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
        String acc = String.format("UP%09d-%d", upId, taxId);
        uniProtKBEntryBuilder.primaryAccession(acc);

        UniProtKBEntry uniProtKBEntry = uniProtKBEntryBuilder.build();
        PrecomputedAnnotationDocument precomputedAnnotationDocument =
                precomputedAnnotationEntryToDocumentMapper.call(uniProtKBEntry);

        solrClient.addBean(
                SolrCollection.precomputedannotation.name(), precomputedAnnotationDocument);
        precomputedAnnotationStoreClient.saveEntry(uniProtKBEntry);
    }
}

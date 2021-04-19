package org.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.uniprot.api.proteome.controller.ProteomeControllerITUtils.getExcludedProteomeDocument;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.*;
import org.uniprot.core.proteome.impl.ComponentBuilder;
import org.uniprot.core.proteome.impl.ProteomeEntryBuilder;
import org.uniprot.core.proteome.impl.ProteomeIdBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author jluo
 * @date: 12 Jun 2019
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            ProteomeRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ProteomeController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            ProteomeGetIdControllerIT.ProteomeGetIdParameterResolver.class,
            ProteomeGetIdControllerIT.ProteomeGetIdContentTypeParamResolver.class
        })
class ProteomeGetIdControllerIT extends AbstractGetByIdControllerIT {
    private static final String UPID = "UP000005640";
    private static final String EXCLUDED_PROTEOME = "UP999999999";
    @Autowired private MockMvc mockMvc;

    @Autowired private ProteomeQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.PROTEOME;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.proteome;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {
        ProteomeEntry entry = create();
        ProteomeDocument document = new ProteomeDocument();
        document.upid = UPID;
        document.proteomeStored = getBinary(entry);

        getStoreManager().saveDocs(DataStoreManager.StoreType.PROTEOME, document);

        saveExcluded();
    }

    @Override
    protected String getIdRequestPath() {
        return "/proteomes/{upid}";
    }

    @Test
    void excludedIdReturnSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(
                        get(getIdRequestPath(), EXCLUDED_PROTEOME)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", is(EXCLUDED_PROTEOME)))
                .andExpect(jsonPath("$.proteomeType", is(ProteomeType.EXCLUDED.getName())))
                .andExpect(jsonPath("$.exclusionReasons", contains("contaminated")));
    }

    private ByteBuffer getBinary(ProteomeEntry entry) {
        try {
            return ByteBuffer.wrap(
                    ProteomeJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    private ProteomeEntry create() {
        ProteomeId proteomeId = new ProteomeIdBuilder(UPID).build();
        String description = "about some proteome";
        Taxonomy taxonomy =
                new TaxonomyBuilder().taxonId(9606).scientificName("Homo sapiens").build();
        LocalDate modified = LocalDate.of(2015, 11, 5);
        //	String reId = "UP000005641";
        //	ProteomeId redId = new ProteomeIdBuilder (reId).build();
        List<CrossReference<ProteomeDatabase>> xrefs = new ArrayList<>();
        CrossReference<ProteomeDatabase> xref1 =
                new CrossReferenceBuilder<ProteomeDatabase>()
                        .database(ProteomeDatabase.GENOME_ACCESSION)
                        .id("ACA121")
                        .build();
        CrossReference<ProteomeDatabase> xref2 =
                new CrossReferenceBuilder<ProteomeDatabase>()
                        .database(ProteomeDatabase.GENOME_ANNOTATION)
                        .id("ADFDA121")
                        .build();
        xrefs.add(xref1);
        xrefs.add(xref2);
        List<Component> components = new ArrayList<>();
        Component component1 =
                new ComponentBuilder()
                        .name("someName1")
                        .description("some description")
                        .proteinCount(10)
                        .build();

        Component component2 =
                new ComponentBuilder()
                        .name("someName2")
                        .description("some description 2")
                        .proteinCount(11)
                        .build();

        components.add(component1);
        components.add(component2);
        List<Citation> citations = new ArrayList<>();
        ProteomeEntryBuilder builder =
                new ProteomeEntryBuilder()
                        .proteomeId(proteomeId)
                        .description(description)
                        .taxonomy(taxonomy)
                        .modified(modified)
                        .proteomeType(ProteomeType.NORMAL)
                        //	.redundantTo(redId)
                        .componentsSet(components)
                        .superkingdom(Superkingdom.EUKARYOTA)
                        .citationsSet(citations)
                        .annotationScore(15);

        return builder.build();
    }

    private void saveExcluded() {
        ProteomeDocument excludedProteomeDoc = getExcludedProteomeDocument(EXCLUDED_PROTEOME);
        getStoreManager().saveDocs(DataStoreManager.StoreType.PROTEOME, excludedProteomeDoc);
    }

    static class ProteomeGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(UPID)
                    .resultMatcher(jsonPath("$.id", is(UPID)))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("UP000005646")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(UPID)
                    .fields("upid,organism")
                    .resultMatcher(jsonPath("$.id", is(UPID)))
                    .resultMatcher(jsonPath("$.taxonomy.taxonId", is(9606)))
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(UPID)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class ProteomeGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(UPID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.id", is(UPID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(content().string(containsString(UPID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(UPID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Proteome Id\tOrganism\tOrganism Id\tProtein count")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UP000005640\tHomo sapiens\t9606\t21")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}

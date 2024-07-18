package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;
import static org.uniprot.api.rest.output.converter.ConverterConstants.*;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.NTriplesPrologs;
import org.uniprot.api.rest.service.RdfPrologs;
import org.uniprot.api.rest.service.TurtlePrologs;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.util.Utils;
import org.uniprot.cv.taxonomy.TaxonomicNode;
import org.uniprot.cv.taxonomy.TaxonomyRepo;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.util.TaxonomyRepoUtil;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.document.uniparc.UniParcDocumentConverter;

@ContextConfiguration(classes = {UniParcRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcEntryLightController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcLightGetIdControllerIT.UniParcLightGetIdParameterResolver.class,
            UniParcLightGetIdControllerIT.UniParcLightGetIdContentTypeParamResolver.class
        })
class UniParcLightGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final TaxonomyRepo taxonomyRepo = TaxonomyRepoMocker.getTaxonomyRepo();
    private static final String UPI_PREF = "UPI0000083D";
    public static final String UNIPARC_ID = "UPI0000083D01";

    @MockBean(name = "uniParcRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired private UniParcLightStoreClient storeClient;

    @Autowired private UniParcCrossReferenceStoreClient xRefStoreClient;

    @Autowired private MockMvc mockMvc;

    @Autowired private UniParcQueryRepository repository;

    @BeforeAll
    void initDataStore() {
        getStoreManager().addStore(DataStoreManager.StoreType.UNIPARC_LIGHT, storeClient);
        getStoreManager()
                .addStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, xRefStoreClient);
    }

    @BeforeEach
    void setUp() {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    @AfterEach
    void cleanStoreClient() {
        storeClient.truncate();
        xRefStoreClient.truncate();
    }

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIPARC;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.uniparc;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {
        UniParcEntry entry = createEntry(1, UPI_PREF);

        UniParcDocumentConverter converter = new UniParcDocumentConverter();
        UniParcDocument doc = converter.convert(entry);
        UniParcDocument.UniParcDocumentBuilder builder = doc.toBuilder();
        for (UniParcCrossReference xref : entry.getUniParcCrossReferences()) {
            if (Utils.notNull(xref.getOrganism())) {
                List<TaxonomicNode> nodes =
                        TaxonomyRepoUtil.getTaxonomyLineage(
                                taxonomyRepo, (int) xref.getOrganism().getTaxonId());
                builder.organismId((int) xref.getOrganism().getTaxonId());
                nodes.forEach(
                        node -> {
                            builder.taxLineageId(node.id());
                            List<String> names = TaxonomyRepoUtil.extractTaxonFromNode(node);
                            names.forEach(builder::organismTaxon);
                        });
            }
        }
        getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPARC, doc);

        UniParcEntryLight entryLight = convertToUniParcEntryLight(entry);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPARC_LIGHT, entryLight);
        for (UniParcCrossReference xref : entry.getUniParcCrossReferences()) {
            String key = getUniParcXRefId(entry.getUniParcId().getValue(), xref);
            xRefStoreClient.saveEntry(key, xref);
        }
    }

    @Override
    protected String getIdRequestPath() {
        return "/uniparc/{upi}/light";
    }

    static class UniParcLightGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .resultMatcher(jsonPath("$.uniParcId", is("UPI0000083D01")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("UPI0000083A09")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .fields("upi,gene")
                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                    .resultMatcher(jsonPath("$.geneNames", contains("geneName01")))
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class UniParcLightGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(UNIPARC_ID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(UNIPARC_ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(startsWith(RdfPrologs.UNIPARC_PROLOG)))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    """
                                                                                <sample>text</sample>
                                                                                <anotherSample>text2</anotherSample>
                                                                                <someMore>text3</someMore>
                                                                            </rdf:RDF>""")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            startsWith(
                                                                    TurtlePrologs.UNIPARC_PROLOG)))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    """
                                                                                <sample>text</sample>
                                                                                <anotherSample>text2</anotherSample>
                                                                                <someMore>text3</someMore>
                                                                            </rdf:RDF>""")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            startsWith(
                                                                    NTriplesPrologs
                                                                            .N_TRIPLES_COMMON_PROLOG)))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    """
                                                                                <sample>text</sample>
                                                                                <anotherSample>text2</anotherSample>
                                                                                <someMore>text3</someMore>
                                                                            </rdf:RDF>""")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(UNIPARC_ID)))
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
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'upi' value has invalid format. It should be a valid UniParc UPI"))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'upi' value has invalid format. It should be a valid UniParc UPI"))
                                    .build())
                    .build();
        }
    }
}

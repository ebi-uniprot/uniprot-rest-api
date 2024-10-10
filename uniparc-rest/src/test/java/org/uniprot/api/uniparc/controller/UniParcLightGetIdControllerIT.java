package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;
import static org.uniprot.api.rest.output.converter.ConverterConstants.*;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetByIdParameterResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.NTriplesPrologs;
import org.uniprot.api.rest.service.RdfPrologs;
import org.uniprot.api.rest.service.TurtlePrologs;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;

@ContextConfiguration(classes = {UniParcRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcEntryLightController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcLightGetIdControllerIT.UniParcLightGetByIdParameterResolver.class,
            UniParcLightGetIdControllerIT.UniParcLightGetIdContentTypeParamResolver.class
        })
class UniParcLightGetIdControllerIT extends BaseUniParcGetByIdControllerTest {

    @MockBean(name = "uniParcRdfRestTemplate")
    private RestTemplate restTemplate;

    protected String getIdRequestPath() {
        return "/uniparc/{upi}/light";
    }

    @Override
    protected String getIdPathValue() {
        return UNIPARC_ID;
    }

    @BeforeEach
    void setUp() {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    @Test
    void idWithExtensionMeansUseThatContentType(GetIdParameter idParameter) throws Exception {
        // do nothing
    }

    static class UniParcLightGetByIdParameterResolver extends AbstractGetByIdParameterResolver {

        @Override
        protected GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                    .resultMatcher(jsonPath("$.oldestCrossRefCreated").exists())
                    .resultMatcher(jsonPath("$.mostRecentCrossRefUpdated").exists())
                    .resultMatcher(jsonPath("$.crossReferenceCount", is(25)))
                    .resultMatcher(
                            jsonPath(
                                    "$.commonTaxons[*].topLevel",
                                    contains("cellular organisms", "other entries")))
                    .resultMatcher(
                            jsonPath(
                                    "$.commonTaxons[*].commonTaxon",
                                    contains("Bacteria", "plasmids")))
                    .resultMatcher(jsonPath("$.uniProtKBAccessions", contains("P12301")))
                    .resultMatcher(jsonPath("$.sequence.value", is("MLMPKRTKYRA")))
                    .resultMatcher(jsonPath("$.sequenceFeatures.size()", is(13)))
                    .build();
        }

        @Override
        protected GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.size()", is(1)))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                    .build();
        }

        @Override
        protected GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("UPI0000083A99")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        protected GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .fields("upi,gene")
                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                    .resultMatcher(
                            header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                    .resultMatcher(jsonPath("$.geneNames", contains("geneName01")))
                    .resultMatcher(jsonPath("$.commonTaxons").doesNotExist())
                    .resultMatcher(jsonPath("$.uniProtKBAccessions").doesNotExist())
                    .resultMatcher(jsonPath("$.sequenceFeatures").doesNotExist())
                    .build();
        }

        @Override
        protected GetIdParameter withInvalidFilterParameter() {
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
        protected GetIdContentTypeParam idSuccessContentTypesParam() {
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
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Entry\tOrganisms\tUniProtKB\tFirst seen\tLast seen\tLength")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UPI0000083D01\t\tP12301\t")))
                                    .build())
                    //                    .contentTypeParam(
                    //                            ContentTypeParam.builder()
                    //                                    .contentType(MediaType.APPLICATION_XML)
                    //                                    .resultMatcher(
                    //                                            content()
                    //                                                    .string(
                    //                                                            startsWith(
                    //
                    // XML_DECLARATION
                    //                                                                            +
                    // UNIPARC_XML_SCHEMA)))
                    //
                    // .resultMatcher(content().string(containsString(UNIPARC_ID)))
                    //                                    .resultMatcher(
                    //                                            content()
                    //                                                    .string(
                    //                                                            endsWith(
                    //
                    // COPYRIGHT_TAG
                    //                                                                            +
                    // UNIPARC_XML_CLOSE_TAG)))
                    //                                    .build())
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
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">UPI0000083D01 status=active")))
                                    .resultMatcher(content().string(containsString("MLMPKRTKYRA")))
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
        protected GetIdContentTypeParam idBadRequestContentTypesParam() {
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
                    //                    .contentTypeParam(
                    //                            ContentTypeParam.builder()
                    //                                    .contentType(MediaType.APPLICATION_XML)
                    //                                    .resultMatcher(
                    //                                            content()
                    //                                                    .string(
                    //                                                            containsString(
                    //                                                                    "The 'upi'
                    // value has invalid format. It should be a valid UniParc UPI")))
                    //                                    .build())
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

    @Test
    void withFilterFieldsReturnSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(getIdRequestPath(), UNIPARC_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "upi,gene");

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                .andExpect(jsonPath("$.geneNames", contains("geneName01")))
                .andExpect(jsonPath("$.commonTaxons").doesNotExist())
                .andExpect(jsonPath("$.uniProtKBAccessions").doesNotExist())
                .andExpect(jsonPath("$.sequenceFeatures").doesNotExist());
    }
}

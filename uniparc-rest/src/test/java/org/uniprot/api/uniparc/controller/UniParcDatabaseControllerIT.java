package org.uniprot.api.uniparc.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.*;

import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.common.repository.UniParcDataStoreTestConfig;
import org.uniprot.api.uniparc.common.repository.UniParcStreamConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

/**
 * @author sahmad
 * @date: 29 Mar 2021
 */
@ContextConfiguration(
        classes = {
            UniParcStreamConfig.class,
            UniParcDataStoreTestConfig.class,
            UniParcRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcDatabaseController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcDatabaseControllerIT.UniParcGetIdParameterResolver.class,
            UniParcDatabaseControllerIT.UniParcGetIdContentTypeParamResolver.class
        })
class UniParcDatabaseControllerIT extends AbstractGetSingleUniParcByIdTest {
    @Override
    protected String getIdPathValue() {
        return UNIPARC_ID;
    }

    @Override
    protected String getIdRequestPath() {
        return "/uniparc/{upi}/databases";
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void testGetDatabasesWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {

        // given
        saveEntry();
        assertThat(name, notNullValue());
        assertThat(paths, notNullValue());
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath(), getIdPathValue())
                                        .param("fields", name)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.results.size()", greaterThan(0)))
                        .andExpect(jsonPath("$.results.*.database").exists())
                        .andExpect(jsonPath("$.results.*.id").exists());

        for (String path : paths) {
            String returnFieldValidatePath = "$.results[*]." + path;
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    @Test
    void testGetDatabasesWithSizeAndPagination() throws Exception {
        // when
        saveEntry();
        Integer size = 3;
        Integer total = 6;
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath(), getIdPathValue())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", String.valueOf(size)));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "6"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];

        // when 2nd page
        response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath(), getIdPathValue())
                                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                                        .param("size", String.valueOf(size))
                                        .param("cursor", cursor));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "6"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", Matchers.is(total - size)));
    }

    @Test
    void testGetByAccessionWithInActiveCrossRefFilterSuccess() throws Exception {
        // when
        String active = "false";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[*].id", notNullValue()))
                .andExpect(jsonPath("$.results[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[*].organism", notNullValue()))
                .andExpect(jsonPath("$.results[*].active", contains(false)));
    }

    @Test
    void testGetByAccessionWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String taxonIds = "9606,5555";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("taxonIds", taxonIds));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[*].id", notNullValue()))
                .andExpect(jsonPath("$.results[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.results[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[*].organism.taxonId", hasItem(9606)));
    }

    @Test
    void testGetByAccessionWithDBFilterSuccess() throws Exception {
        // when
        String dbTypes = "UniProtKB/TrEMBL,embl";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("dbTypes", dbTypes));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", iterableWithSize(2)))
                .andExpect(jsonPath("$.results[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.results[*].organism", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[*].database",
                                containsInAnyOrder("UniProtKB/TrEMBL", "EMBL")));
    }

    @Test
    void testGetByAccessionWithActiveCrossRefFilterSuccess() throws Exception {
        // when
        String active = "true";
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), getIdPathValue())
                                        .param("active", active));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", iterableWithSize(5)))
                .andExpect(jsonPath("$.results[*].id", notNullValue()))
                .andExpect(jsonPath("$.results[*].id", hasItem(ACCESSION)))
                .andExpect(jsonPath("$.results[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[*].organism", notNullValue()))
                .andExpect(jsonPath("$.results[*].active", contains(true, true, true, true, true)));
    }

    static class UniParcGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .resultMatcher(jsonPath("$.results.size()", is(6)))
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
                    .fields("protein,organism")
                    .resultMatcher(jsonPath("$.results.*.organism").exists())
                    .resultMatcher(jsonPath("$.results.*.proteinName").exists())
                    .resultMatcher(jsonPath("$.results.*.version").doesNotExist())
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

    static class UniParcGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(UNIPARC_ID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.results.size()", is(6)))
                                    .resultMatcher(jsonPath("$.results.*.database").exists())
                                    .resultMatcher(jsonPath("$.results.*.id").exists())
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Database\tIdentifier\tVersion\tOrganism\tFirst seen\tLast seen\tActive")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UniProtKB/Swiss-Prot\tP10001\t7\tName 7787\t2017-05-17\t2017-02-27\tYes\n"
                                                                            + "UniProtKB/TrEMBL\tP12301\t7\tName 9606\t2017-02-12\t2017-04-23\tYes\n"
                                                                            + "RefSeq\tWP_168893201\t7\t\t2017-02-12\t2017-04-23\tYes\n"
                                                                            + "EMBL\tembl1\t7\t\t2017-02-12\t2017-04-23\tYes\n"
                                                                            + "UNIMES\tunimes1\t7\t\t2017-02-12\t2017-04-23\tNo\n"
                                                                            + "VectorBase\tcommon-vector\t7\t\t2017-02-12\t2017-04-23\tYes")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(emptyString())))
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
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'upi' value has invalid format. It should be a valid UniParc UPI"))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }
    }

    @SuppressWarnings("squid:S2699")
    @Test
    void idWithExtensionMeansUseThatContentType(GetIdParameter idParameter) {
        // do nothing, need to override to use AbstractGetSingleUniParcByIdTest
        // because the path param is in the middle of the url and we cannot use pattern
        // {pathParam}.json
    }

    private Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC_CROSSREF)
                .getReturnFields()
                .stream()
                .map(returnField -> Arguments.of(returnField.getName(), returnField.getPaths()));
    }
}

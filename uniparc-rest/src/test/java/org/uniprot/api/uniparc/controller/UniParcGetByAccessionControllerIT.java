package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.repository.store.UniParcStreamConfig;

/**
 * @author sahmad
 * @created 17/08/2020
 */
@ContextConfiguration(
        classes = {
            UniParcStreamConfig.class,
            UniParcDataStoreTestConfig.class,
            UniParcRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcGetByAccessionControllerIT.UniParcGetByAccessionParameterResolver.class,
            UniParcGetByAccessionControllerIT.UniParcGetByAccessionContentTypeParamResolver.class
        })
class UniParcGetByAccessionControllerIT extends AbstractGetSingleUniParcByIdTest {

    @Override
    protected String getIdRequestPath() {
        return "/uniparc/accession/{accession}";
    }

    @Override
    protected String getIdPathValue() {
        return ACCESSION;
    }

    static class UniParcGetByAccessionParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        protected GetIdParameter validIdParameter() {
            GetIdParameter.GetIdParameterBuilder idParam = GetIdParameter.builder().id(ACCESSION);
            idParam.resultMatcher(jsonPath("$.uniParcId", equalTo(UNIPARC_ID)));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences", iterableWithSize(5)));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].id", hasItem(ACCESSION)));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].version", notNullValue()));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].versionI", notNullValue()));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].active", notNullValue()));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].created", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("$.uniParcCrossReferences[*].lastUpdated", notNullValue()));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].taxonomy", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("uniParcCrossReferences[*].taxonomy.scientificName", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("uniParcCrossReferences[*].taxonomy.commonName", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("$.uniParcCrossReferences[*].proteinName", notNullValue()));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].geneName", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("$.uniParcCrossReferences[*].proteomeId", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("$.uniParcCrossReferences[*].component", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("$.uniParcCrossReferences[*].proteomeId", notNullValue()));
            idParam.resultMatcher(jsonPath("$.uniParcCrossReferences[*].chain", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequence", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequence.value", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequence.length", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequence.molWeight", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequence.crc64", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequence.md5", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequenceFeatures", iterableWithSize(13)));
            idParam.resultMatcher(jsonPath("$.sequenceFeatures[*].database", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequenceFeatures[*].databaseId", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequenceFeatures[*].locations", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequenceFeatures[0].locations", iterableWithSize(2)));
            idParam.resultMatcher(
                    jsonPath("$.sequenceFeatures[*].locations[*].start", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("$.sequenceFeatures[*].locations[*].end", notNullValue()));
            idParam.resultMatcher(jsonPath("$.sequenceFeatures[*].interproGroup", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("$.sequenceFeatures[*].interproGroup.id", notNullValue()));
            idParam.resultMatcher(
                    jsonPath("$.sequenceFeatures[*].interproGroup.name", notNullValue()));
            idParam.resultMatcher(jsonPath("$.oldestCrossRefCreated", notNullValue()));
            idParam.resultMatcher(jsonPath("$.mostRecentCrossRefUpdated", notNullValue()));
            return idParam.build();
        }

        @Override
        protected GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    Matchers.contains(
                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                    .build();
        }

        @Override
        protected GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("P10101")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath("$.messages.*", Matchers.contains("Resource not found")))
                    .build();
        }

        @Override
        protected GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("upi,organism")
                    .resultMatcher(jsonPath("$.uniParcId", Matchers.is(UNIPARC_ID)))
                    .resultMatcher(jsonPath("$.uniParcCrossReferences.*.organism").exists())
                    .resultMatcher(jsonPath("$.sequence").doesNotExist())
                    .resultMatcher(jsonPath("$.sequenceFeatures").doesNotExist())
                    .resultMatcher(jsonPath("$.sequenceFeatures").doesNotExist())
                    .resultMatcher(jsonPath("$.oldestCrossRefCreated").exists())
                    .resultMatcher(jsonPath("$.mostRecentCrossRefUpdated").exists())
                    .build();
        }

        @Override
        protected GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    Matchers.contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class UniParcGetByAccessionContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        protected GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ACCESSION)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.uniParcCrossReferences[*].id",
                                                    hasItem(ACCESSION)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(content().string(containsString(UNIPARC_ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(Matchers.containsString(UNIPARC_ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(Matchers.containsString(UNIPARC_ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            Matchers.containsString(
                                                                    "Entry\tOrganisms\tUniProtKB\tFirst seen\tLast seen\tLength")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            Matchers.containsString(
                                                                    "UPI0000083D01\tName 7787; Name 9606\tP10001; P12301\t2017-02-12\t2017-04-23\t11")))
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
                                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .build();
        }
    }
}

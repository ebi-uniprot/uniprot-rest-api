package org.uniprot.api.support.data.subcellular.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.subcellular.repository.SubcellularLocationRepository;
import org.uniprot.core.cv.subcell.SubcellLocationCategory;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.cv.subcell.impl.SubcellularLocationEntryBuilder;
import org.uniprot.core.json.parser.subcell.SubcellularLocationJsonConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(SubcellularLocationController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            SubcellularLocationGetIdControllerIT.SubcellularLocationGetIdParameterResolver.class,
            SubcellularLocationGetIdControllerIT.SubcellularLocationGetIdContentTypeParamResolver
                    .class
        })
public class SubcellularLocationGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final String SUBCELL_ACCESSION = "SL-0005";

    @Autowired private SubcellularLocationRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.SUBCELLULAR_LOCATION;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.subcellularlocation;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {
        SubcellularLocationEntry subcellularLocationEntry =
                new SubcellularLocationEntryBuilder()
                        .name("subcell name")
                        .id(SUBCELL_ACCESSION)
                        .category(SubcellLocationCategory.LOCATION)
                        .definition("Definition value")
                        .build();

        SubcellularLocationDocument document =
                SubcellularLocationDocument.builder()
                        .id(SUBCELL_ACCESSION)
                        .subcellularlocationObj(
                                getSubcellularLocationBinary(subcellularLocationEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.SUBCELLULAR_LOCATION, document);
    }

    @Override
    protected String getIdRequestPath() {
        return "/subcellularlocation/";
    }

    private ByteBuffer getSubcellularLocationBinary(SubcellularLocationEntry entry) {
        try {
            return ByteBuffer.wrap(
                    SubcellularLocationJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Unable to parse SubcellularLocationEntry to binary json: ", e);
        }
    }

    static class SubcellularLocationGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(SUBCELL_ACCESSION)
                    .resultMatcher(jsonPath("$.id", is(SUBCELL_ACCESSION)))
                    .resultMatcher(jsonPath("$.name", is("subcell name")))
                    .resultMatcher(jsonPath("$.definition", is("Definition value")))
                    .resultMatcher(jsonPath("$.category", is("Cellular component")))
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
                                            "The subcellular location id value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("SL-0000")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(SUBCELL_ACCESSION)
                    .fields("name,definition,category")
                    .resultMatcher(jsonPath("$.name", is("subcell name")))
                    .resultMatcher(jsonPath("$.definition", is("Definition value")))
                    .resultMatcher(jsonPath("$.category", is("Cellular component")))
                    .resultMatcher(jsonPath("$.id").exists()) // required
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(SUBCELL_ACCESSION)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class SubcellularLocationGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(SUBCELL_ACCESSION)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.id", is(SUBCELL_ACCESSION)))
                                    .resultMatcher(jsonPath("$.name", is("subcell name")))
                                    .resultMatcher(jsonPath("$.definition", is("Definition value")))
                                    .resultMatcher(jsonPath("$.category", is("Cellular component")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(containsString(SUBCELL_ACCESSION)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Subcellular location ID\tDescription\tCategory\tName")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "SL-0005\tDefinition value\tCellular component\tsubcell name")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.OBO_MEDIA_TYPE))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "format-version: 1.2\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "default-namespace: uniprot:locations\n")))
                                    .resultMatcher(content().string(containsString("[Term]\n")))
                                    .resultMatcher(
                                            content().string(containsString("id: SL-0005\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(containsString("name: subcell name\n")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "def: \"Definition value\" []\n")))
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
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The subcellular location id value has invalid format. It should match the regular expression 'SL-[0-9]{4}'")))
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
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}

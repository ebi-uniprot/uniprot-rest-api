package org.uniprot.api.disease;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Collections;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractDownloadControllerIT;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadParameterResolver;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            DiseaseDownloadControllerIT.DiseaseDownloadParameterResolver.class
        })
public class DiseaseDownloadControllerIT extends AbstractDownloadControllerIT {
    @Autowired private DiseaseRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.DISEASE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.disease;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getDownloadRequestPath() {
        return "/disease/download";
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        DiseaseSolrDocumentHelper.createDiseaseDocuments(this.getStoreManager(), numberOfEntries);
    }

    static class DiseaseDownloadParameterResolver extends AbstractDownloadParameterResolver {
        @Override
        protected SearchParameter downloadAllParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*"))
                    .resultMatcher(jsonPath("$.results.length()", is(500)))
                    .build();
        }

        @Override
        protected SearchParameter downloadLessThanDefaultBatchSizeParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*"))
                    .queryParam(
                            "size",
                            Collections.singletonList(
                                    String.valueOf(
                                            BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.length()",
                                    is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40)))
                    .build();
        }

        @Override
        protected SearchParameter downloadDefaultBatchSizeParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*"))
                    .queryParam(
                            "size",
                            Collections.singletonList(
                                    String.valueOf(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.length()",
                                    is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE)))
                    .build();
        }

        @Override
        protected SearchParameter downloadMoreThanBatchSizeParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*"))
                    .queryParam(
                            "size",
                            Collections.singletonList(
                                    String.valueOf(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.length()",
                                    is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3)))
                    .build();
        }

        @Override
        protected SearchParameter downloadSizeLessThanZeroParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*"))
                    .queryParam("size", Collections.singletonList(String.valueOf(-1)))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath("$.messages.*", contains("'size' must be greater than 0")))
                    .build();
        }

        @Override
        protected SearchParameter downloadWithoutQueryParameter() {
            return SearchParameter.builder()
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath("$.messages.*", contains("'query' is a required parameter")))
                    .build();
        }

        @Override
        protected SearchParameter downloadWithBadQueryParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("random_field:protein"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("'random_field' is not a valid search field")))
                    .build();
        }
    }
}

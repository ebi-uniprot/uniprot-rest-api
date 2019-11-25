package org.uniprot.api.disease.download;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.disease.DiseaseController;
import org.uniprot.api.disease.DiseaseRepository;
import org.uniprot.api.disease.DiseaseSolrDocumentHelper;
import org.uniprot.api.rest.controller.AbstractDownloadControllerIT;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            DiseaseDownloadParameterResolver.class,
            DiseaseSearchContentTypeParamResolver.class
        })
public class DiseaseDownloadControllerIT extends AbstractDownloadControllerIT {
    @Autowired private DiseaseRepository repository;

    @Test
    @Override
    protected void testDownloadAll(SearchParameter queryParameter) throws Exception {
        super.testDownloadAll(queryParameter);
    }

    @Test
    @Override
    protected void testDownloadLessThanDefaultBatchSize(SearchParameter queryParameter)
            throws Exception {
        super.testDownloadLessThanDefaultBatchSize(queryParameter);
    }

    @Test
    @Override
    protected void testDownloadDefaultBatchSize(SearchParameter queryParameter) throws Exception {
        super.testDownloadDefaultBatchSize(queryParameter);
    }

    @Test
    @Override
    protected void testDownloadMoreThanBatchSize(SearchParameter queryParameter) throws Exception {
        super.testDownloadMoreThanBatchSize(queryParameter);
    }

    @Test
    @Override
    protected void testDownloadSizeLessThanZero(SearchParameter queryParameter) throws Exception {
        super.testDownloadSizeLessThanZero(queryParameter);
    }

    @Test
    @Override
    protected void testDownloadWithoutQuery(SearchParameter queryParameter) throws Exception {
        super.testDownloadWithoutQuery(queryParameter);
    }

    @Test
    @Override
    protected void testDownloadWithBadQuery(SearchParameter queryParameter) throws Exception {
        super.testDownloadWithBadQuery(queryParameter);
    }

    @Test
    @Override
    protected void searchSuccessContentTypes(SearchContentTypeParam contentTypeParam)
            throws Exception {
        super.searchSuccessContentTypes(contentTypeParam);
    }

    @Test
    @Override
    protected void searchBadRequestContentTypes(SearchContentTypeParam contentTypeParam)
            throws Exception {
        super.searchBadRequestContentTypes(contentTypeParam);
    }

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

    @Override
    protected void saveEntry(String accession, long suffix) {
        DiseaseSolrDocumentHelper.createDiseaseDocuments(this.getStoreManager(), accession, suffix);
    }
}

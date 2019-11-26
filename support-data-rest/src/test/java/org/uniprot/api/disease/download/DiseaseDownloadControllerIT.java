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
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
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
    protected void testDownloadAllJson(DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadAllJson(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadAllTSV(DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadAllTSV(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadAllList(DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadAllList(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadAllOBO(DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadAllOBO(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadAllXLS(DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadAllXLS(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadLessThanDefaultBatchSize(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadLessThanDefaultBatchSize(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadDefaultBatchSize(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadDefaultBatchSize(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadMoreThanBatchSize(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadMoreThanBatchSize(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadSizeLessThanZero(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadSizeLessThanZero(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadWithoutQuery(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithoutQuery(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadWithBadQuery(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithBadQuery(parameterAndResult);
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

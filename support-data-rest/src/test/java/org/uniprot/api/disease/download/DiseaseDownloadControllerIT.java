package org.uniprot.api.disease.download;

import org.junit.jupiter.api.Disabled;
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
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class, DiseaseDownloadParameterResolver.class})
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
    protected void testDownloadLessThanDefaultBatchSizeJson(
            DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadLessThanDefaultBatchSizeJson(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadLessThanDefaultBatchSizeTSV(
            DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadLessThanDefaultBatchSizeTSV(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadLessThanDefaultBatchSizeList(
            DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadLessThanDefaultBatchSizeList(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadLessThanDefaultBatchSizeOBO(
            DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadLessThanDefaultBatchSizeOBO(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadLessThanDefaultBatchSizeXLS(
            DownloadParamAndResult parameterAndResult) throws Exception {
        super.testDownloadLessThanDefaultBatchSizeXLS(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadDefaultBatchSizeJson(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadDefaultBatchSizeJson(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadMoreThanBatchSizeJson(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadMoreThanBatchSizeJson(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadSizeLessThanZeroJson(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadSizeLessThanZeroJson(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadWithoutQueryJson(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithoutQueryJson(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadWithoutQueryList(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithoutQueryList(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadWithoutQueryTSV(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithoutQueryTSV(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadWithoutQueryOBO(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithoutQueryOBO(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadWithoutQueryXLS(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithoutQueryXLS(parameterAndResult);
    }

    @Test
    @Override
    protected void testDownloadWithBadQueryJson(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithBadQueryJson(parameterAndResult);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    @Override
    protected void testDownloadWithBadQueryList(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithBadQueryList(parameterAndResult);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    @Override
    protected void testDownloadWithBadQueryTSV(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithBadQueryTSV(parameterAndResult);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    @Override
    protected void testDownloadWithBadQueryOBO(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithBadQueryOBO(parameterAndResult);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    @Override
    protected void testDownloadWithBadQueryXLS(DownloadParamAndResult parameterAndResult)
            throws Exception {
        super.testDownloadWithBadQueryXLS(parameterAndResult);
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

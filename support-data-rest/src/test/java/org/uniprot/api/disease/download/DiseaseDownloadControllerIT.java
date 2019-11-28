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
    protected void testDownloadAllJSON(DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadAll(parameterAndResult);
    }

    @Test
    protected void testDownloadAllTSV(DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadAll(parameterAndResult);
    }

    @Test
    protected void testDownloadAllList(DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadAll(parameterAndResult);
    }

    @Test
    protected void testDownloadAllOBO(DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadAll(parameterAndResult);
    }

    @Test
    protected void testDownloadAllXLS(DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadAll(parameterAndResult);
    }

    @Test
    protected void testDownloadLessThanDefaultBatchSizeJSON(
            DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadLessThanDefaultBatchSize(parameterAndResult);
    }

    @Test
    protected void testDownloadLessThanDefaultBatchSizeTSV(
            DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadLessThanDefaultBatchSize(parameterAndResult);
    }

    @Test
    protected void testDownloadLessThanDefaultBatchSizeList(
            DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadLessThanDefaultBatchSize(parameterAndResult);
    }

    @Test
    protected void testDownloadLessThanDefaultBatchSizeOBO(
            DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadLessThanDefaultBatchSize(parameterAndResult);
    }

    @Test
    protected void testDownloadLessThanDefaultBatchSizeXLS(
            DownloadParamAndResult parameterAndResult) throws Exception {
        testDownloadLessThanDefaultBatchSize(parameterAndResult);
    }

    @Test
    protected void testDownloadDefaultBatchSizeJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadDefaultBatchSize(parameterAndResult);
    }

    @Test
    protected void testDownloadMoreThanBatchSizeJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadMoreThanBatchSize(parameterAndResult);
    }

    @Test
    protected void testDownloadWithSortJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithSort(parameterAndResult);
    }

    @Test
    protected void testDownloadWithSortList(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithSort(parameterAndResult);
    }

    @Test
    protected void testDownloadWithSortTSV(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithSort(parameterAndResult);
    }

    @Test
    protected void testDownloadWithSortXLS(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithSort(parameterAndResult);
    }

    @Test
    protected void testDownloadWithSortOBO(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithSort(parameterAndResult);
    }

    @Test
    protected void testDownloadNonDefaultFieldsJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadNonDefaultFields(parameterAndResult);
    }


    @Test
    protected void testDownloadInvalidFieldsJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadInvalidFields(parameterAndResult);
    }

    @Test
    protected void testDownloadByAccessionJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadByAccession(parameterAndResult);
    }

    @Test
    protected void testDownloadSizeLessThanZeroJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadSizeLessThanZero(parameterAndResult);
    }

    @Test
    protected void testDownloadWithoutQueryJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithoutQuery(parameterAndResult);
    }

    @Test
    protected void testDownloadWithoutQueryList(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithoutQuery(parameterAndResult);
    }

    @Test
    protected void testDownloadWithoutQueryTSV(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithoutQuery(parameterAndResult);
    }

    @Test
    protected void testDownloadWithoutQueryOBO(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithoutQuery(parameterAndResult);
    }

    @Test
    protected void testDownloadWithoutQueryXLS(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithoutQuery(parameterAndResult);
    }

    @Test
    protected void testDownloadWithBadQueryJSON(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithBadQuery(parameterAndResult);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    protected void testDownloadWithBadQueryList(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithBadQuery(parameterAndResult);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    protected void testDownloadWithBadQueryTSV(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithBadQuery(parameterAndResult);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    protected void testDownloadWithBadQueryOBO(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithBadQuery(parameterAndResult);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    protected void testDownloadWithBadQueryXLS(DownloadParamAndResult parameterAndResult)
            throws Exception {
        testDownloadWithBadQuery(parameterAndResult);
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

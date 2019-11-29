package org.uniprot.api.disease.download.IT;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.disease.DiseaseController;
import org.uniprot.api.disease.download.resolver.DiseaseDownloadQueryParamResolver;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.support_data.SupportDataApplication;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class, DiseaseDownloadQueryParamResolver.class})
public class DiseaseDownloadQueryIT extends BaseDiseaseDownloadIT {
    @Test
    protected void testDownloadByAccessionJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        // clear the collection
        cleanData();
        // when
        saveEntry(ACC1, 1);
        saveEntry(ACC2, 2);
        saveEntry(ACC3, 3);
        // then
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @Test
    protected void testDownloadWithoutQueryJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Test
    protected void testDownloadWithoutQueryList(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Test
    protected void testDownloadWithoutQueryTSV(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Test
    protected void testDownloadWithoutQueryOBO(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Test
    protected void testDownloadWithoutQueryXLS(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Test
    protected void testDownloadWithBadQueryJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    protected void testDownloadWithBadQueryList(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    protected void testDownloadWithBadQueryTSV(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    protected void testDownloadWithBadQueryOBO(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @Disabled // see https://www.ebi.ac.uk/panda/jira/browse/TRM-23243
    @Test
    protected void testDownloadWithBadQueryXLS(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }
}

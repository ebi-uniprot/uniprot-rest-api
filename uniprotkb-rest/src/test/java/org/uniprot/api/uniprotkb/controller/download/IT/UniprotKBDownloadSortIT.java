// package org.uniprot.api.uniprotkb.controller.download.IT;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.http.HttpStatus;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit.jupiter.SpringExtension;
// import org.uniprot.api.DataStoreTestConfig;
// import org.uniprot.api.disease.DiseaseController;
// import org.uniprot.api.disease.download.resolver.DiseaseDownloadSortParamResolver;
// import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
// import org.uniprot.api.support_data.SupportDataApplication;
//
// @ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
// @ActiveProfiles(profiles = "offline")
// @WebMvcTest(DiseaseController.class)
// @ExtendWith(value = {SpringExtension.class, DiseaseDownloadSortParamResolver.class})
// public class UniprotKBDownloadSortIT extends BaseUniprotKBDownloadIT {
//
//    @BeforeEach
//    public void setUpData() {
//        // when
//        saveEntry(ACC1, 1);
//        saveEntry(ACC2, 2);
//        saveEntry(ACC3, 3);
//    }
//
//    @Test
//    protected void testDownloadWithSortJSON(DownloadParamAndResult parameterAndResult)
//            throws Exception {
//        // then
//        sendAndVerify(parameterAndResult, HttpStatus.OK);
//    }
//
//    @Test
//    protected void testDownloadWithSortList(DownloadParamAndResult parameterAndResult)
//            throws Exception {
//        // then
//        sendAndVerify(parameterAndResult, HttpStatus.OK);
//    }
//
//    @Test
//    protected void testDownloadWithSortTSV(DownloadParamAndResult parameterAndResult)
//            throws Exception {
//        // then
//        sendAndVerify(parameterAndResult, HttpStatus.OK);
//    }
//
//    @Test
//    protected void testDownloadWithSortXLS(DownloadParamAndResult parameterAndResult)
//            throws Exception {
//        // then
//        sendAndVerify(parameterAndResult, HttpStatus.OK);
//    }
//
//    @Test
//    protected void testDownloadWithSortOBO(DownloadParamAndResult parameterAndResult)
//            throws Exception {
//        // then
//        sendAndVerify(parameterAndResult, HttpStatus.OK);
//    }
// }

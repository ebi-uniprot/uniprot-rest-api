package uk.ac.ebi.uniprot.crossref.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.crossref.controller.CrossRefController;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntryBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.search.document.dbxref.CrossRefDocument;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(CrossRefController.class)
@ExtendWith(value = {SpringExtension.class, CrossRefGetIdControllerIT.CrossRefGetIdParameterResolver.class,
        CrossRefGetIdControllerIT.CrossRefGetIdContentTypeParamResolver.class})
public class CrossRefGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final String ACCESSION = "DB-0104";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Override
    public MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    public String getIdRequestPath() {
        return "/xref/";
    }

    @Override
    public void saveEntry() {

        CrossRefEntryBuilder entryBuilder = new CrossRefEntryBuilder();
        CrossRefEntry crossRefEntry = entryBuilder.accession(ACCESSION)
                .abbrev("TIGRFAMs")
                .name("TIGRFAMs; a protein family database")
                .pubMedId("17151080")
                .doiId("10.1093/nar/gkl1043")
                .linkType("Explicit")
                .server("http://tigrfams.jcvi.org/cgi-bin/index.cgi")
                .dbUrl("http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")
                .category("Family and domain databases")
                .reviewedProteinCount(10L)
                .unreviewedProteinCount(5L)
                .build();

        CrossRefDocument document = CrossRefDocument.builder()
                .accession(crossRefEntry.getAccession())
                .abbrev(crossRefEntry.getAbbrev())
                .name(crossRefEntry.getName())
                .pubMedId(crossRefEntry.getPubMedId())
                .doiId(crossRefEntry.getDoiId())
                .linkType(crossRefEntry.getLinkType())
                .server(crossRefEntry.getServer())
                .dbUrl(crossRefEntry.getDbUrl())
                .category(crossRefEntry.getCategory())
                .reviewedProteinCount(crossRefEntry.getReviewedProteinCount())
                .unreviewedProteinCount(crossRefEntry.getUnreviewedProteinCount())
                .build();

        this.storeManager.saveDocs(DataStoreManager.StoreType.CROSSREF, document);
    }


    static class CrossRefGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder().id(ACCESSION)
                    .resultMatcher(jsonPath("$.link_type", is("Explicit")))
                    .resultMatcher(jsonPath("$.server", is("http://tigrfams.jcvi.org/cgi-bin/index.cgi")))
                    .resultMatcher(jsonPath("$.unreviewed_protein_count", is(5)))
                    .resultMatcher(jsonPath("$.name", is("TIGRFAMs; a protein family database")))
                    .resultMatcher(jsonPath("$.db_url", is("http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")))
                    .resultMatcher(jsonPath("$.pub_med_id", is("17151080")))
                    .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.abbrev", is("TIGRFAMs")))
                    .resultMatcher(jsonPath("$.reviewed_protein_count", is(10)))
                    .resultMatcher(jsonPath("$.category", is("Family and domain databases")))
                    .resultMatcher(jsonPath("$.doi_id", is("10.1093/nar/gkl1043")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder().id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("The cross ref id value should be in the form of DB-XXXX")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder().id("DB-0000")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder().id(ACCESSION).fields("accession,category,unreviewed_protein_count")
                    .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.category", is("Family and domain databases")))
                    .resultMatcher(jsonPath("$.unreviewed_protein_count", is(5)))
                    .resultMatcher(jsonPath("$.name").doesNotExist())
                    .resultMatcher(jsonPath("$.reviewed_protein_count").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder().id(ACCESSION).fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class CrossRefGetIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ACCESSION)
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.link_type", is("Explicit")))
                            .resultMatcher(jsonPath("$.server", is("http://tigrfams.jcvi.org/cgi-bin/index.cgi")))
                            .resultMatcher(jsonPath("$.unreviewed_protein_count", is(5)))
                            .resultMatcher(jsonPath("$.name", is("TIGRFAMs; a protein family database")))
                            .resultMatcher(jsonPath("$.db_url", is("http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")))
                            .resultMatcher(jsonPath("$.pub_med_id", is("17151080")))
                            .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                            .resultMatcher(jsonPath("$.abbrev", is("TIGRFAMs")))
                            .resultMatcher(jsonPath("$.reviewed_protein_count", is(10)))
                            .resultMatcher(jsonPath("$.category", is("Family and domain databases")))
                            .resultMatcher(jsonPath("$.doi_id", is("10.1093/nar/gkl1043")))
                            .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                            .resultMatcher(jsonPath("$.messages.*", contains("The cross ref id value should be in the form of DB-XXXX")))
                            .build())
                    .build();
        }
    }
}

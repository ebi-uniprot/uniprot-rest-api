package org.uniprot.api.support.data.crossref.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdWithTypeExtensionControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.crossref.repository.CrossRefRepository;
import org.uniprot.core.Statistics;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.core.cv.xdb.impl.CrossRefEntryBuilder;
import org.uniprot.core.impl.StatisticsBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(CrossRefController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            CrossRefGetIdControllerIT.CrossRefGetIdParameterResolver.class,
            CrossRefGetIdControllerIT.CrossRefGetIdContentTypeParamResolver.class
        })
public class CrossRefGetIdControllerIT extends AbstractGetByIdWithTypeExtensionControllerIT {

    private static final String ACCESSION = "DB-0104";

    @Autowired private CrossRefRepository repository;

    @Autowired
    @Qualifier("xrefRDFRestTemplate")
    private RestTemplate restTemplate;

    @Override
    protected String getIdRequestPath() {
        return "/database/{id}";
    }

    @Override
    protected String getIdRequestPathWithoutPathVariable() {
        return "/database/";
    }

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.CROSSREF;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.crossref;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {

        Statistics statistics =
                new StatisticsBuilder()
                        .reviewedProteinCount(10L)
                        .unreviewedProteinCount(5L)
                        .build();

        CrossRefEntryBuilder entryBuilder = new CrossRefEntryBuilder();
        CrossRefEntry crossRefEntry =
                entryBuilder
                        .id(ACCESSION)
                        .abbrev("TIGRFAMs")
                        .name("TIGRFAMs; a protein family database")
                        .pubMedId("17151080")
                        .doiId("10.1093/nar/gkl1043")
                        .linkType("Explicit")
                        .server("http://tigrfams.jcvi.org/cgi-bin/index.cgi")
                        .dbUrl("http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")
                        .category("Family and domain databases")
                        .statistics(statistics)
                        .build();

        CrossRefDocument document =
                CrossRefDocument.builder()
                        .id(crossRefEntry.getId())
                        .abbrev(crossRefEntry.getAbbrev())
                        .name(crossRefEntry.getName())
                        .pubMedId(crossRefEntry.getPubMedId())
                        .doiId(crossRefEntry.getDoiId())
                        .linkType(crossRefEntry.getLinkType())
                        .server(crossRefEntry.getServer())
                        .dbUrl(crossRefEntry.getDbUrl())
                        .category(crossRefEntry.getCategory())
                        .reviewedProteinCount(statistics.getReviewedProteinCount())
                        .unreviewedProteinCount(statistics.getUnreviewedProteinCount())
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.CROSSREF, document);
    }

    @Override
    protected RestTemplate getRestTemple() {
        return restTemplate;
    }

    @Override
    protected String getSearchAccession() {
        return ACCESSION;
    }

    @Override
    protected String getRDFProlog() {
        return RDFPrologs.XREF_PROLOG;
    }

    static class CrossRefGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .resultMatcher(jsonPath("$.linkType", is("Explicit")))
                    .resultMatcher(
                            jsonPath("$.server", is("http://tigrfams.jcvi.org/cgi-bin/index.cgi")))
                    .resultMatcher(jsonPath("$.statistics.unreviewedProteinCount", is(5)))
                    .resultMatcher(jsonPath("$.name", is("TIGRFAMs; a protein family database")))
                    .resultMatcher(
                            jsonPath(
                                    "$.dbUrl",
                                    is(
                                            "http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")))
                    .resultMatcher(jsonPath("$.pubMedId", is("17151080")))
                    .resultMatcher(jsonPath("$.id", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.abbrev", is("TIGRFAMs")))
                    .resultMatcher(jsonPath("$.statistics.reviewedProteinCount", is(10)))
                    .resultMatcher(jsonPath("$.category", is("Family and domain databases")))
                    .resultMatcher(jsonPath("$.doiId", is("10.1093/nar/gkl1043")))
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
                                            "The cross ref id value should be in the form of DB-XXXX")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("DB-0000")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("id,category,unreviewed_protein_count")
                    .resultMatcher(jsonPath("$.id", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.category", is("Family and domain databases")))
                    .resultMatcher(jsonPath("$.statistics.unreviewedProteinCount", is(5)))
                    .resultMatcher(jsonPath("$.name").doesNotExist())
                    .resultMatcher(jsonPath("$.statistics.reviewedProteinCount").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class CrossRefGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ACCESSION)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.linkType", is("Explicit")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.server",
                                                    is(
                                                            "http://tigrfams.jcvi.org/cgi-bin/index.cgi")))
                                    .resultMatcher(
                                            jsonPath("$.statistics.unreviewedProteinCount", is(5)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.name",
                                                    is("TIGRFAMs; a protein family database")))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.dbUrl",
                                                    is(
                                                            "http://tigrfams.jcvi.org/cgi-bin/HmmReportPage.cgi?acc=%s")))
                                    .resultMatcher(jsonPath("$.pubMedId", is("17151080")))
                                    .resultMatcher(jsonPath("$.id", is(ACCESSION)))
                                    .resultMatcher(jsonPath("$.abbrev", is("TIGRFAMs")))
                                    .resultMatcher(
                                            jsonPath("$.statistics.reviewedProteinCount", is(10)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.category",
                                                    is("Family and domain databases")))
                                    .resultMatcher(jsonPath("$.doiId", is("10.1093/nar/gkl1043")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.RDF_MEDIA_TYPE))
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
                                                            "The cross ref id value should be in the form of DB-XXXX")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The cross ref id value should be in the form of DB-XXXX")))
                                    .build())
                    .build();
        }
    }
}

package org.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.impl.JournalArticleBuilder;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.*;
import org.uniprot.core.proteome.impl.*;
import org.uniprot.core.proteome.impl.ComponentBuilder;
import org.uniprot.core.proteome.impl.ProteomeEntryBuilder;
import org.uniprot.core.proteome.impl.ProteomeIdBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author jluo
 * @date: 13 Jun 2019
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            ProteomeRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ProteomeController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            ProteomeSearchControllerIT.ProteomeSearchContentTypeParamResolver.class,
            ProteomeSearchControllerIT.ProteomeSearchParameterResolver.class
        })
public class ProteomeSearchControllerIT extends AbstractSearchWithFacetControllerIT {
    private static final String UPID_PREF = "UP000005";

    @Autowired private ProteomeQueryRepository repository;

    @Autowired private ProteomeFacetConfig facetConfig;

    @Value("${search.default.page.size}")
    protected String defaultPageSize;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.PROTEOME;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.proteome;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/proteome/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return Integer.parseInt(defaultPageSize);
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.PROTEOME;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "upid":
                value = UPID_PREF + 231;
                break;
            case "organism_id":
            case "taxonomy_id":
                value = "9606";
                break;

            case "organism_name":
                value = "human";
                break;
            case "annotation_score":
                value = "3";
                break;
            case "proteome_type":
                value = "1";
                break;
        }
        return value;
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(231);
        saveEntry(520);
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    private void saveEntry(int i) {
        ProteomeEntry entry = createEntry(i);
        ProteomeDocument document = new ProteomeDocument();
        document.upid = entry.getId().getValue();
        document.organismName.add("Homo sapiens");
        document.organismName.add("human");
        document.organismTaxId = 9606;
        document.content.add(document.upid);
        document.content.addAll(document.organismName);
        document.content.add(entry.getDescription());
        document.proteomeStored = getBinary(entry);
        document.isRedundant = i % 2 != 0;
        document.isReferenceProteome = i % 2 != 0;
        document.isExcluded = i % 2 != 0;
        document.proteomeType = 1;
        document.score = 3;
        document.organismTaxon = document.organismName;
        document.taxLineageIds.add(9606);
        document.organismSort = "human";
        document.superkingdom = "eukaryota";
        document.genomeAccession.add("someAcc");
        document.genomeAssembly.add("someAcc");

        getStoreManager().saveDocs(DataStoreManager.StoreType.PROTEOME, document);
    }

    private ByteBuffer getBinary(ProteomeEntry entry) {
        try {
            return ByteBuffer.wrap(
                    ProteomeJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    private String getName(String prefix, int i) {
        if (i < 10) {
            return prefix + "00" + i;
        } else if (i < 100) {
            return prefix + "0" + i;
        } else return prefix + i;
    }

    private ProteomeEntry createEntry(int i) {
        ProteomeId proteomeId = new ProteomeIdBuilder(getName(UPID_PREF, i)).build();
        String description = getName("Description", i);
        Taxonomy taxonomy =
                new TaxonomyBuilder()
                        .taxonId(9606)
                        .scientificName("Homo sapiens")
                        .mnemonic("HUMAN")
                        .build();
        LocalDate modified = LocalDate.of(2015, 11, 5);
        //	String reId = "UP000005641";
        //	ProteomeId redId = new ProteomeIdBuilder (reId).build();
        List<CrossReference<ProteomeDatabase>> xrefs = new ArrayList<>();
        CrossReference<ProteomeDatabase> xref1 =
                new CrossReferenceBuilder<ProteomeDatabase>()
                        .database(ProteomeDatabase.GENOME_ASSEMBLY)
                        .id(getName("ACA", i))
                        .build();
        xrefs.add(xref1);
        List<Component> components = new ArrayList<>();
        Component component1 =
                new ComponentBuilder()
                        .name("someName1")
                        .description("some description")
                        .type(org.uniprot.core.proteome.ComponentType.UNPLACED)
                        .build();

        Component component2 =
                new ComponentBuilder()
                        .name("someName2")
                        .description("some description 2")
                        .type(org.uniprot.core.proteome.ComponentType.SEGMENTED_GENOME)
                        .build();

        components.add(component1);
        components.add(component2);

        BuscoReport buscoReport =
                new BuscoReportBuilder()
                        .total(100)
                        .fragmented(110)
                        .missing(120)
                        .complete(130)
                        .completeSingle(140)
                        .completeDuplicated(150)
                        .lineageDb("lineage value")
                        .build();

        CPDReport cpdReport =
                new CPDReportBuilder()
                        .averageCdss(100)
                        .confidence(110)
                        .proteomeCount(120)
                        .status(CPDStatus.CLOSE_TO_STANDARD)
                        .stdCdss(12.3d)
                        .build();

        ProteomeCompletenessReport completenessReport =
                new ProteomeCompletenessReportBuilder()
                        .buscoReport(buscoReport)
                        .cpdReport(cpdReport)
                        .build();

        GenomeAssembly genomeAssembly =
                new GenomeAssemblyBuilder()
                        .assemblyId("assembly id")
                        .genomeAssemblyUrl("assembly url")
                        .source(GenomeAssemblySource.ENSEMBL)
                        .level(GenomeAssemblyLevel.PARTIAL)
                        .build();

        ProteomeEntryBuilder builder =
                new ProteomeEntryBuilder()
                        .proteomeId(proteomeId)
                        .description(description)
                        .taxonomy(taxonomy)
                        .modified(modified)
                        .proteomeType(ProteomeType.NORMAL)
                        .redundantTo(new ProteomeIdBuilder("UP000000001").build())
                        .strain("strain value")
                        .isolate("isolate value")
                        .citationsAdd(
                                new JournalArticleBuilder()
                                        .title("citation title")
                                        .journalName("journalName value")
                                        .build())
                        .proteomeCrossReferencesSet(xrefs)
                        .redundantProteomesAdd(
                                new RedundantProteomeBuilder().proteomeId("UP0000000002").build())
                        .panproteome(new ProteomeIdBuilder("UP000000003").build())
                        .componentsSet(components)
                        .taxonLineagesAdd(new TaxonomyLineageBuilder().taxonId(10L).build())
                        .superkingdom(Superkingdom.EUKARYOTA)
                        .sourceDb("sourceDb value")
                        .canonicalProteinsAdd(
                                new CanonicalProteinBuilder()
                                        .canonicalProtein(
                                                new ProteinBuilder()
                                                        .accession("P00001")
                                                        .geneNameType(GeneNameType.GENE_NAME)
                                                        .build())
                                        .build())
                        .genomeAssembly(genomeAssembly)
                        .proteomeCompletenessReport(completenessReport)
                        .exclusionReasonsAdd(ExclusionReason.MIXED_CULTURE)
                        .annotationScore(15);

        return builder.build();
    }

    static class ProteomeSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upid:UP000005231"))
                    .resultMatcher(jsonPath("$.results.*.id", contains("UP000005231")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upid:UP000004231"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upid:*"))
                    .resultMatcher(
                            jsonPath("$.results.*.id", contains("UP000005231", "UP000005520")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("organism_name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'organism_name' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList(
                                    "upid:INVALID OR organism_id:INVALID "
                                            + "OR organism_name:INVALID OR taxonomy_id:invalid OR superkingdom:invalid"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID",
                                            "The organism id filter value should be a number",
                                            "The taxonomy id filter value should be a number")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("annotation_score desc"))
                    .resultMatcher(
                            jsonPath("$.results.*.id", contains("UP000005231", "UP000005520")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("organism"))
                    .resultMatcher(
                            jsonPath("$.results.*.id", contains("UP000005231", "UP000005520")))
                    .resultMatcher(jsonPath("$.results.*.taxonomy.taxonId", contains(9606, 9606)))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("reference"))
                    .resultMatcher(
                            jsonPath("$.results.*.id", contains("UP000005231", "UP000005520")))
                    .resultMatcher(jsonPath("$.facets", iterableWithSize(1)))
                    .resultMatcher(jsonPath("$.facets[0].values", iterableWithSize(2)))
                    .resultMatcher(jsonPath("$.facets[0].label", is("Status")))
                    .resultMatcher(jsonPath("$.facets[0].name", is("reference")))
                    .resultMatcher(jsonPath("$.facets[0].allowMultipleSelection", is(false)))
                    .resultMatcher(
                            jsonPath(
                                    "$.facets[0].values.*.label",
                                    contains("Other Proteomes", "Reference proteomes")))
                    .resultMatcher(
                            jsonPath("$.facets[0].values.*.value", contains("false", "true")))
                    .resultMatcher(jsonPath("$.facets[0].values.*.count", contains(1, 1)))
                    .build();
        }
    }

    static class ProteomeSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("organism_id:9606")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.id",
                                                    contains("UP000005231", "UP000005520")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(content().string(containsString("UP000005231")))
                                    .resultMatcher(content().string(containsString("UP000005520")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("UP000005231")))
                                    .resultMatcher(content().string(containsString("UP000005520")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Proteome Id\tOrganism\tOrganism Id\tProtein count")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UP000005231\tHomo sapiens\t9606\t0")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UP000005520\tHomo sapiens\t9606\t0")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("upid:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
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
                    .build();
        }
    }
}

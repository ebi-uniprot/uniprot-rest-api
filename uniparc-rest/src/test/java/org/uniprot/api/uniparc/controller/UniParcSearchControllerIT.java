package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.repository.store.UniParcStoreClient;
import org.uniprot.api.uniparc.repository.store.UniParcStreamConfig;
import org.uniprot.core.Location;
import org.uniprot.core.Property;
import org.uniprot.core.Sequence;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.uniparc.*;
import org.uniprot.core.uniparc.impl.*;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.field.UniParcResultFields;

import com.beust.jcommander.internal.Lists;

/**
 * @author jluo
 * @date: 25 Jun 2019
 */
@ContextConfiguration(
        classes = {
            UniParcStreamConfig.class,
            UniParcDataStoreTestConfig.class,
            UniParcRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcSearchControllerIT.UniParcSearchContentTypeParamResolver.class,
            UniParcSearchControllerIT.UniParcSearchParameterResolver.class
        })
public class UniParcSearchControllerIT extends AbstractSearchControllerIT {
    private static final String UPI_PREF = "UPI0000083A";

    @Autowired private UniParcQueryRepository repository;

    @Autowired private UniParcFacetConfig facetConfig;

    private UniParcStoreClient storeClient;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIPARC;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.uniparc;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/uniparc/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        switch (searchField) {
            case "upi":
                value = UPI_PREF + 11;
                break;
            case "taxonomy_id":
                value = "9606";
                break;
            case "length":
                value = "[* TO *]";
                break;
            case "accession":
            case "isoform":
                value = "P12345";
                break;
            case "upid":
                value = "UP000005640";
                break;
        }
        return value;
    }

    @Override
    protected SearchFieldConfig getSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected List<String> getAllReturnedFields() {
        return Lists.newArrayList(UniParcResultFields.INSTANCE.getAllFields().keySet());
    }

    @BeforeAll
    void initDataStore() {
        storeClient =
                new UniParcStoreClient(
                        VoldemortInMemoryUniParcEntryStore.getInstance("avro-uniparc"));
        getStoreManager().addStore(DataStoreManager.StoreType.UNIPARC, storeClient);

        getStoreManager()
                .addDocConverter(
                        DataStoreManager.StoreType.UNIPARC,
                        new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));
    }

    @AfterEach
    void cleanStoreClient() {
        storeClient.truncate();
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(11);
        saveEntry(20);
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
    }

    private void saveEntry(int i) {
        UniParcEntry entry = createEntry(i);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);

        getStoreManager().saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPARC, entry);
    }

    private String getName(String prefix, int i) {
        if (i < 10) {
            return prefix + "0" + i;
        } else return prefix + i;
    }

    private UniParcEntry createEntry(int i) {
        String seq = "MVSWGRFICLVVVTMATLSLARPSFSLVED";
        Sequence sequence = new SequenceBuilder(seq).build();
        List<UniParcCrossReference> xrefs = getXrefs(i);
        List<SequenceFeature> seqFeatures = getSeqFeatures(i);
        List<Taxonomy> taxonomies = getTaxonomies();
        return new UniParcEntryBuilder()
                .uniParcId(new UniParcIdBuilder(getName(UPI_PREF, i)).build())
                .uniParcCrossReferencesSet(xrefs)
                .sequence(sequence)
                .sequenceFeaturesSet(seqFeatures)
                .taxonomiesSet(taxonomies)
                .build();
    }

    private List<Taxonomy> getTaxonomies() {
        Taxonomy taxonomy =
                new TaxonomyBuilder().taxonId(9606).scientificName("Homo sapiens").build();
        Taxonomy taxonomy2 = new TaxonomyBuilder().taxonId(10090).scientificName("MOUSE").build();
        return Arrays.asList(taxonomy, taxonomy2);
    }

    private List<SequenceFeature> getSeqFeatures(int i) {
        List<Location> locations = Arrays.asList(new Location(12, 23), new Location(45, 89));
        InterProGroup domain =
                new InterProGroupBuilder()
                        .name(getName("Inter Pro Name", i))
                        .id(getName("IP0000", i))
                        .build();
        SequenceFeature sf =
                new SequenceFeatureBuilder()
                        .interproGroup(domain)
                        .signatureDbType(SignatureDbType.PFAM)
                        .signatureDbId(getName("SIG0000", i))
                        .locationsSet(locations)
                        .build();
        SequenceFeature sf3 =
                SequenceFeatureBuilder.from(sf).signatureDbType(SignatureDbType.PROSITE).build();
        return Arrays.asList(sf, sf3);
    }

    private List<UniParcCrossReference> getXrefs(int i) {
        List<Property> properties = new ArrayList<>();
        properties.add(
                new Property(
                        UniParcCrossReference.PROPERTY_PROTEIN_NAME, getName("proteinName", i)));
        properties.add(
                new Property(UniParcCrossReference.PROPERTY_GENE_NAME, getName("geneName", i)));
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder()
                        .versionI(3)
                        .database(UniParcDatabase.SWISSPROT)
                        .id("P12345")
                        .version(7)
                        .active(true)
                        .created(LocalDate.of(2017, 5, 17))
                        .lastUpdated(LocalDate.of(2017, 2, 27))
                        .propertiesSet(properties)
                        .build();

        List<Property> properties2 = new ArrayList<>();
        properties2.add(
                new Property(
                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                        getName("anotherProteinName", i)));
        properties2.add(new Property(UniParcCrossReference.PROPERTY_NCBI_TAXONOMY_ID, "9606"));
        properties2.add(new Property(UniParcCrossReference.PROPERTY_PROTEOME_ID, "UP000005640"));

        UniParcCrossReference xref2 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.TREMBL)
                        .id(getName("P123", i))
                        .version(7)
                        .active(true)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesSet(properties2)
                        .build();

        return Arrays.asList(xref, xref2);
    }

    static class UniParcSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upi:UPI0000083A11"))
                    .resultMatcher(jsonPath("$.results.*.uniParcId", contains("UPI0000083A11")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upi:UPI0000083B11"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("upi:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniParcId",
                                    contains("UPI0000083A11", "UPI0000083A20")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("taxonomy_name:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'taxonomy_name' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList(
                                    "upi:INVALID OR taxonomy_id:INVALID "
                                            + "OR length:INVALID OR upid:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI",
                                            "'length' filter type 'general' is invalid. Expected 'range' filter type",
                                            "The taxonomy id filter value should be a number",
                                            "The 'upid' value has invalid format. It should be a valid Proteome UPID")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("upi desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniParcId",
                                    contains("UPI0000083A20", "UPI0000083A11")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("organism"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniParcId",
                                    contains("UPI0000083A11", "UPI0000083A20")))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    //    .queryParam("facets", Collections.singletonList("reference"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.uniParcId.value",
                                    contains("UPI0000083A11", "UPI0000083A20")))
                    .build();
        }
    }

    static class UniParcSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("taxonomy_id:9606")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.results.*.uniParcId",
                                                    contains("UPI0000083A11", "UPI0000083A20")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content().string(containsString("UPI0000083A11")))
                                    .resultMatcher(
                                            content().string(containsString("UPI0000083A20")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(containsString("UPI0000083A11")))
                                    .resultMatcher(
                                            content().string(containsString("UPI0000083A20")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Entry\tOrganisms\tUniProtKB\tFirst seen\tLast seen\tLength")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UPI0000083A11	Homo sapiens; MOUSE	P12345; P12311	2017-02-12	2017-04-23	30")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UPI0000083A20	Homo sapiens; MOUSE	P12345; P12320	2017-02-12	2017-04-23	30")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE))
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
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE))
                                    .build())
                    .build();
        }
    }
}

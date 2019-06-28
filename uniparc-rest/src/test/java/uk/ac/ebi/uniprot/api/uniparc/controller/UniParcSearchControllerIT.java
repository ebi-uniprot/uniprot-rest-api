package uk.ac.ebi.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;

import uk.ac.ebi.uniprot.api.configure.uniparc.UniParcResultFields;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractSearchControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.SaveScenario;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import uk.ac.ebi.uniprot.api.uniparc.UniParcRestApplication;
import uk.ac.ebi.uniprot.api.uniparc.repository.UniParcFacetConfig;
import uk.ac.ebi.uniprot.domain.Location;
import uk.ac.ebi.uniprot.domain.Property;
import uk.ac.ebi.uniprot.domain.Sequence;
import uk.ac.ebi.uniprot.domain.builder.SequenceBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.InterproGroup;
import uk.ac.ebi.uniprot.domain.uniparc.SequenceFeature;
import uk.ac.ebi.uniprot.domain.uniparc.SignatureDbType;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcDBCrossReference;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcDatabaseType;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.domain.uniparc.builder.InterProGroupBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.builder.SequenceFeatureBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.builder.UniParcDBCrossReferenceBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.builder.UniParcEntryBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.builder.UniParcIdBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.taxonomy.Taxonomy;
import uk.ac.ebi.uniprot.domain.uniprot.taxonomy.builder.TaxonomyBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.uniparc.UniParcJsonConfig;
import uk.ac.ebi.uniprot.search.document.uniparc.UniParcDocument;
import uk.ac.ebi.uniprot.search.document.uniparc.UniParcDocument.UniParcDocumentBuilder;
import uk.ac.ebi.uniprot.search.field.SearchField;
import uk.ac.ebi.uniprot.search.field.UniParcField;

/**
 *
 * @author jluo
 * @date: 25 Jun 2019
 *
*/
@ContextConfiguration(classes= {UniParcDataStoreTestConfig.class, UniParcRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(value = {SpringExtension.class, UniParcSearchControllerIT.UniParcSearchContentTypeParamResolver.class,
		UniParcSearchControllerIT.UniParcSearchParameterResolver.class})
public class UniParcSearchControllerIT extends AbstractSearchControllerIT {
	 private static final String UPI_PREF = "UPI0000083A";
	    @Autowired
	    private MockMvc mockMvc;
	    @Autowired
	    SolrClient solrClient;
	    @Autowired
	    private DataStoreManager storeManager;
	    
	    @Autowired
	    private UniParcFacetConfig facetConfig;
	@Override
	protected void cleanEntries() {
		 storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);

	}

	@Override
	protected MockMvc getMockMvc() {
		return mockMvc;
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
	protected List<SearchField> getAllSearchFields() {
		 return Arrays.asList(UniParcField.Search.values());
	}

	
	@Override
	protected String getFieldValueForValidatedField(SearchField searchField) {
		 String value = "";
	        switch (searchField.getName()) {
	            case "upi":
	            	value =UPI_PREF +11;
	            	break;
	            case "taxonomy_id":
	            	value ="9606";
	            	break;
	            case "length":
	            	value ="30";
	            	break;
	            case "accession":
	            case "isoform":
	            	value="P12345";
	            	break;
	            case "upid":
	            	value="UP000005640";
	            	break;
	        }
	        return value;
	}

	@Override
	protected List<String> getAllSortFields() {
		 return Arrays.stream(UniParcField.Sort.values())
	                .map(UniParcField.Sort::name)
	                .collect(Collectors.toList());
	}

	@Override
	protected List<String> getAllFacetFields() {
		 return new ArrayList<>(facetConfig.getFacetNames());
	}

	@Override
	protected List<String> getAllReturnedFields() {
		return Lists.newArrayList(
		UniParcResultFields.INSTANCE.getAllFields()
		.keySet());
	}

	@Override
	protected void saveEntry(SaveScenario saveContext) {
		saveEntry(11);
		saveEntry(20);

	}

	@Override
	protected void saveEntries(int numberOfEntries) {
		IntStream.rangeClosed(1, numberOfEntries)
		.forEach(i-> saveEntry(i));
	}
	
	
	private void saveEntry(int i) {
		UniParcEntry entry = createEntry(i);
		UniParcDocumentBuilder builder = UniParcDocument.builder();
		builder.upi(entry.getUniParcId().getValue()).seqLength(entry.getSequence().getLength())
				.sequenceChecksum(entry.getSequence().getCrc64());
		entry.getDbXReferences().forEach(val -> processDbReference(val, builder));
		builder.entryStored(getBinary(entry));
		entry.getTaxonomies().stream().forEach(taxon -> processTaxonomy(taxon, builder));
		builder.upid("UP000005640");
		UniParcDocument doc =builder.build();
		
		  storeManager.saveDocs(DataStoreManager.StoreType.UNIPARC, doc);

	}
	
	
	private void processDbReference(UniParcDBCrossReference xref, UniParcDocumentBuilder builder) {
		UniParcDatabaseType type = xref.getDatabaseType();
		if (xref.isActive()) {
			builder.active(type.toDisplayName());
		}
		builder.database(type.toDisplayName());
		if ((type == UniParcDatabaseType.SWISSPROT) || (type == UniParcDatabaseType.TREMBL)) {
			builder.uniprotAccession(xref.getId());
			builder.uniprotIsoform(xref.getId());
		}

		if (type == UniParcDatabaseType.SWISSPROT_VARSPLIC) {
			builder.uniprotIsoform(xref.getId());
		}
		xref.getProperties().stream().filter(val -> val.getKey().equals(UniParcDBCrossReference.PROPERTY_PROTEOME_ID))
				.map(val -> val.getValue()).forEach(val -> builder.upid(val));

		xref.getProperties().stream().filter(val -> val.getKey().equals(UniParcDBCrossReference.PROPERTY_PROTEIN_NAME))
				.map(val -> val.getValue()).forEach(val -> builder.proteinName(val));

		xref.getProperties().stream().filter(val -> val.getKey().equals(UniParcDBCrossReference.PROPERTY_GENE_NAME))
				.map(val -> val.getValue()).forEach(val -> builder.geneName(val));

	}

	private void processTaxonomy(Taxonomy taxon, UniParcDocumentBuilder builder) {
		builder.taxLineageId((int) taxon.getTaxonId());
		builder.organismTaxon(taxon.getScientificName());
		if (taxon.hasCommonName()) {
			builder.organismTaxon(taxon.getCommonName());
		}
		if (taxon.hasMnemonic()) {
			builder.organismTaxon(taxon.getMnemonic());
		}
		if (taxon.hasSynonyms()) {
			builder.organismTaxons(taxon.getSynonyms());
		}
	}


	 private ByteBuffer getBinary(UniParcEntry entry) {
	        try {
	            return ByteBuffer.wrap(UniParcJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
	        } catch (JsonProcessingException e) {
	            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
	        }
	    }

	
	 private String getName(String prefix, int i) {
			if(i<10) {
				return prefix + "0" +i;
			}else
				return prefix  + i;
			
		}

	private UniParcEntry createEntry(int i) {
		String seq = "MVSWGRFICLVVVTMATLSLARPSFSLVED";
		Sequence sequence = new SequenceBuilder(seq).build();
		List<UniParcDBCrossReference> xrefs = getXrefs(i);
		List<SequenceFeature> seqFeatures = getSeqFeatures(i) ;
		List<Taxonomy> taxonomies =getTaxonomies();
		UniParcEntry entry = new UniParcEntryBuilder().uniParcId(new UniParcIdBuilder(getName(UPI_PREF, i)).build())
				.databaseCrossReferences(xrefs).sequence(sequence)
				.sequenceFeatures(seqFeatures)
				.taxonomies(taxonomies).build();
		return entry;
	}
	private List<Taxonomy> getTaxonomies(){
		Taxonomy taxonomy = TaxonomyBuilder.newInstance().taxonId(9606).scientificName("Homo sapiens").build();
		Taxonomy taxonomy2 = TaxonomyBuilder.newInstance().taxonId(10090).scientificName("MOUSE").build();
		return Arrays.asList(taxonomy, taxonomy2);
	}
	private List<SequenceFeature> getSeqFeatures(int i) {
		List<Location> locations = Arrays.asList(new Location(12, 23), new Location(45, 89));
		InterproGroup domain = new InterProGroupBuilder().name(getName("Inter Pro Name", i)).id(getName("IP0000", i)).build();
		SequenceFeature sf = new SequenceFeatureBuilder().interproGroup(domain).signatureDbType(SignatureDbType.PFAM)
				.signatureDbId(getName("SIG0000", i)).locations(locations).build();
		SequenceFeature sf3 = new SequenceFeatureBuilder().from(sf).signatureDbType(SignatureDbType.PROSITE).build();
		return Arrays.asList(sf, sf3);
	}

	private List<UniParcDBCrossReference> getXrefs(int i) {
		List<Property> properties = new ArrayList<>();
		properties.add(new Property(UniParcDBCrossReference.PROPERTY_PROTEIN_NAME, getName("proteinName", i)));
		properties.add(new Property(UniParcDBCrossReference.PROPERTY_GENE_NAME, getName("geneName", i)));
		UniParcDBCrossReference xref = new UniParcDBCrossReferenceBuilder().versionI(3)
				.databaseType(UniParcDatabaseType.SWISSPROT).id("P12345").version(7).active(true)
				.created(LocalDate.of(2017, 5, 17)).lastUpdated(LocalDate.of(2017, 2, 27)).properties(properties)
				.build();

		List<Property> properties2 = new ArrayList<>();
		properties2.add(new Property(UniParcDBCrossReference.PROPERTY_PROTEIN_NAME,  getName("anotherProteinName", i)));
		properties2.add(new Property(UniParcDBCrossReference.PROPERTY_NCBI_TAXONOMY_ID, "9606"));

		UniParcDBCrossReference xref2 = new UniParcDBCrossReferenceBuilder().versionI(1)
				.databaseType(UniParcDatabaseType.TREMBL).id(getName("P123", i)).version(7).active(true)
				.created(LocalDate.of(2017, 2, 12)).lastUpdated(LocalDate.of(2017, 4, 23)).properties(properties2)
				.build();

		return Arrays.asList(xref, xref2);
	}
	
	
	  static class UniParcSearchParameterResolver extends AbstractSearchParameterResolver {

	        @Override
	        protected SearchParameter searchCanReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upi:UPI0000083A11"))
	                    .resultMatcher(jsonPath("$.results.*.uniParcId.value",contains("UPI0000083A11")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchCanReturnNotFoundParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upi:UPI0000083B11"))
	                    .resultMatcher(jsonPath("$.results.size()",is(0)))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upi:*"))
	                    .resultMatcher(jsonPath("$.results.*.uniParcId.value",contains("UPI0000083A11", "UPI0000083A20")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("taxonomy_name:[1 TO 10]"))
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",
	                    		contains("'taxonomy_name' filter type 'range' is invalid. Expected 'term' filter type")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upi:INVALID OR taxonomy_id:INVALID " +
	                            "OR length:INVALID OR upid:INVALID"))
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",containsInAnyOrder(
	                    		"The 'upi' value has invalid format. It should be a valid UniParc UPI",
	                            "The sequence length filter value should be a number",
	                            "The taxonomy id filter value should be a number",
	                            "The 'upid' value has invalid format. It should be a valid Proteome UPID"
	                      )))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("*:*"))
	                    .queryParam("sort", Collections.singletonList("upi desc"))
	                    .resultMatcher(jsonPath("$.results.*.uniParcId.value",contains("UPI0000083A20", "UPI0000083A11")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("*:*"))
	                    .queryParam("fields", Collections.singletonList("organism"))
	                    .resultMatcher(jsonPath("$.results.*.uniParcId.value",contains( "UPI0000083A11", "UPI0000083A20")))
	                  
	                
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("*:*"))
	                //    .queryParam("facets", Collections.singletonList("reference"))
	                    .resultMatcher(jsonPath("$.results.*.uniParcId.value",contains("UPI0000083A11", "UPI0000083A20")))
	                    .build();
	        }
	    }


	    static class UniParcSearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver{

	        @Override
	        protected SearchContentTypeParam searchSuccessContentTypesParam() {
	            return SearchContentTypeParam.builder()
	                    .query("taxonomy_id:9606")
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_JSON)
	                            .resultMatcher(jsonPath("$.results.*.uniParcId.value",contains("UPI0000083A11", "UPI0000083A20")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_XML)
	                            .resultMatcher(content().string(containsString("UPI0000083A11")))
	                            .resultMatcher(content().string(containsString("UPI0000083A20")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString("UPI0000083A11")))
	                            .resultMatcher(content().string(containsString("UPI0000083A20")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString("Entry\tOrganisms\tUniProtKB\tFirst seen\tLast seen\tLength")))
	                            .resultMatcher(content().string(containsString("UPI0000083A11	Homo sapiens; MOUSE	P12345; P12311	2017-02-12	2017-04-23	30")))
	                            .resultMatcher(content().string(containsString("UPI0000083A20	Homo sapiens; MOUSE	P12345; P12320	2017-02-12	2017-04-23	30")))

	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
	                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
	                            .resultMatcher(content().contentType(UniProtMediaType.FASTA_MEDIA_TYPE))
	                            .build())
	                    .build();
	        }

	        @Override
	        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
	            return SearchContentTypeParam.builder()
	                    .query("upid:invalid")
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_JSON)
	                            .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                            .resultMatcher(jsonPath("$.messages.*",contains("The 'upid' value has invalid format. It should be a valid Proteome UPID")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_XML)
	                            .resultMatcher(content().string(containsString("The 'upid' value has invalid format. It should be a valid Proteome UPID")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
	                            .resultMatcher(content().string(isEmptyString()))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
	                            .resultMatcher(content().string(isEmptyString()))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
	                            .resultMatcher(content().string(isEmptyString()))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
	                            .resultMatcher(content().contentType(UniProtMediaType.FASTA_MEDIA_TYPE))
	                            .build())
	                    .build();
	        }
	    }
}


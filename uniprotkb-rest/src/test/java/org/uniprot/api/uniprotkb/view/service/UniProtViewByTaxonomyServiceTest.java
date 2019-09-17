package org.uniprot.api.uniprotkb.view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniprotkb.view.TaxonomyNode;
import org.uniprot.api.uniprotkb.view.ViewBy;


@ExtendWith(MockitoExtension.class)
class UniProtViewByTaxonomyServiceTest {
	 @Mock
	 private SolrClient solrClient;
	 
	 @Mock
	 private TaxonomyService taxonService;
	 private UniProtViewByTaxonomyService service;
	 
	 
	@BeforeEach
	public void setup() {
		solrClient =Mockito.mock(SolrClient.class);
		taxonService =Mockito.mock(TaxonomyService.class);

		service = new UniProtViewByTaxonomyService( solrClient, "uniprot", taxonService);
	 }
	 
	@Test
	public void test()throws IOException, SolrServerException {
		mockTaxonService();
		Map<String, Long> counts = new HashMap<>();
		counts.put("1425170", 23l);
		counts.put("9606", 50l);
		MockServiceHelper.mockServiceQueryResponse( solrClient, "taxonomoy_id", counts);
		List<ViewBy> viewBys = service.get("", "");
		assertEquals(2, viewBys.size());
		ViewBy viewBy1 = MockServiceHelper.createViewBy("1425170", "Homo heidelbergensis", 23l, UniProtViewByTaxonomyService.URL_PREFIX +"1425170" , false);
		assertTrue(viewBys.contains(viewBy1));
		ViewBy viewBy2 = MockServiceHelper.createViewBy("9606", "Homo sapiens", 50l, UniProtViewByTaxonomyService.URL_PREFIX +"9606" , false);
		assertTrue(viewBys.contains(viewBy2));
	}

   

	void mockTaxonService() {
		List<TaxonomyNode> nodes = new ArrayList<>();
		TaxonomyNode node1 = new TaxonomyNode();
		node1.setTaxonomyId(1425170);
		node1.setScientificName("Homo heidelbergensis");
		
		TaxonomyNode node2 = new TaxonomyNode();
		node2.setTaxonomyId(9606);
		node2.setScientificName("Homo sapiens");
		nodes.add(node1);
		nodes.add(node2);
		when(taxonService.getChildren(any())).thenReturn(nodes);
	}
}

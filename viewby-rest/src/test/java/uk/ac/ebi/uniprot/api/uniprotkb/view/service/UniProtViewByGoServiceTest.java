package uk.ac.ebi.uniprot.api.uniprotkb.view.service;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import uk.ac.ebi.uniprot.api.uniprotkb.view.model.ViewBy;
import uk.ac.ebi.uniprot.api.uniprotkb.view.service.UniProtViewByGoService;
import uk.ac.ebi.uniprot.cv.go.GoService;
import uk.ac.ebi.uniprot.cv.go.GoTerm;
import uk.ac.ebi.uniprot.cv.go.impl.GoTermImpl;

@RunWith(MockitoJUnitRunner.class)
class UniProtViewByGoServiceTest {
	 @Mock
	 private SolrClient solrClient;
	 
	 @Mock
	 private GoService goService;
	 private UniProtViewByGoService service;
	 
	 
	@BeforeEach
	public void setup() {
		solrClient =Mockito.mock(SolrClient.class);
		goService =Mockito.mock(GoService.class);
		mockGoService();
		service = new UniProtViewByGoService( solrClient, "uniprot", goService);
	 }
	
	@Test
	void test() throws IOException, SolrServerException {
		Map<String, Long> counts = new HashMap<>();
		counts.put("GO:0008150", 78l);
		counts.put("GO:0005575", 70l);	
		counts.put("GO:0003674", 73l);	
		MockServiceHelper.mockServiceQueryResponse( solrClient, "go_id", counts);
		List<ViewBy> viewBys = service.get("", "1");
		assertEquals(3, viewBys.size());
		ViewBy viewBy1 = MockServiceHelper.createViewBy("GO:0008150", "biological_process", 78l, UniProtViewByGoService.URL_PREFIX +"GO:0008150" , true);
		assertTrue(viewBys.contains(viewBy1));
		ViewBy viewBy2 = MockServiceHelper.createViewBy("GO:0005575", "cellular_component", 70l, UniProtViewByGoService.URL_PREFIX +"GO:0005575" , true);
		assertTrue(viewBys.contains(viewBy2));
		ViewBy viewBy3 = MockServiceHelper.createViewBy("GO:0003674", "molecular_function", 73l, UniProtViewByGoService.URL_PREFIX +"GO:0003674" , true);
		assertTrue(viewBys.contains(viewBy3));
	}
	void mockGoService() {
		List<String> goids =Arrays.asList("GO:0008150", "GO:0005575", "GO:0003674");
		when(goService.getChildrenById(any())).thenReturn(goids);
		
		GoTerm goTerm1 = new GoTermImpl("GO:0008150", "biological_process") ;
		GoTerm goTerm2 = new GoTermImpl("GO:0005575", "cellular_component") ;
		GoTerm goTerm3 = new GoTermImpl("GO:0003674", "molecular_function") ;
		
		when(goService.getGoTermById("GO:0008150")).thenReturn(goTerm1);
		when(goService.getGoTermById("GO:0005575")).thenReturn(goTerm2);
		when(goService.getGoTermById("GO:0003674")).thenReturn(goTerm3);
		
		
	}
}

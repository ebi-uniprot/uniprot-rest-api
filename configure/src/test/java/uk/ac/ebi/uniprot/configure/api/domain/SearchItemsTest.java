package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItem;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItemType;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtSearchItem;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtSearchItems;


public class SearchItemsTest {

	
	@Test
	public void testRead2() {
		String filename ="src/main/resources/uniprot/uniprot_search.json";
		try {
			UniProtSearchItems terms =UniProtSearchItems.readFromFile(filename);
			 List<UniProtSearchItem> items = terms.getSearchItems();
			 for(UniProtSearchItem item :items) {
				 checkItem(item);
			 }
			
		assertEquals(27, terms.getSearchItems().size() );
	//	System.out.println(terms);
	//	System.out.println( terms.getSearchItems().size() +"\t and total search terms= " + terms.getCount());
		}catch(Exception e ) {
			e.printStackTrace();
			fail("test");
		}
	}
	private void checkItem(SearchItem item) {
		if((item.getItemType() ==SearchItemType.GROUP) || (item.getItemType() ==SearchItemType.GROUP_DISPLAY)) {
			 List<SearchItem> items = item.getItems();
			 items.forEach(val ->checkItem(val));
		}else {
			String term = item.getTerm();
			System.out.println(item.getLabel() +"\t" + term );
			String autoComplete = item.getAutoComplete();
			if(!isNullOrEmpty(autoComplete)) {
				System.err.println(autoComplete);
			}
			assertFalse(isNullOrEmpty(term));
		}
	}
	private boolean isNullOrEmpty(String val) {
		return (val ==null)||( val.length()==0);
	}
}

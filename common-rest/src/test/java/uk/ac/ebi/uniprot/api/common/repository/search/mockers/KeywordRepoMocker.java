package uk.ac.ebi.uniprot.api.common.repository.search.mockers;

import uk.ac.ebi.uniprot.dataservice.source.impl.keyword.KeywordFileRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.keyword.KeywordRepo;

public class KeywordRepoMocker {
	public static KeywordRepo getKeywordRepo() {
	//	 String filePath= KeywordRepoMocker.class.getClassLoader().getResource("keywlist.txt").getFile();		 
		 return new KeywordFileRepo("keywlist.txt");
	}
}

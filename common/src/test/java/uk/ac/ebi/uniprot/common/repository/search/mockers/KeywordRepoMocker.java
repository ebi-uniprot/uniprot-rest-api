package uk.ac.ebi.uniprot.common.repository.search.mockers;

import uk.ac.ebi.uniprot.dataservice.source.impl.keyword.KeywordFileRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.keyword.KeywordRepo;

public class KeywordRepoMocker {
	public static KeywordRepo getKeywordRepo() {
		 String filePath= Thread.currentThread().getContextClassLoader().getResource("keywlist.txt").getFile();
		 
		 return new KeywordFileRepo(filePath);
	}
}

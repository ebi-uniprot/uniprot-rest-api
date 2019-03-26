package uk.ac.ebi.uniprot.common.repository.search.mockers;

import uk.ac.ebi.uniprot.dataservice.source.impl.pathway.PathwayFileRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.pathway.PathwayRepo;

public class PathwayRepoMocker {
	public static PathwayRepo getPathwayRepo() {
		   String filePath= Thread.currentThread().getContextClassLoader().getResource("unipathway.txt").getFile();
		   return new PathwayFileRepo(filePath);
	}
}

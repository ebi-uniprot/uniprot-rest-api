package uk.ac.ebi.uniprot.api.common.repository.search.mockers;

import uk.ac.ebi.uniprot.dataservice.source.impl.pathway.PathwayFileRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.pathway.PathwayRepo;

public class PathwayRepoMocker {
	public static PathwayRepo getPathwayRepo() {
		return new PathwayFileRepo("unipathway.txt");
	}
}

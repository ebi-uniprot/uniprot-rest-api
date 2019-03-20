package uk.ac.ebi.uniprot.common.repository.search.mockers;

import uk.ac.ebi.uniprot.dataservice.source.impl.taxonomy.FileNodeIterable;
import uk.ac.ebi.uniprot.dataservice.source.impl.taxonomy.TaxonomyMapRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.taxonomy.TaxonomyRepo;

import java.io.File;

public class TaxonomyRepoMocker {

    public static TaxonomyRepo getTaxonomyRepo() {
        String filePath= Thread.currentThread().getContextClassLoader().getResource("taxonomy/taxonomy.dat").getFile();
        File taxonomicFile = new File(filePath);

        FileNodeIterable taxonomicNodeIterable = new FileNodeIterable(taxonomicFile);
        return new TaxonomyMapRepo(taxonomicNodeIterable);
    }

}

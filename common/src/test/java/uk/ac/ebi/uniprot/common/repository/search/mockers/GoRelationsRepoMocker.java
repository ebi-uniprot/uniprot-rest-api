package uk.ac.ebi.uniprot.common.repository.search.mockers;

import uk.ac.ebi.uniprot.dataservice.source.impl.go.GoRelationFileReader;
import uk.ac.ebi.uniprot.dataservice.source.impl.go.GoRelationFileRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.go.GoRelationRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.go.GoTermFileReader;

public class GoRelationsRepoMocker {

    public static GoRelationRepo getGoRelationRepo() {
        String gotermPath = Thread.currentThread().getContextClassLoader().getResource("goterm").getFile();
        return GoRelationFileRepo.create(new GoRelationFileReader(gotermPath),
                new GoTermFileReader(gotermPath));
    }
}

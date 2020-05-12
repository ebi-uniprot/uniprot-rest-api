package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created 05/05/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class UniProtKBEntryInteraction {
    private  String accession;
    private  String name;
    private  String proteinExistence;
    private  long taxonomy;
    private List<Interaction.IntActComment> interactions;
    private  List<DiseaseComment> diseases;
    private  List<SubcellularLocationComment> subcellularLocations;
}

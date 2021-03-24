package org.uniprot.api.uniparc.output.converter;

import org.uniprot.api.uniparc.model.UniParcEntryWrapper;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.core.uniparc.UniParcCrossReference;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sahmad
 * @created 24/03/2021
 */
public class UniParcEntryCrossRefValueMapper implements EntityValueMapper<UniParcCrossReference> {
    @Override
    public Map<String, String> mapEntity(UniParcCrossReference entity, List<String> fieldNames) {
        Map<String, String> fieldValue = new HashMap<>();
        fieldValue.put("database", entity.getDatabase().getName());
        fieldValue.put("accession", entity.getId());
        fieldValue.put("gene", entity.getGeneName());
        fieldValue.put("ncbiGi", entity.getNcbiGi());
        fieldValue.put("organism", entity.getOrganism().getScientificName());
        fieldValue.put("organism_id", String.valueOf(entity.getOrganism().getTaxonId()));
        fieldValue.put("protein", entity.getProteinName());
        fieldValue.put("proteome", entity.getProteomeId() + ":" + entity.getComponent());
        fieldValue.put("active", entity.isActive() ? "Yes" : "No");
        fieldValue.put("first_seen", entity.getCreated().toString());
        fieldValue.put("last_seen", entity.getLastUpdated().toString());
        fieldValue.put("timeline", String.valueOf(Period.between(entity.getCreated(), entity.getLastUpdated()).getDays()));
        fieldValue.put("version", String.valueOf(entity.getVersion()));
        fieldValue.put("version_uniparc", String.valueOf(entity.getVersionI()));
        return fieldValue;
    }
}

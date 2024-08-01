package org.uniprot.api.uniparc.common.repository.store.crossref;

import static org.uniprot.core.util.Utils.notNull;
import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.core.uniparc.Proteome;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.ProteomeBuilder;
import org.uniprot.core.uniparc.impl.UniParcEntryLightBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;

public class UniParcCrossReferenceLazyLoader {

    private static final ReturnFieldConfig FIELD_CONFIG =
            ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC);
    static final List<String> LAZY_FIELD_LIST =
            List.of("organism_id", "organism", "protein", "gene", "proteome");
    private final UniProtStoreClient<UniParcCrossReference> crossRefStoreClient;
    private final int batchSize;

    public UniParcCrossReferenceLazyLoader(
            UniProtStoreClient<UniParcCrossReference> crossRefStoreClient, int batchSize) {
        this.crossRefStoreClient = crossRefStoreClient;
        this.batchSize = batchSize;
    }

    public List<UniParcEntryLight> populateLazyFields(
            List<UniParcEntryLight> entries, List<String> lazyFields) {
        List<UniParcEntryLight> result = new ArrayList<>();
        for (UniParcEntryLight entry : entries) {
            UniParcEntryLight lightEntry = populateLazyFields(entry, lazyFields);
            result.add(lightEntry);
        }
        return result;
    }

    public UniParcEntryLight populateLazyFields(UniParcEntryLight entry, List<String> lazyFields) {
        UniParcEntryLightBuilder builder = UniParcEntryLightBuilder.from(entry);
        int crossRefSize = entry.getUniParcCrossReferences().size();
        for (int i = 0; i < crossRefSize; i = i + batchSize) {
            List<String> batchIds =
                    entry.getUniParcCrossReferences()
                            .subList(i, Math.min(i + batchSize, crossRefSize));
            addLazyFields(builder, lazyFields, batchIds);
        }
        return builder.build();
    }

    public List<String> getLazyFields(String fields) {
        List<String> result = new ArrayList<>();
        if (notNullNotEmpty(fields)) {
            for (String field : fields.split(",")) {
                String fieldItem = FIELD_CONFIG.getReturnFieldByName(field.strip()).getName();
                if (LAZY_FIELD_LIST.contains(fieldItem)) {
                    result.add(fieldItem);
                }
            }
        }
        return result;
    }

    private void addLazyFields(
            UniParcEntryLightBuilder builder, List<String> lazyFields, List<String> batchIds) {
        List<UniParcCrossReference> xrefs = crossRefStoreClient.getEntries(batchIds);
        for (UniParcCrossReference xref : xrefs) {
            if ((lazyFields.contains(LAZY_FIELD_LIST.get(0))
                            || lazyFields.contains(LAZY_FIELD_LIST.get(1)))
                    && notNull(xref.getOrganism())) {
                builder.organismsAdd(xref.getOrganism());
            }
            if (lazyFields.contains(LAZY_FIELD_LIST.get(2))
                    && notNullNotEmpty(xref.getProteinName())) {
                builder.proteinNamesAdd(xref.getProteinName());
            }
            if (lazyFields.contains(LAZY_FIELD_LIST.get(3))
                    && notNullNotEmpty(xref.getGeneName())) {
                builder.geneNamesAdd(xref.getGeneName());
            }
            if (lazyFields.contains(LAZY_FIELD_LIST.get(4))
                    && notNullNotEmpty(xref.getProteomeId())) {
                Proteome proteome =
                        new ProteomeBuilder()
                                .id(xref.getProteomeId())
                                .component(xref.getComponent())
                                .build();
                builder.proteomesAdd(proteome);
            }
        }
    }
}

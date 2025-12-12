package org.uniprot.api.common.repository.stream.store.uniparc;

import static org.uniprot.core.util.Utils.notNull;
import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.core.uniparc.Proteome;
import org.uniprot.core.uniparc.ProteomeIdComponent;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.ProteomeBuilder;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
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
    private final UniProtStoreClient<UniParcCrossReferencePair> crossRefStoreClient;
    private final UniParcCrossReferenceStoreConfigProperties configProperties;

    public UniParcCrossReferenceLazyLoader(
            UniProtStoreClient<UniParcCrossReferencePair> crossRefStoreClient,
            UniParcCrossReferenceStoreConfigProperties configProperties) {
        this.crossRefStoreClient = crossRefStoreClient;
        this.configProperties = configProperties;
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
        int batchSize = configProperties.getGroupSize();
        UniParcEntryLightBuilder builder = UniParcEntryLightBuilder.from(entry);
        if (entry.getCrossReferenceCount() > 0) {
            int pageNumbers = (entry.getCrossReferenceCount() / batchSize);
            for (int i = 0; i <= pageNumbers; i = i + batchSize) {
                addLazyFields(builder, lazyFields, entry.getUniParcId() + "_" + i);
            }
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
            UniParcEntryLightBuilder builder, List<String> lazyFields, String key) {
        UniParcCrossReferencePair pair =
                crossRefStoreClient
                        .getEntry(key)
                        .orElseThrow(
                                () ->
                                        new ServiceException(
                                                "unable to find cross reference page " + key));
        for (UniParcCrossReference xref : pair.getValue()) {
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
                    && notNullNotEmpty(xref.getProteomeIdComponents())) {
                List<ProteomeIdComponent> proteomeIdComponents = xref.getProteomeIdComponents();

                for (ProteomeIdComponent proteomeIdComponentPair : proteomeIdComponents) {
                    String proteomeId = proteomeIdComponentPair.getProteomeId();
                    String proteomeComponent = proteomeIdComponentPair.getComponent();
                    Proteome proteome =
                            new ProteomeBuilder()
                                    .id(proteomeId)
                                    .component(proteomeComponent)
                                    .build();
                    builder.proteomesAdd(proteome);
                }
            }
        }
    }
}

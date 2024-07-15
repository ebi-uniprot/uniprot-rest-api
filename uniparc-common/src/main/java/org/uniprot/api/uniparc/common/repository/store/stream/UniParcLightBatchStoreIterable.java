package org.uniprot.api.uniparc.common.repository.store.stream;

import static org.uniprot.core.util.Utils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcEntryLightBuilder;
import org.uniprot.core.util.PairImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;

import net.jodah.failsafe.RetryPolicy;

public class UniParcLightBatchStoreIterable extends BatchStoreIterable<UniParcEntryLight> {

    static final List<String> LAZY_FIELD_LIST = List.of("organism_id", "organism", "protein", "gene", "proteome");
    private static final ReturnFieldConfig FIELD_CONFIG =
            ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC);
    private final UniProtStoreClient<UniParcCrossReference> crossRefStoreClient;
    private final List<String> lazyFields;
    private final int batchSize;

    public UniParcLightBatchStoreIterable(
            Iterable<String> sourceIterable,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            UniProtStoreClient<UniParcCrossReference> crossRefStoreClient,
            String fields) {
        super(sourceIterable, storeClient, retryPolicy, batchSize);
        this.crossRefStoreClient = crossRefStoreClient;
        this.lazyFields = getLazyFields(fields);
        this.batchSize = batchSize;
    }

    public UniParcLightBatchStoreIterable(
            Iterator<String> sourceIterator,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            RetryPolicy<Object> retryPolicy,
            int batchSize,
            UniProtStoreClient<UniParcCrossReference> crossRefStoreClient,
            String fields) {
        super(sourceIterator, storeClient, retryPolicy, batchSize);
        this.crossRefStoreClient = crossRefStoreClient;
        this.lazyFields = getLazyFields(fields);
        this.batchSize = batchSize;
    }

    @Override
    protected List<UniParcEntryLight> convertBatch(List<String> batch) {
        List<UniParcEntryLight> entries = super.convertBatch(batch);
        if (notNullNotEmpty(lazyFields)) {
            entries = loadLazyLoadFields(entries);
        }
        return entries;
    }


    private List<UniParcEntryLight> loadLazyLoadFields(List<UniParcEntryLight> entries) {
        List<UniParcEntryLight> result = new ArrayList<>();
        for (UniParcEntryLight entry : entries) {
            UniParcEntryLightBuilder builder = UniParcEntryLightBuilder.from(entry);
            List<String> batchIds = new ArrayList<>(batchSize);
            int index = 0;
            for (String xrefId : entry.getUniParcCrossReferences()) {
                batchIds.add(xrefId);
                if (++index % batchSize == 0) {
                    addLazyFields(builder, batchIds);
                    batchIds = new ArrayList<>();
                }
            }
            if (!batchIds.isEmpty()) {
                addLazyFields(builder, batchIds);
            }
            result.add(builder.build());
        }
        return result;
    }

    private void addLazyFields(UniParcEntryLightBuilder builder, List<String> batchIds) {
        List<UniParcCrossReference> xrefs = crossRefStoreClient.getEntries(batchIds);
        for (UniParcCrossReference xref : xrefs) {
            if ((lazyFields.contains(LAZY_FIELD_LIST.get(0)) || lazyFields.contains(LAZY_FIELD_LIST.get(1)))
                    && notNull(xref.getDatabase())) {
                builder.organismsAdd(
                        new PairImpl<>(
                                (int) xref.getOrganism().getTaxonId(),
                                xref.getOrganism().getScientificName()));
            }
            if (lazyFields.contains(LAZY_FIELD_LIST.get(2)) && notNullNotEmpty(xref.getProteinName())) {
                builder.proteinNamesAdd(xref.getProteinName());
            }
            if (lazyFields.contains(LAZY_FIELD_LIST.get(3)) && notNullNotEmpty(xref.getGeneName())) {
                builder.geneNamesAdd(xref.getGeneName());
            }
            if (lazyFields.contains(LAZY_FIELD_LIST.get(4)) && notNullNotEmpty(xref.getProteomeId())) {
                builder.proteomeIdsAdd(xref.getProteomeId());
            }
        }
    }

    private List<String> getLazyFields(String fields) {
        List<String> result = new ArrayList<>();
        if (notNullNotEmpty(fields)) {
            for (String field : fields.split(",")) {
                String fieldItem = FIELD_CONFIG.getReturnFieldByName(field.strip()).getName();
                if(LAZY_FIELD_LIST.contains(fieldItem)){
                    result.add(fieldItem);
                }
            }
        }
        return result;
    }
}

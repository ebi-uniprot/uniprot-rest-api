package org.uniprot.api.literature.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Component
public class LiteratureSortClause extends AbstractSolrSortClause {
    private static final String DOC_ID = "id";

    @Override
    protected List<Pair<String, Sort.Direction>> getDefaultFieldSortOrderPairs() {
        if (this.defaultFieldSortOrderPairs == null) {
            this.defaultFieldSortOrderPairs = new ArrayList<>();
            this.defaultFieldSortOrderPairs.add(new ImmutablePair<>(DOC_ID, Sort.Direction.ASC));
        }
        return this.defaultFieldSortOrderPairs;
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return DOC_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.LITERATURE;
    }
}

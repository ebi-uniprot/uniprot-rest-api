package org.uniprot.api.uniprotkb.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

@Component
public class UniProtSolrSortClause extends AbstractSolrSortClause {
    private static final String ANNOTATION_SCORE = "annotation_score";
    private static final String ACCESSION_ID = "accession_id";

    @Override
    protected List<Pair<String, Sort.Direction>> getDefaultFieldSortOrderPairs() {
        if (this.defaultFieldSortOrderPairs == null) {
            this.defaultFieldSortOrderPairs = new ArrayList<>();
            this.defaultFieldSortOrderPairs.add(
                    new ImmutablePair<>(ANNOTATION_SCORE, Sort.Direction.DESC));
            this.defaultFieldSortOrderPairs.add(
                    new ImmutablePair<>(ACCESSION_ID, Sort.Direction.ASC));
        }
        return this.defaultFieldSortOrderPairs;
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return ACCESSION_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    public String getSolrSortFieldName(String name) {
        return super.getSolrSortFieldName(name);
    }
}

package org.uniprot.api.proteome.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.genecentric.GeneCentricEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.genecentric.GeneCentricDocument;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@Service
@Import(GeneCentricSolrQueryConfig.class)
public class GeneCentricService extends BasicSearchService<GeneCentricDocument, GeneCentricEntry> {
    public static final String GENECENTRIC_ID_FIELD = "accession_id";
    private final SearchFieldConfig searchFieldConfig;

    @Autowired
    public GeneCentricService(
            GeneCentricQueryRepository repository,
            SearchFieldConfig geneCentricSearchFieldConfig,
            RequestConverter geneCentricRequestConverter) {
        super(repository, new GeneCentricEntryConverter(), geneCentricRequestConverter);
        searchFieldConfig = geneCentricSearchFieldConfig;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(GENECENTRIC_ID_FIELD);
    }
}

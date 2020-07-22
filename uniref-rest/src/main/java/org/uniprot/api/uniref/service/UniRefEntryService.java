package org.uniprot.api.uniref.service;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.uniref.request.UniRefIdRequest;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
@Service
public class UniRefEntryService{

    private final SearchFieldConfig searchFieldConfig;
    private final SolrQueryRepository<UniRefDocument> repository;
    private final UniRefEntryConverter entryConverter;
    @Autowired
    public UniRefEntryService(
            SolrQueryRepository<UniRefDocument> repository, UniRefEntryConverter entryConverter) {
        this.repository = repository;
        this.entryConverter = entryConverter;
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF);
    }

    public UniRefEntryResult getEntity(String value, UniRefIdRequest idRequesst) {
        try {
            UniRefDocument document = getEntryFromSolr(value);
            UniRefEntryResult entry = entryConverter.convertEntry(document, idRequesst);
            if (entry == null) {
                String message =
                        entryConverter.getClass() + " can not convert object for: [" + value + "]";
                throw new ServiceException(message);
            } else {
                return entry;
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get entity for id: [" + value + "]";
            throw new ServiceException(message, e);
        }
    }

    private UniRefDocument getEntryFromSolr(String value) {
        SolrRequest solrRequest = SolrRequest.builder()
                .query(getIdField() + ":" + value)
                .rows(NumberUtils.INTEGER_ONE)
                .build();
        return repository
                .getEntry(solrRequest)
                .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));
    }

    private String getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }
}

package org.uniprot.api.uniref.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.uniref.repository.store.UniRefEntryStoreRepository;
import org.uniprot.api.uniref.request.UniRefIdRequest;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
@Service
public class UniRefEntryService {

    private final UniRefEntryStoreRepository entryStoreRepository;

    @Autowired
    public UniRefEntryService(UniRefEntryStoreRepository entryStoreRepository) {
        this.entryStoreRepository = entryStoreRepository;
    }

    public UniRefEntryResult getEntity(String clusterId, UniRefIdRequest idRequesst) {
        try {
            return entryStoreRepository.getEntryById(clusterId, idRequesst);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get entity for id: [" + clusterId + "]";
            throw new ServiceException(message, e);
        }
    }

    public List<Facet> getFacets(String clusterId) {
        try {
            return entryStoreRepository.getFacets(clusterId);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get entity for id: [" + clusterId + "]";
            throw new ServiceException(message, e);
        }
    }
}

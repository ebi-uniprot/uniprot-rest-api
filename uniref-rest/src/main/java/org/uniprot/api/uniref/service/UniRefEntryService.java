package org.uniprot.api.uniref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.uniref.repository.store.UniRefEntryStoreRepository;
import org.uniprot.core.uniref.UniRefEntry;

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

    public UniRefEntry getEntity(String clusterId) {
        try {
            return entryStoreRepository.getEntryById(clusterId);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get entity for id: [" + clusterId + "]";
            throw new ServiceException(message, e);
        }
    }
}

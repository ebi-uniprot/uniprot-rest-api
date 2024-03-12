package org.uniprot.api.uniref.common.service.entry;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.stream.rdf.RdfServiceFactory;
import org.uniprot.api.uniref.common.repository.store.UniRefEntryStoreRepository;
import org.uniprot.core.uniref.UniRefEntry;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
@Service
public class UniRefEntryService {

    private final UniRefEntryStoreRepository entryStoreRepository;
    private final RdfServiceFactory rdfServiceFactory;

    @Autowired
    public UniRefEntryService(
            UniRefEntryStoreRepository entryStoreRepository,
            RdfServiceFactory uniRefRdfServiceFactory) {
        this.entryStoreRepository = entryStoreRepository;
        this.rdfServiceFactory = uniRefRdfServiceFactory;
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

    public String getRdf(String id, String type, String format) {
        ResourceNotFoundException nfe =
                new ResourceNotFoundException(
                        "Unable to get UniRefEntry from store. ClusterId:" + id);
        String rdf =
                this.rdfServiceFactory
                        .getRdfService(type, format)
                        .getEntry(id)
                        .orElseThrow(() -> nfe);
        if (Objects.isNull(rdf)) {
            throw nfe;
        }
        return rdf;
    }
}

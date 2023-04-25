package org.uniprot.api.uniref.service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.rest.service.RDFClient;
import org.uniprot.api.rest.service.TagProvider;
import org.uniprot.api.uniref.repository.store.UniRefEntryStoreRepository;
import org.uniprot.core.uniref.UniRefEntry;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
@Service
public class UniRefEntryService {

    private final UniRefEntryStoreRepository entryStoreRepository;
    private final RDFClient RDFClient;

    @Autowired
    public UniRefEntryService(
            UniRefEntryStoreRepository entryStoreRepository,
            @Qualifier("unirefRdfRestTemplate") RestTemplate restTemplate,
            TagProvider tagProvider) {
        this.entryStoreRepository = entryStoreRepository;
        this.RDFClient = new RDFClient(tagProvider, restTemplate);
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

    public String getRDFXml(String id, String type, String format) {
        ResourceNotFoundException nfe =
                new ResourceNotFoundException(
                        "Unable to get UniRefEntry from store. ClusterId:" + id);
        String rdf = this.RDFClient.getEntry(id, type, format).orElseThrow(() -> nfe);
        if (Objects.isNull(rdf)) {
            throw nfe;
        }
        return rdf;
    }
}

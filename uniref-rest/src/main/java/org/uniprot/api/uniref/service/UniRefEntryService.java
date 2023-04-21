package org.uniprot.api.uniref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.rest.service.RDFXMLClient;
import org.uniprot.api.rest.service.TagProvider;
import org.uniprot.api.uniref.repository.store.UniRefEntryStoreRepository;
import org.uniprot.core.uniref.UniRefEntry;

import java.util.Objects;

/**
 * @author lgonzales
 * @since 09/07/2020
 */
@Service
public class UniRefEntryService {

    private final UniRefEntryStoreRepository entryStoreRepository;
    private final RDFXMLClient rdfxmlClient;

    @Autowired
    public UniRefEntryService(
            UniRefEntryStoreRepository entryStoreRepository,
            @Qualifier("idMappingRdfRestTemplate") RestTemplate restTemplate,
            TagProvider tagProvider) {
        this.entryStoreRepository = entryStoreRepository;
        this.rdfxmlClient = new RDFXMLClient(tagProvider, restTemplate);
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
        String rdf = this.rdfxmlClient.getEntry(id, type, format).orElseThrow(() -> nfe);
        if (Objects.isNull(rdf)) {
            throw nfe;
        }
        return rdf;
    }
}

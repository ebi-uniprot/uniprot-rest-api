package org.uniprot.api.common.repository.stream.rdf;

import java.util.List;
import java.util.Optional;

import org.uniprot.api.rest.service.RDFXMLClient;
import org.uniprot.store.datastore.common.StoreService;

public class RdfXmlService implements StoreService<String> {
    private final String type;
    private final String format;
    private final RDFXMLClient rdfxmlClient;

    public RdfXmlService(String type, String format, RDFXMLClient rdfxmlClient) {
        this.type = type;
        this.format = format;
        this.rdfxmlClient = rdfxmlClient;
    }

    @Override
    public List<String> getEntries(Iterable<String> ids) {
        return rdfxmlClient.getEntries(ids, type, format);
    }

    @Override
    public String getStoreName() {
        return "RdfXmlStore";
    }

    @Override
    public Optional<String> getEntry(String id) {
        return rdfxmlClient.getEntry(id, type, format);
    }
}

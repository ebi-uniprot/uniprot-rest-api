package org.uniprot.api.common.repository.stream.rdf;

import java.util.List;
import java.util.Optional;

import org.uniprot.api.rest.service.RDFClient;
import org.uniprot.store.datastore.common.StoreService;

public class RdfXmlService implements StoreService<String> {
    private final String type;
    private final String format;
    private final RDFClient RDFClient;

    public RdfXmlService(String type, String format, RDFClient RDFClient) {
        this.type = type;
        this.format = format;
        this.RDFClient = RDFClient;
    }

    @Override
    public List<String> getEntries(Iterable<String> ids) {
        return RDFClient.getEntries(ids, type, format);
    }

    @Override
    public String getStoreName() {
        return "RdfXmlStore";
    }

    @Override
    public Optional<String> getEntry(String id) {
        return RDFClient.getEntry(id, type, format);
    }
}

package org.uniprot.api.support.data.common;

import java.util.function.Function;

import org.uniprot.store.search.document.Document;

/**
 * @author sahmad
 * @created 01/02/2021
 *     <p>RDF needs just the integral part of cross ref id. e.g. 234 of DB-0234, for disease id.
 *     e.g. 4240 of DI-04240
 */
public class SolrDocumentRDFIdConverter implements Function<Document, String> {
    @Override
    public String apply(Document solrDocument) {
        String[] parts = solrDocument.getDocumentId().split("-");
        int idValue = Integer.parseInt(parts[1]);
        return String.valueOf(idValue);
    }
}

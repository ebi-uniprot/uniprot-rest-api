package org.uniprot.api.support.data.common;

import java.util.function.Function;

import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

/**
 * @author sahmad
 * @created 01/02/2021
 *     <p>RDF needs just the integral part of cross ref id. e.g. 234 of DB-0234, for disease id.
 *     e.g. 4240 of DI-04240
 */
public class SolrDocumentRdfIdConverter implements Function<Document, String> {
    @Override
    public String apply(Document solrDocument) {
        String[] parts = solrDocument.getDocumentId().split("-");
        int idValue;
        if (solrDocument instanceof TaxonomyDocument
                || solrDocument instanceof LiteratureDocument) {
            idValue = Integer.parseInt(parts[0]);
        } else {
            idValue = Integer.parseInt(parts[1]);
        }
        return String.valueOf(idValue);
    }
}

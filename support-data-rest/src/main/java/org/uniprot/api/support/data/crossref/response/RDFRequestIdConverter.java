package org.uniprot.api.support.data.crossref.response;

import java.util.function.Function;

import org.uniprot.store.search.document.dbxref.CrossRefDocument;

/**
 * @author sahmad
 * @created 01/02/2021
 *     <p>RDF needs just the integral part of cross ref id. e.g. 234 of DB-0234
 */
public class RDFRequestIdConverter implements Function<CrossRefDocument, String> {
    @Override
    public String apply(CrossRefDocument crossRefDocument) {
        String[] parts = crossRefDocument.getDocumentId().split("-");
        int idValue = Integer.parseInt(parts[1]);
        return String.valueOf(idValue);
    }
}

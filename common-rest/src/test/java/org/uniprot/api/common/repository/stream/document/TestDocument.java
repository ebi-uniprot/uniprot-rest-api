package org.uniprot.api.common.repository.stream.document;

import org.uniprot.store.search.document.Document;

/**
 * @author sahmad
 * @created 27/01/2021
 */
public class TestDocument implements Document {
    private static final long serialVersionUID = 4437254385462185898L;
    private String id;
    private String name;

    public TestDocument(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getDocumentId() {
        return id;
    }
}

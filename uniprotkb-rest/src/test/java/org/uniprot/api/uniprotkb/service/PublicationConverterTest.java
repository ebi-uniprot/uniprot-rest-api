package org.uniprot.api.uniprotkb.service;

import org.junit.jupiter.api.Test;
import org.uniprot.store.indexer.uniprot.mockers.PublicationDocumentMocker;
import org.uniprot.store.search.document.publication.PublicationDocument;

import static org.junit.jupiter.api.Assertions.*;

class PublicationConverterTest {
    @Test
    void thing() {
        PublicationDocument document = PublicationDocumentMocker.create(1, 1);
        System.out.println(document);
    }
}
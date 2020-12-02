package org.uniprot.api.unirule.controller;

import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSolrStreamControllerIT;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.unirule.UniRuleRestApplication;
import org.uniprot.api.unirule.repository.UniRuleQueryRepository;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilderTest;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.unirule.UniRuleDocumentConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.unirule.UniRuleDocument;

/**
 * @author sahmad
 * @since 02/12/2020
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniRuleRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRuleController.class)
@ExtendWith(value = {SpringExtension.class})
class UniRuleStreamControllerIT extends AbstractSolrStreamControllerIT {

    @Autowired private UniRuleQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIRULE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.unirule;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getStreamPath() {
        return "/unirule/stream";
    }

    @Override
    protected int saveEntries() {
        int numberOfEntries = 12;
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
        return numberOfEntries;
    }

    private void saveEntry(int suffix) {
        UniRuleEntry entry = UniRuleEntryBuilderTest.createObject(2);
        UniRuleEntry uniRuleEntry = UniRuleControllerITUtils.updateValidValues(entry, suffix);
        UniRuleDocumentConverter docConverter = new UniRuleDocumentConverter();
        UniRuleDocument document = docConverter.convertToDocument(uniRuleEntry);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIRULE, document);
    }
}

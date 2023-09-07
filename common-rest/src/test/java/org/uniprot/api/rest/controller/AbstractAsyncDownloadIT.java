package org.uniprot.api.rest.controller;

public abstract class AbstractAsyncDownloadIT extends AbstractStreamControllerIT{
    /**
     * Move code of {@link AbstractDownloadIT} to old class AbstractUniProtKBDownloadIT in uniprotkb-rest module
     * Move code from the following classes:
        1. AsyncDownloadIntegrationTest
        2. AbstractUniProtKBDownloadIT
        3. Add required abstract methods to implement in UniProtKBAsyncDownloadIT or UniRefAsync or UniParcAsync

      After that
      1. Rename AsyncDownloadIntegrationTest to UniProtKBAsyncDownloadIT
      2. UniProtKBAsyncDownloadIT extends AbstractAsyncDownloadIT
      3. Add data creation code from AbstractUniProtKBDownloadIT to UniProtKBAsyncDownloadIT e.g. saveEntry, saveTaxonomy etc
      4. It will cause data creation code duplication in UniProtKBAsyncDownloadIT and UniProtKBDownloadControllerIT
      5. See point 5 in {@link AbstractDownloadControllerIT}
     */
}

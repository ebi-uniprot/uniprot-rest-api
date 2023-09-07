package org.uniprot.api.rest.controller;

public abstract class AbstractDownloadControllerIT extends AbstractAsyncDownloadIT {
    /**
     * Move code from the following classes:
     * 1. AbstractDownloadControllerIT of uniprotkb-rest
     * 2. Add abstract method for values which change e.g. query, fields, ids etc
     * 3. UniProtKBDownloadControllerIT will extend common's AbstractDownloadControllerIT
     * 4. Add test data creation code from AbstractUniProtKBDownloadIT to UniProtKBDownloadControllerIT e.g. saveEntry, saveTaxonomy etc
     * 5. To avoid data creation code, we can add a util class with static methods
     */

}

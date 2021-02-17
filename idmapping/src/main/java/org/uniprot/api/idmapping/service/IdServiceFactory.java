package org.uniprot.api.idmapping.service;

/**
 * @author sahmad
 * @created 16/02/2021
 */
public class IdServiceFactory {
    // TODO Fill a map with key as to and value as object of type BasicIdService
    // call from controller to get service
    public static BasicIdService getIdService(String to) {
        if ("uniprotkb".equals(to)) { // stub use enum in to and switch case
            return new UniProtKBIdService(null, null, null, null);
        } else if ("uniref".equals(to)) {
            return new UniRefIdService(null, null, null, null);
        } // etc
        return null;
    }

    private IdServiceFactory() {}
}

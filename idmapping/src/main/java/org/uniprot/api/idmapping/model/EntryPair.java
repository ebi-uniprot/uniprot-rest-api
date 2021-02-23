package org.uniprot.api.idmapping.model;


/**
 * @author sahmad
 * @created 17/02/2021
 */
public interface EntryPair<T> {
    String getFrom();

    T getTo();
}

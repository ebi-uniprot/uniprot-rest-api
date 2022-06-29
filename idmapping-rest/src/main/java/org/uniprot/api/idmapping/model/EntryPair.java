package org.uniprot.api.idmapping.model;

import java.io.Serializable;

/**
 * @author sahmad
 * @created 17/02/2021
 */
public interface EntryPair<T> extends Serializable {
    String getFrom();

    T getTo();
}

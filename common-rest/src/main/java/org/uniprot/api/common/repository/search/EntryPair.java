package org.uniprot.api.common.repository.search;

import java.io.Serializable;

/**
 * @author sahmad
 * @created 17/02/2021
 */
public interface EntryPair<T> extends Serializable {
    String getFrom();

    T getTo();
}

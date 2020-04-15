package org.uniprot.api.unisave.repository.domain;

/**
 * The status of an Entry Index.
 *
 * @author wudong
 */
public enum EntryIndexStatus {

    // it is a new one.
    N,
    // the entry is same as last release, no need to load.
    S,
    // the entry indexed need to be load.
    U
    // the entry indexed has been loaded.//it is not used. don't want to update entry index table.
    // Done

}

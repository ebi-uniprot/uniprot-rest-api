package org.uniprot.api.unisave.repository.domain;

/**
 * A abstract entry from one release. An entry can be identified by an Identifier (the Entry
 * Accession) and its entry version.
 *
 * @author wudong
 */
public interface Entry extends BasicEntryInfo {
    EntryContent getEntryContent();
}

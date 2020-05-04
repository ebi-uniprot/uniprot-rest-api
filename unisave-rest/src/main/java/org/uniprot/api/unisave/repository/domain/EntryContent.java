package org.uniprot.api.unisave.repository.domain;

/**
 * An Entry's Content.
 *
 * @author wudong
 */
public interface EntryContent {

    ContentTypeEnum getType();

    String getFullContent();

    String getDiffContent();

    Long getReferenceEntryId();
}

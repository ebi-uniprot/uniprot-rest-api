package org.uniprot.api.unisave.repository.domain;

/**
 * An Entry's Content.
 * <p/>
 * <p/>
 *
 * @author wudong
 */
public interface EntryContent {

	ContentTypeEnum getType();

	String getFullcontent();

	String getDiffcontent();

	Long getReferenceEntryId();

}

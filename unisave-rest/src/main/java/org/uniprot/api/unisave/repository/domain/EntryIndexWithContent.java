package org.uniprot.api.unisave.repository.domain;

/**
 * The Entry Index retrieved with the the previous entry's content in the
 * database (if it is existed).
 * <p/>
 * When Entry Index is retrieved with the content, diff can be build when using
 * the entry index to build new Entry which will be directly put into database
 * without query the db again.
 *
 * @author wudong
 */
public interface EntryIndexWithContent extends EntryIndex {

	/**
	 * The previous entry's content. This can be a diff or a full content
	 * depending on the type.
	 *
	 * @return
	 */
	String getContent();

	/**
	 * The content's type, Either 'Diff' or 'Full'.
	 *
	 * @return
	 */
	String getContentType();

	/**
	 * The previous entry's ID, if it is a full entry. otherwise it will be
	 * enpty.
	 *
	 * @return
	 */
	Long getContentEntryId();

	/**
	 * The reference content for the diff, if the content is a diff, otherwise
	 * it will be null.
	 *
	 * @return
	 */
	String getRefContent();

	/**
	 * The reference entryid for the diff, if the content is a diff, otherwise
	 * it will be null.
	 *
	 * @return
	 */
	Long getRefEntryContentId();

}

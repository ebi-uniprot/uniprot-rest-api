package org.uniprot.api.unisave.repository.domain.impl;

import com.google.common.base.Preconditions;
import org.uniprot.api.unisave.repository.domain.ContentTypeEnum;
import org.uniprot.api.unisave.repository.domain.Entry;
import org.uniprot.api.unisave.repository.domain.EntryContent;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

/**
 * The Entry Content that is embedded in the Entry table.
 *
 * @author wudong
 */
@Embeddable
public class EntryContentImpl implements EntryContent {

	/**
	 * The diff content stored in a vchar.
	 */
	@Column(name = "diff_content", nullable = true)
	private String diffcontent;

	/**
	 * The diff content's reference entry.
	 */
	//@ManyToOne(optional = true, fetch = FetchType.LAZY)
	//@JoinColumn(name = "full_content_entry_id", referencedColumnName = "entry_id", updatable = false, insertable = false)
	//@Transient;
	//private EntryImpl referenceEntry;

	/**
	 * The diff content's reference entry.
	 */
	@Column(name = "full_content_entry_id", nullable = true)
	private Long referenceEntry_ID;

	/**
	 * The full content stored in a full content.
	 */
	@Lob
	@Column(name = "full_content", nullable = true)
	private String fullcontent;

	@Override
	public ContentTypeEnum getType() {
		Preconditions.checkState(referenceEntry_ID != null
				|| fullcontent != null,
				"entrycontent cannot have both diff content and full content.");

		Preconditions.checkState(
				(referenceEntry_ID == null || fullcontent == null),
				"entrycontent cannot be both full and diff.");

		if (referenceEntry_ID != null)
			return ContentTypeEnum.Diff;
		else
			return ContentTypeEnum.Full;
	}

	@Override
	public String getFullcontent() {
		return fullcontent;
	}

	@Override
	public String getDiffcontent() {
		return diffcontent;
	}

	@Override
	public Long getReferenceEntryId() {
		return referenceEntry_ID;
	}

//    public void setReferenceEntry(EntryImpl referenceEntry) {
//	this.referenceEntry = referenceEntry;
//    }


	public void setDiffcontent(String diffcontent, Entry reference) {
		Preconditions.checkArgument(reference instanceof EntryImpl);
		EntryImpl ei = (EntryImpl) reference;
		// Preconditions.checkArgument(ei.getEntryid()!=0,
		// "The reference have been saved.");
		Preconditions
				.checkArgument(ei.getEntryContent().getType() == ContentTypeEnum.Full);

		setDiffcontent(diffcontent, ei.getEntryid());
	}

	public void setFullcontent(String fullcontent) {
		this.fullcontent = fullcontent;
		this.diffcontent = null;

		this.referenceEntry_ID = null;
	}

	@Override
	public String toString() {
		ContentTypeEnum type = this.getType();
		switch (type) {
			case Diff:
				return this.diffcontent;
			case Full:
				return this.getFullcontent();
		}
		return null;
	}

	public void setDiffcontent(String diff, Long refid) {
		Preconditions.checkArgument(refid != 0,
				"the referenced entry must has already been in database.");
		this.diffcontent = diff;
		this.referenceEntry_ID = refid;
		this.fullcontent = null;
	}
}

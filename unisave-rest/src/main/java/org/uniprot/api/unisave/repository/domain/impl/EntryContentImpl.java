package org.uniprot.api.unisave.repository.domain.impl;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

import org.uniprot.api.unisave.repository.domain.ContentTypeEnum;
import org.uniprot.api.unisave.repository.domain.EntryContent;

import com.google.common.base.Preconditions;

import lombok.Data;

/**
 * The Entry Content that is embedded in the Entry table.
 *
 * @author wudong
 */
@Embeddable
@Data
public class EntryContentImpl implements EntryContent {
    /** The diff content stored in a vchar. */
    @Column(name = "diff_content", nullable = true)
    private String diffContent;

    /** The diff content's reference entry. */
    @Column(name = "full_content_entry_id", nullable = true)
    private Long referenceEntryId;

    /** The full content stored in a full content. */
    @Lob
    @Column(name = "full_content", nullable = true)
    private String fullContent;

    @Override
    public ContentTypeEnum getType() {
        Preconditions.checkState(
                referenceEntryId != null || fullContent != null,
                "entrycontent cannot have both diff content and full content.");

        Preconditions.checkState(
                (referenceEntryId == null || fullContent == null),
                "entrycontent cannot be both full and diff.");

        if (referenceEntryId != null) return ContentTypeEnum.DIFF;
        else return ContentTypeEnum.FULL;
    }
}

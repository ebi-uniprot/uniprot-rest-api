package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.EntryIndexWithContent;

import com.google.common.base.Preconditions;

public class EntryIndexWithContentImpl extends EntryIndexImpl implements EntryIndexWithContent {

    private Long contentEntryId;
    private String content;
    private String contentType;
    private String refContent;
    private Long refEntryContentId;

    @Override
    public Long getContentEntryId() {
        return contentEntryId;
    }

    public void setContentEntryId(Long contentEntryId) {
        Preconditions.checkArgument(
                contentEntryId != null && contentEntryId != 0,
                "RefEntryId must existed in database.");
        this.contentEntryId = contentEntryId;
    }

    @Override
    public Long getRefEntryContentId() {
        return refEntryContentId;
    }

    public void setRefEntryContentId(Long refEntryId) {
        Preconditions.checkArgument(
                refEntryId != null && refEntryId != 0, "RefEntryId must existed in database.");

        this.refEntryContentId = refEntryId;
    }

    public void setContent(String string) {
        this.content = string;
    }

    public void setContentType(String string) {
        this.contentType = string;
    }

    public void setRefContent(String string) {
        this.refContent = string;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getRefContent() {
        return refContent;
    }

    public void setDiffContent(String string) {
        // TODO Auto-generated method stub

    }
}

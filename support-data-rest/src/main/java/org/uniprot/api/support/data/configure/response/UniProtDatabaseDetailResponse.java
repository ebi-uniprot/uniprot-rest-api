package org.uniprot.api.support.data.configure.response;

import java.util.List;

import lombok.Getter;

import org.uniprot.core.cv.xdb.UniProtDatabaseAttribute;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;

@Getter
public class UniProtDatabaseDetailResponse {
    private String name;
    private String displayName;
    private String category;
    private String uriLink;
    private List<UniProtDatabaseAttribute> attributes;
    private boolean implicit;
    private String linkedReason;
    private String idMappingName;

    private UniProtDatabaseDetailResponse(
            String name,
            String displayName,
            String category,
            String uriLink,
            List<UniProtDatabaseAttribute> attributes,
            boolean implicit,
            String linkedReason,
            String idMappingName) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.uriLink = uriLink;
        this.attributes = attributes;
        this.implicit = implicit;
        this.linkedReason = linkedReason;
        this.idMappingName = idMappingName;
    }

    public static UniProtDatabaseDetailResponse getUniProtDatabaseDetailResponse(
            UniProtDatabaseDetail dbDetail) {
        return new UniProtDatabaseDetailResponse(
                dbDetail.getName(),
                dbDetail.getDisplayName(),
                dbDetail.getCategory().getName(),
                dbDetail.getUriLink(),
                dbDetail.getAttributes(),
                dbDetail.isImplicit(),
                dbDetail.getLinkedReason(),
                dbDetail.getIdMappingName());
    }
}

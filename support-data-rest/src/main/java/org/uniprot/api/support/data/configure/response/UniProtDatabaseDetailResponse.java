package org.uniprot.api.support.data.configure.response;

import java.util.List;
import java.util.stream.Collectors;

import org.uniprot.core.cv.xdb.UniProtDatabaseAttribute;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UniProtDatabaseDetailResponse {
    private String name;
    private String displayName;
    private String category;
    private String uriLink;
    private List<UniProtDatabaseAttributeResponse> attributes;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean implicit;

    private String linkedReason;

    private UniProtDatabaseDetailResponse(
            String name,
            String displayName,
            String category,
            String uriLink,
            List<UniProtDatabaseAttributeResponse> attributes,
            boolean implicit,
            String linkedReason) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.uriLink = uriLink;
        this.attributes = attributes;
        this.implicit = implicit;
        this.linkedReason = linkedReason;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    static class UniProtDatabaseAttributeResponse {
        private String name;
        private String xmlTag;
        private String uriLink;

        public UniProtDatabaseAttributeResponse(String name, String xmlTag, String uriLink) {
            this.name = name;
            this.xmlTag = xmlTag;
            this.uriLink = uriLink;
        }

        static UniProtDatabaseAttributeResponse getUniProtDatabaseAttributeResponse(
                UniProtDatabaseAttribute attribute) {
            return new UniProtDatabaseAttributeResponse(
                    attribute.getName(), attribute.getXmlTag(), attribute.getUriLink());
        }

        static List<UniProtDatabaseAttributeResponse> getUniProtDatabaseAttributeResponses(
                List<UniProtDatabaseAttribute> attributes) {
            return attributes.stream()
                    .map(UniProtDatabaseAttributeResponse::getUniProtDatabaseAttributeResponse)
                    .collect(Collectors.toList());
        }
    }

    public static UniProtDatabaseDetailResponse getUniProtDatabaseDetailResponse(
            UniProtDatabaseDetail dbDetail) {
        return new UniProtDatabaseDetailResponse(
                dbDetail.getName(),
                dbDetail.getDisplayName(),
                dbDetail.getCategory().getName(),
                dbDetail.getUriLink(),
                UniProtDatabaseAttributeResponse.getUniProtDatabaseAttributeResponses(
                        dbDetail.getAttributes()),
                dbDetail.isImplicit(),
                dbDetail.getLinkedReason());
    }
}

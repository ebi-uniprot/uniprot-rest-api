package org.uniprot.api.idmapping.controller.request;

import lombok.*;

import java.util.List;

/**
 * Created 08/02/2021
 *
 * @author Edd
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class IDMappingRequest {
    private String taxId;
    private String taxOff;
    private String from;
    private String to;
    @Singular private List<String> ids;
    private String async;

    public static class IDMappingRequestBuilder {
        private String taxOff = "YES";
        private String async = "NO";
    }
}

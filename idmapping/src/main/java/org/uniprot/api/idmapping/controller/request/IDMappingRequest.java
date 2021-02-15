package org.uniprot.api.idmapping.controller.request;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Created 08/02/2021
 *
 * @author Edd
 */
@Builder
@Data
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

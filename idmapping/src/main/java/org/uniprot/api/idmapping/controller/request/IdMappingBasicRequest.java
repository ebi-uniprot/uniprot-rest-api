package org.uniprot.api.idmapping.controller.request;

import lombok.Data;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Data
public class IdMappingBasicRequest {
    private String from;
    private String to;
    private String ids;
    private String fields;
    private String sort;
}

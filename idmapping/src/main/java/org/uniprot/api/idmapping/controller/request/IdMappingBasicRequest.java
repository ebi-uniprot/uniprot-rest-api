package org.uniprot.api.idmapping.controller.request;

import lombok.*;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Data
@EqualsAndHashCode
public class IdMappingBasicRequest {
    private String from;
    private String to;
    private String ids;
    private String taxId;
    private String fields;
    private String sort;
}

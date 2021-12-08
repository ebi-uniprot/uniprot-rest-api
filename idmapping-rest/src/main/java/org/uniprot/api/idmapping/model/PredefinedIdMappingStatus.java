package org.uniprot.api.idmapping.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

/**
 * @author sahmad
 * @created 07/12/2021
 */
@Getter
public enum PredefinedIdMappingStatus {
    FACET_WARNING(20, "Filters are not supported for mapping results with IDs more than "),
    ENRICHMENT_WARNING(21,  "UniProt data enrichment is not supported for mapping results with \"mapped to\" IDs more than "),
    LIMIT_EXCEED_ERROR(40, "Id Mapping API is not supported for mapping results with \"mapped to\" IDs more than ")
    ;
    private String message;
    private int code;

    PredefinedIdMappingStatus(int code, String message) {
        this.code = code;
        this.message =  message;
    }
}

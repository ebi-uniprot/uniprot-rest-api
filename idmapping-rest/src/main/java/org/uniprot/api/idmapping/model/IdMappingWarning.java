package org.uniprot.api.idmapping.model;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * @author sahmad
 * @created 07/12/2021
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum IdMappingWarning {
    FACET(1, "Filters are not supported for mapping results with over 10,000 IDs."),
    ENRICHMENT(2,  "UniProt data enrichment is not supported for mapping results with over 100,000 \"mapped to\" IDs.")
    ;
    private String message;
    private int code;

    IdMappingWarning(int code, String message) {
        this.code = code;
        this.message =  message;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}

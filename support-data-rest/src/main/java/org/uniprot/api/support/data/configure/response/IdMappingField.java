package org.uniprot.api.support.data.configure.response;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

/**
 * @author sahmad
 * @created 15/03/2021
 */
@Data
@Builder(toBuilder = true)
public class IdMappingField implements Serializable {
    private String groupName;
    private String displayName;
    private String name;
    private boolean from;
    private boolean to;
    private Integer ruleId;
}

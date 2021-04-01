package org.uniprot.api.support.data.configure.response;

import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_STR;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 * @author sahmad
 * @created 15/03/2021
 */
@Data
@Builder(toBuilder = true)
public class IdMappingField implements Serializable {
    private static final long serialVersionUID = 2187523299497519033L;
    @Singular private List<Group> groups;
    private List<Rule> rules;

    @Data
    @Builder(toBuilder = true)
    public static class Group implements Serializable {
        private static final long serialVersionUID = 4229413172365901286L;
        private String groupName;
        private List<Field> items;
    }

    @Data
    @Builder(toBuilder = true)
    public static class Field implements Serializable {
        private static final long serialVersionUID = 3296274672007139133L;
        private String displayName;
        private String name;
        private boolean from;
        private boolean to;
        private Integer ruleId;
    }

    @Data
    @Builder(toBuilder = true)
    public static class Rule implements Serializable {
        private static final long serialVersionUID = 1847092547405960734L;
        private Integer ruleId;
        @Singular private List<String> tos;
        @Builder.Default private String defaultTo = UNIPROTKB_STR;
        private boolean taxonId;
    }
}

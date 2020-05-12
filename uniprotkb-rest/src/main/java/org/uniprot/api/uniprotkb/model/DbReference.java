package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created 05/05/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class DbReference {
    private String type;
    private String id;
    private Map<String, String> properties;
    private String isoform;
    private List<Evidence> evidences;
}

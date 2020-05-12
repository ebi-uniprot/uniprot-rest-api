package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Created 05/05/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class DbReferenceObject {
    private String name;
    private String id;
    private String url;
    private String alternativeUrl;
    private Boolean reviewed;
}

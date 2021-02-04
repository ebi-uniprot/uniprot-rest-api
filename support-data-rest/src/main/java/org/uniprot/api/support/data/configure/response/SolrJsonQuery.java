package org.uniprot.api.support.data.configure.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SolrJsonQuery {

    private String type;

    private String queryOperator;

    private String field;

    private String value;

    private String from;
    private Boolean fromInclude;

    private String to;
    private Boolean toInclude;

    List<SolrJsonQuery> booleanQuery;
}

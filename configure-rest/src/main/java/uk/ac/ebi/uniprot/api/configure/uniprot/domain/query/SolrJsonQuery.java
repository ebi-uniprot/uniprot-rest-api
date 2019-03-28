package uk.ac.ebi.uniprot.api.configure.uniprot.domain.query;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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

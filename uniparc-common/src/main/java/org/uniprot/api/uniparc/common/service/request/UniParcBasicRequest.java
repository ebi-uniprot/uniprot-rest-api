package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
public class UniParcBasicRequest implements BasicRequest {

    public static final String PROTEOME_COMPONENT = "proteomecomponent";

    @Parameter(description = QUERY_UNIPARC_DESCRIPTION, example = QUERY_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    protected String query;

    @Parameter(description = SORT_UNIPARC_DESCRIPTION, example = SORT_UNIPARC_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String sort;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String fields;

    @Parameter(hidden = true)
    private String format;

    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }

    public void setQuery(String query) {
        this.query = query;
        if (query.contains(PROTEOME_COMPONENT)) {
            Query parsedQuery = ValidSolrQueryFields.QueryFieldValidator.getParsedQuery(query);
            if (parsedQuery instanceof BooleanQuery booleanQuery) {
                List<BooleanClause> clauses = booleanQuery.clauses();
                Map<String, String> queryFields =
                        clauses.stream()
                                .map(clause -> clause.getQuery().toString().split(":"))
                                .collect(Collectors.toMap(split -> split[0], split -> split[1]));
                BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
                for (BooleanClause clause : clauses) {
                    String fieldName = clause.getQuery().toString().split(":")[0];
                    if (PROTEOME_COMPONENT.equals(fieldName)) {
                        String proteomeComponent =
                                StringUtils.unwrap(queryFields.get(PROTEOME_COMPONENT), '"');
                        String proteomeId = queryFields.get("proteome");
                        String proteomeComponentWithAddedProteome =
                                StringUtils.wrap(proteomeId + " " + proteomeComponent, '"');
                        queryBuilder.add(
                                new TermQuery(
                                        new Term(
                                                PROTEOME_COMPONENT,
                                                proteomeComponentWithAddedProteome)),
                                clause.getOccur());
                    } else {
                        queryBuilder.add(clause);
                    }
                }
                this.query = queryBuilder.build().toString();
            }
        }
    }
}

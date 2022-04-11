package org.uniprot.api.uniprotkb.controller.request;

import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SortFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
public class UniProtKBBasicRequest {

  @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "uniprotkb-search-fields.json")
  @Parameter(description = "Criteria to search the proteins. It can take any valid solr query.")
  @NotNull(message = "{search.required}")
  @ValidSolrQuerySyntax(message = "{search.invalid.query}")
  @ValidSolrQueryFields(
      uniProtDataType = UniProtDataType.UNIPROTKB,
      messagePrefix = "search.uniprot")
  private String query;

  @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniprotkb-return-fields.json")
  @Parameter(description = "Comma separated list of fields to be returned in response")
  @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
  private String fields;

  @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "uniprotkb-search-fields.json")
  @Parameter(description = "Name of the field to be sorted on")
  @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPROTKB)
  private String sort;

  @Parameter(description = "Flag to include Isoform or not")
  @Pattern(
      regexp = "true|false",
      flags = {Pattern.Flag.CASE_INSENSITIVE},
      message = "{search.invalid.includeIsoform}")
  private String includeIsoform;

  public boolean isIncludeIsoform() {
    return Boolean.parseBoolean(includeIsoform);
  }


}

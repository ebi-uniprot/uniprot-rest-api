package org.uniprot.api.uniprotkb.common.service.precomputed;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@ParameterObject
public class PrecomputedAnnotationStreamByProteomeRequest implements StreamRequest, BasicRequest {
    private static final String TAXONOMY_ID_STR = "taxonomy_id";

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(regexp = "^true$|^false$", message = "{search.uniprot.invalid.download}")
    private String download;

    @Parameter(
            description = PROTEOME_UPID_UNIPARC_DESCRIPTION,
            example = PROTEOME_UPID_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    @Pattern(
            regexp = FieldRegexConstants.PROTEOME_ID_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.upid.value}")
    private String upId;

    @Parameter(description = ID_TAX_DESCRIPTION, example = ID_TAX_EXAMPLE)
    @Pattern(
            regexp = FieldRegexConstants.TAXONOMY_ID_REGEX,
            message = "{search.taxonomy.invalid.id}")
    private String taxonomyId;

    @Parameter(description = SORT_DESCRIPTION, example = "accession asc")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.PRECOMPUTED_ANNOTATION)
    private String sort;

    @Parameter(hidden = true)
    @ValidReturnFields(uniProtDataType = UniProtDataType.PRECOMPUTED_ANNOTATION)
    private String fields;

    @Parameter(hidden = true)
    private String format;

    @Override
    public String getQuery() {
        return TAXONOMY_ID_STR + ":" + this.upId;
    }

    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}

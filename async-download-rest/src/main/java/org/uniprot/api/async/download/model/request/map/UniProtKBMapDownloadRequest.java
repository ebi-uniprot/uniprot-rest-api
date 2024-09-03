package org.uniprot.api.async.download.model.request.map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.FIELDS_UNIPROTKB_EXAMPLE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.async.download.model.request.ValidDownloadRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.CustomConstraintGroup;
import org.uniprot.api.rest.validation.ValidAsyncDownloadFormats;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidTSVAndXLSFormatOnlyFields;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBStreamRequest;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.uniprot.store.config.UniProtDataType;

@Data
@EqualsAndHashCode(callSuper = true)
@ValidDownloadRequest(groups = CustomConstraintGroup.class)
@ParameterObject
public class UniProtKBMapDownloadRequest extends UniProtKBStreamRequest
        implements MapDownloadRequest {

    @ValidAsyncDownloadFormats(
            formats = {
                FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE
            })
    @Parameter(description = FORMAT_UNIPARC_DESCRIPTION, example = FORMAT_UNIREF_EXAMPLE)
    private String format;

    private boolean force;

    @Parameter(description = FIELDS_UNIREF_DESCRIPTION, example = FIELDS_UNIREF_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    @ValidTSVAndXLSFormatOnlyFields(fieldPattern = "xref_.*_full")
    private String fields;

    @Parameter(hidden = true)
    private String downloadJobId;

    private String to;

    @Override
    public String getFrom() {
        return "UniProtKB";
    }

    @Override
    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}

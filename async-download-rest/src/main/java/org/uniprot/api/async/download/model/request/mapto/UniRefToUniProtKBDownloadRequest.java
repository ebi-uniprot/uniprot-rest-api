package org.uniprot.api.async.download.model.request.mapto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.async.download.model.request.ValidDownloadRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.CustomConstraintGroup;
import org.uniprot.api.rest.validation.ValidAsyncDownloadFormats;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidTSVAndXLSFormatOnlyFields;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;
import org.uniprot.store.config.UniProtDataType;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.store.search.SolrCollection.uniprot;
import static org.uniprot.store.search.SolrCollection.uniref;

@Data
@EqualsAndHashCode(callSuper = true)
@ValidDownloadRequest(groups = CustomConstraintGroup.class)
@ParameterObject
public class UniRefToUniProtKBDownloadRequest extends UniRefStreamRequest
        implements MapToDownloadRequest {

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
    @Parameter(description = FORMAT_UNIPROTKB_DESCRIPTION, example = FORMAT_UNIPROTKB_EXAMPLE)
    private String format;

    private boolean force;

    @Parameter(description = FIELDS_UNIPROTKB_DESCRIPTION, example = FIELDS_UNIPROTKB_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    @ValidTSVAndXLSFormatOnlyFields(fieldPattern = "xref_.*_full")
    private String fields;

    @Parameter(hidden = true)
    private String downloadJobId;

    @Override
    public String getTo() {
        return uniprot.name();
    }

    @Override
    public String getFrom() {
        return uniref.name();
    }

    @Override
    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}

package org.uniprot.api.async.download.model.request.uniref;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.FORMAT_UNIREF_DESCRIPTION;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.FORMAT_UNIREF_EXAMPLE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.ValidAsyncDownloadFormats;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniRefDownloadRequest extends UniRefStreamRequest
        implements SolrStreamDownloadRequest {

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
    @Parameter(description = FORMAT_UNIREF_DESCRIPTION, example = FORMAT_UNIREF_EXAMPLE)
    private String format;

    @Parameter(hidden = true)
    private boolean force;

    @Parameter(hidden = true)
    private String downloadJobId;

    @Override
    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}

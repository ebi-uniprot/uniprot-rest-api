package org.uniprot.api.async.download.refactor.request.uniprotkb;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.FORMAT_UNIPROTKB_DESCRIPTION;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.FORMAT_UNIPROTKB_EXAMPLE;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.async.download.model.common.ValidDownloadRequest;
import org.uniprot.api.async.download.refactor.request.SolrStreamDownloadRequest;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.CustomConstraintGroup;
import org.uniprot.api.rest.validation.ValidAsyncDownloadFormats;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBStreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

@Data
@EqualsAndHashCode(callSuper = true)
@ValidDownloadRequest(groups = CustomConstraintGroup.class)
@ParameterObject
public class UniProtKBDownloadRequest extends UniProtKBStreamRequest
        implements SolrStreamDownloadRequest {
    @ValidAsyncDownloadFormats(
            formats = {
                UniProtMediaType.TSV_MEDIA_TYPE_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                UniProtMediaType.FF_MEDIA_TYPE_VALUE,
                UniProtMediaType.LIST_MEDIA_TYPE_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                UniProtMediaType.FASTA_MEDIA_TYPE_VALUE,
                UniProtMediaType.GFF_MEDIA_TYPE_VALUE,
                UniProtMediaType.RDF_MEDIA_TYPE_VALUE,
                UniProtMediaType.TURTLE_MEDIA_TYPE_VALUE,
                UniProtMediaType.N_TRIPLES_MEDIA_TYPE_VALUE,
                UniProtMediaType.HDF5_MEDIA_TYPE_VALUE
            })
    @Parameter(description = FORMAT_UNIPROTKB_DESCRIPTION, example = FORMAT_UNIPROTKB_EXAMPLE)
    private String format;

    @Parameter(hidden = true)
    private boolean force;

    private String jobId;

    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}
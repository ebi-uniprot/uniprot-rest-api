package org.uniprot.api.uniprotkb.controller.request;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.validation.ValidAsyncDownloadContentTypes;

@Data
@EqualsAndHashCode(callSuper = true)
public class UniProtKBDownloadRequest extends UniProtKBStreamRequest implements DownloadRequest {

    @ValidAsyncDownloadContentTypes(
            contentTypes = {
                TSV_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE
            })
    private String contentType;
}

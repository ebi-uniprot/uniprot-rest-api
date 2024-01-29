package org.uniprot.api.uniref.common.queue;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.ValidAsyncDownloadFormats;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;

@Data
@EqualsAndHashCode(callSuper = true)
public class UniRefDownloadRequest extends UniRefStreamRequest implements DownloadRequest {

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
    private String format;

    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}

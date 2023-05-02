package org.uniprot.api.uniprotkb.controller.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.validation.ValidAsyncDownloadFormats;

import java.util.Objects;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class UniProtKBDownloadRequest extends UniProtKBStreamRequest implements DownloadRequest {

    @ValidAsyncDownloadFormats(
            formats = {
                TSV_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                FF_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                GFF_MEDIA_TYPE_VALUE,
                RDF_MEDIA_TYPE_VALUE,
                TURTLE_MEDIA_TYPE_VALUE,
                N_TRIPLES_MEDIA_TYPE_VALUE,
                HDF5_MEDIA_TYPE_VALUE
            })
    private String format;

    public void setFormat(String format) {
        String longFormat;
        try {
            longFormat = UniProtMediaType.getMediaTypeForFileExtension(format).toString();
        } catch (IllegalArgumentException ile) {
            longFormat = null;
        }
        this.format = Objects.nonNull(longFormat) ? longFormat : format;
    }
}

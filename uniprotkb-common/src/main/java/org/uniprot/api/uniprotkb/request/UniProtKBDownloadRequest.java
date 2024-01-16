package org.uniprot.api.uniprotkb.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.CustomConstraintGroup;
import org.uniprot.api.rest.validation.ValidAsyncDownloadFormats;
import org.uniprot.api.rest.validation.ValidDownloadRequest;

@Data
@EqualsAndHashCode(callSuper = true)
@ValidDownloadRequest(groups = CustomConstraintGroup.class)
public class UniProtKBDownloadRequest extends UniProtKBStreamRequest implements DownloadRequest {
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
    private String format;

    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}

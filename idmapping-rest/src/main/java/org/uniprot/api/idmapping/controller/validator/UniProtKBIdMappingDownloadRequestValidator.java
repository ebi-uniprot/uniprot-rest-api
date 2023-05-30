package org.uniprot.api.idmapping.controller.validator;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.List;

import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

public class UniProtKBIdMappingDownloadRequestValidator
        extends AbstractIdMappingDownloadRequestValidator {

    static ReturnFieldConfig returnFieldConfig =
            ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);

    public static final List<String> VALID_FORMATS =
            List.of(
                    FASTA_MEDIA_TYPE_VALUE,
                    TSV_MEDIA_TYPE_VALUE,
                    APPLICATION_JSON_VALUE,
                    APPLICATION_XML_VALUE,
                    RDF_MEDIA_TYPE_VALUE,
                    TURTLE_MEDIA_TYPE_VALUE,
                    N_TRIPLES_MEDIA_TYPE_VALUE,
                    FF_MEDIA_TYPE_VALUE,
                    GFF_MEDIA_TYPE_VALUE,
                    LIST_MEDIA_TYPE_VALUE);

    @Override
    protected String getType() {
        return "UniProtKB";
    }

    @Override
    protected ReturnFieldConfig getReturnFieldConfig() {
        return returnFieldConfig;
    }

    @Override
    protected List<String> getValidFormats() {
        return VALID_FORMATS;
    }
}

package org.uniprot.api.idmapping.controller.validator;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.List;

import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

public class UniParcIdMappingDownloadRequestValidator
        extends AbstractIdMappingDownloadRequestValidator {

    static ReturnFieldConfig returnFieldConfig =
            ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC);

    public static final List<String> validFormat =
            List.of(
                    FASTA_MEDIA_TYPE_VALUE,
                    TSV_MEDIA_TYPE_VALUE,
                    APPLICATION_JSON_VALUE,
                    APPLICATION_XML_VALUE,
                    RDF_MEDIA_TYPE_VALUE,
                    LIST_MEDIA_TYPE_VALUE);

    @Override
    protected String getType() {
        return "UniParc";
    }

    @Override
    protected ReturnFieldConfig getReturnFieldConfig() {
        return returnFieldConfig;
    }

    @Override
    protected List<String> getValidFormats() {
        return validFormat;
    }
}

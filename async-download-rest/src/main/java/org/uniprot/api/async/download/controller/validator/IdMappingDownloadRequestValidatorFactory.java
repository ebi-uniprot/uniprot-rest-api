package org.uniprot.api.async.download.controller.validator;

import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

public class IdMappingDownloadRequestValidatorFactory {

    public IdMappingDownloadRequestValidator create(String type) {
        IdMappingDownloadRequestValidator validator;
        switch (type) {
            case IdMappingFieldConfig.UNIPROTKB_STR, IdMappingFieldConfig.UNIPROTKB_SWISS_STR:
                validator = new UniProtKBIdMappingDownloadRequestValidator();
                break;
            case IdMappingFieldConfig.UNIPARC_STR:
                validator = new UniParcIdMappingDownloadRequestValidator();
                break;
            case IdMappingFieldConfig.UNIREF_50_STR:
            case IdMappingFieldConfig.UNIREF_90_STR:
            case IdMappingFieldConfig.UNIREF_100_STR:
                validator = new UniRefIdMappingDownloadRequestValidator();
                break;
            default:
                throw new InvalidRequestException(
                        "The IdMapping 'to' parameter value is invalid. It should be '"
                                + IdMappingFieldConfig.UNIPROTKB_STR
                                + "', '"
                                + IdMappingFieldConfig.UNIPROTKB_SWISS_STR
                                + "', '"
                                + IdMappingFieldConfig.UNIPARC_STR
                                + "', '"
                                + IdMappingFieldConfig.UNIREF_50_STR
                                + "', '"
                                + IdMappingFieldConfig.UNIREF_90_STR
                                + "' or '"
                                + IdMappingFieldConfig.UNIREF_100_STR
                                + "'.");
        }
        return validator;
    }
}

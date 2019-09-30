package org.uniprot.api.rest.controller.param;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/** @author lgonzales */
@Data
@Builder
public class GetIdContentTypeParam {

    private String id;

    @Singular List<ContentTypeParam> contentTypeParams;
}

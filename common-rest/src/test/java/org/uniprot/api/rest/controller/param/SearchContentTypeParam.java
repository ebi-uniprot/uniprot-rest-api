package org.uniprot.api.rest.controller.param;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 *
 * @author lgonzales
 */
@Data
@Builder
public class SearchContentTypeParam {

    private String query;

    @Singular
    List<ContentTypeParam> contentTypeParams;

}

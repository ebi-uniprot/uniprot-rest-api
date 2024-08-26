package org.uniprot.api.async.download.model.request.map;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.ValidAsyncDownloadFormats;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;

import javax.validation.constraints.NotNull;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

public interface MapDownloadRequest extends DownloadRequest, SolrStreamDownloadRequest {

    public String getFrom();

    public String getTo();
}

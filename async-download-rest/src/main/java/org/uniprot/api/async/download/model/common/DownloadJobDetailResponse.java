package org.uniprot.api.async.download.model.common;

import java.util.List;

import lombok.Data;

import org.uniprot.api.common.repository.search.ProblemPair;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author lgonzales
 * @since 23/04/2021
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DownloadJobDetailResponse {
    private String redirectURL;
    private List<ProblemPair> warnings;
    private List<ProblemPair> errors;
    private String query;
    private String fields;
    private String sort;
    private String format;
}
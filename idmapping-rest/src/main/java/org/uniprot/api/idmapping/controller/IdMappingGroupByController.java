package org.uniprot.api.idmapping.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.idmapping.common.service.IdMappingJobService.IDMAPPING_PATH;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.PredefinedAPIStatus.FACET_WARNING;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdGroupByRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.UniProtKBIdGroupByService;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdGroupByECService;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdGroupByGOService;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdGroupByKeywordService;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdGroupByTaxonomyService;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = TAG_IDMAPPING_RESULT, description = TAG_IDMAPPING_RESULT_DESC)
@RestController
@RequestMapping(value = IDMAPPING_PATH)
@Validated
public class IdMappingGroupByController {
    private final UniProtKBIdGroupByTaxonomyService uniProtKBIdGroupByTaxonomyService;
    private final UniProtKBIdGroupByKeywordService uniProtKBIdGroupByKeywordService;
    private final UniProtKBIdGroupByGOService uniProtKBIdGroupByGOService;
    private final UniProtKBIdGroupByECService uniProtKBIdGroupByECService;
    private final IdMappingJobCacheService cacheService;
    private final Integer maxIdMappingToIdsWithFacetsCount;

    public IdMappingGroupByController(
            UniProtKBIdGroupByTaxonomyService uniProtKBIdGroupByTaxonomyService,
            UniProtKBIdGroupByKeywordService uniProtKBIdGroupByKeywordService,
            UniProtKBIdGroupByGOService uniProtKBIdGroupByGOService,
            UniProtKBIdGroupByECService uniProtKBIdGroupByECService,
            IdMappingJobCacheService cacheService,
            @Value("${mapping.max.to.ids.with.facets.count:#{null}}")
                    Integer maxIdMappingToIdsWithFacetsCount) {
        this.uniProtKBIdGroupByTaxonomyService = uniProtKBIdGroupByTaxonomyService;
        this.uniProtKBIdGroupByKeywordService = uniProtKBIdGroupByKeywordService;
        this.uniProtKBIdGroupByGOService = uniProtKBIdGroupByGOService;
        this.uniProtKBIdGroupByECService = uniProtKBIdGroupByECService;
        this.cacheService = cacheService;
        this.maxIdMappingToIdsWithFacetsCount = maxIdMappingToIdsWithFacetsCount;
    }

    @GetMapping(
            value = "/{jobId}/groups/taxonomy",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            hidden = true,
            summary = ID_MAPPING_GROUP_BY_TAXONOMY_RESULT_OPERATION,
            description = GROUP_TAXONOMY_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "group by results",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GroupByResult.class))
                        })
            })
    public ResponseEntity<GroupByResult> resultsByTaxonomy(
            @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable String jobId,
            @Parameter(description = QUERY_UNIPROTKB_TAXONOMY_DESCRIPTION)
                    @RequestParam(value = "query", required = false, defaultValue = "*:*")
                    String query,
            @Parameter(description = GROUP_PARENT_DESCRIPTION)
                    @Pattern(
                            regexp = FieldRegexConstants.TAXONOMY_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{groupby.taxonomy.invalid.id}")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return getGroupByResult(jobId, query, parent, uniProtKBIdGroupByTaxonomyService);
    }

    @GetMapping(
            value = "/{jobId}/groups/keyword",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            hidden = true,
            summary = ID_MAPPING_GROUP_BY_KEYWORD_RESULT_OPERATION,
            description = GROUP_KEYWORD_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "group by results",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GroupByResult.class))
                        })
            })
    public ResponseEntity<GroupByResult> resultsByKeyword(
            @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable String jobId,
            @Parameter(description = QUERY_UNIPROTKB_KEYWORD_DESCRIPTION)
                    @RequestParam(value = "query", required = false, defaultValue = "*:*")
                    String query,
            @Parameter(description = GROUP_PARENT_DESCRIPTION)
                    @Pattern(
                            regexp = FieldRegexConstants.KEYWORD_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{groupby.keyword.invalid.id}")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return getGroupByResult(jobId, query, parent, uniProtKBIdGroupByKeywordService);
    }

    @GetMapping(
            value = "/{jobId}/groups/go",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            hidden = true,
            summary = ID_MAPPING_GROUP_BY_GO_RESULT_OPERATION,
            description = GROUP_GO_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "group by results",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GroupByResult.class))
                        })
            })
    public ResponseEntity<GroupByResult> resultsByGO(
            @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable String jobId,
            @Parameter(description = QUERY_UNIPROTKB_GO_DESCRIPTION)
                    @RequestParam(value = "query", required = false, defaultValue = "*:*")
                    String query,
            @Parameter(description = GROUP_PARENT_DESCRIPTION)
                    @Pattern(
                            regexp = FieldRegexConstants.GO_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{groupby.go.invalid.id}")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return getGroupByResult(jobId, query, parent, uniProtKBIdGroupByGOService);
    }

    @GetMapping(
            value = "/{jobId}/groups/ec",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            hidden = true,
            summary = ID_MAPPING_GROUP_BY_EC_RESULT_OPERATION,
            description = GROUP_EC_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        description = "group by results",
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GroupByResult.class))
                        })
            })
    public ResponseEntity<GroupByResult> resultsByEC(
            @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION) @PathVariable String jobId,
            @Parameter(description = QUERY_UNIPROTKB_EC_DESCRIPTION)
                    @RequestParam(value = "query", required = false, defaultValue = "*:*")
                    String query,
            @Parameter(description = GROUP_PARENT_DESCRIPTION)
                    @Pattern(
                            regexp = FieldRegexConstants.EC_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{groupby.ec.invalid.id}")
                    @RequestParam(value = "parent", required = false)
                    String parent) {
        return getGroupByResult(jobId, query, parent, uniProtKBIdGroupByECService);
    }

    private ResponseEntity<GroupByResult> getGroupByResult(
            String jobId,
            String query,
            String parent,
            UniProtKBIdGroupByService<?> idGroupByService) {
        IdMappingJob completedJob = cacheService.getCompletedJobAsResource(jobId);
        // validate the mapped ids size
        validatedMappedIdsLimit(completedJob.getIdMappingResult());

        UniProtKBIdGroupByRequest idMappingGroupByRequest =
                new UniProtKBIdGroupByRequest(query, parent, getToIds(completedJob));
        GroupByResult groupByResult = idGroupByService.getGroupByResult(idMappingGroupByRequest);
        return new ResponseEntity<>(groupByResult, HttpStatus.OK);
    }

    private static List<String> getToIds(IdMappingJob completedJob) {
        return completedJob.getIdMappingResult().getMappedIds().stream()
                .map(IdMappingStringPair::getTo)
                .collect(Collectors.toList());
    }

    private void validatedMappedIdsLimit(IdMappingResult result) {
        if (Utils.notNullNotEmpty(result.getMappedIds())
                && result.getMappedIds().size() > this.maxIdMappingToIdsWithFacetsCount) {
            throw new InvalidRequestException(
                    FACET_WARNING.getErrorMessage(this.maxIdMappingToIdsWithFacetsCount));
        }
    }
}

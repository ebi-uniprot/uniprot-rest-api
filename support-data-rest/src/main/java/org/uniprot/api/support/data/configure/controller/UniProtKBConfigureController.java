package org.uniprot.api.support.data.configure.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.openapi.OpenApiConstants.*;
import static org.uniprot.api.support.data.configure.response.AdvancedSearchTerm.PATH_PREFIX_FOR_AUTOCOMPLETE_SEARCH_FIELDS;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtDatabaseDetailResponse;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.api.support.data.configure.service.UniProtKBConfigureService;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.uniprotkb.evidence.EvidenceDatabaseDetail;
import org.uniprot.store.search.domain.DatabaseGroup;
import org.uniprot.store.search.domain.EvidenceGroup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = TAG_CONFIG, description = TAG_CONFIG_DESC)
@RestController
@RequestMapping("/configure/uniprotkb")
public class UniProtKBConfigureController {
    private final UniProtKBConfigureService service;
    private String searchTermResponse;

    public UniProtKBConfigureController(UniProtKBConfigureService service) {
        this.service = service;
    }

    @Operation(
            summary = CONFIG_UNIPROTKB_SEARCH_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            AdvancedSearchTerm
                                                                                    .class)))
                        })
            })
    @GetMapping("/search-fields")
    public List<AdvancedSearchTerm> getUniProtSearchTerms() {
        return service.getUniProtSearchItems(PATH_PREFIX_FOR_AUTOCOMPLETE_SEARCH_FIELDS);
    }

    @Operation(
            summary = CONFIG_UNIPROTKB_ANNOTATION_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            EvidenceGroup.class)))
                        })
            })
    @GetMapping("/annotation_evidences")
    public List<EvidenceGroup> getUniProtAnnotationEvidences() {
        return service.getAnnotationEvidences();
    }

    @Operation(
            summary = CONFIG_UNIPROTKB_GO_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            EvidenceGroup.class)))
                        })
            })
    @GetMapping("/go_evidences")
    public List<EvidenceGroup> getUniProtGoEvidences() {
        return service.getGoEvidences();
    }

    @Operation(
            summary = CONFIG_UNIPROTKB_DATABASE_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            DatabaseGroup.class)))
                        })
            })
    @GetMapping("/databases")
    public List<DatabaseGroup> getUniProtDatabase() {
        return service.getDatabases();
    }

    @Operation(
            summary = CONFIG_UNIPROTKB_FIELDS_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniProtReturnField
                                                                                    .class)))
                        })
            })
    @GetMapping("/result-fields")
    public List<UniProtReturnField> getResultFields2() {
        return service.getResultFields2();
    }

    @Operation(
            summary = CONFIG_UNIPROTKB_ALL_DATABASE_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            UniProtDatabaseDetail
                                                                                    .class)))
                        })
            })
    @GetMapping("/allDatabases")
    public List<UniProtDatabaseDetailResponse> getUniProtAllDatabase() {
        return service.getAllDatabases();
    }

    @Operation(
            summary = CONFIG_UNIPROTKB_EVID_DATABASE_OPERATION,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            EvidenceDatabaseDetail
                                                                                    .class)))
                        })
            })
    @GetMapping("/evidenceDatabases")
    public List<EvidenceDatabaseDetail> getUniProtEvidenceDatabase() {
        return service.getEvidenceDatabases();
    }
}

package org.uniprot.api.support.data.configure.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.api.support.data.configure.service.UniProtKBConfigureService;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.uniprotkb.evidence.EvidenceDatabaseDetail;
import org.uniprot.store.search.domain.DatabaseGroup;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.FieldGroup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "Configuration",
        description = "These services provide configuration data used in UniProt website")
@RestController
@RequestMapping("/configure/uniprotkb")
public class UniProtKBConfigureController {
    private final UniProtKBConfigureService service;
    private String searchTermResponse;

    public UniProtKBConfigureController(UniProtKBConfigureService service) {
        this.service = service;
    }

    // FIXME Delete this method once UI team starts consuming response of api search-terms.
    //  See method getUniProtSearchTerms
    @Operation(hidden = true)
    @GetMapping("/search_terms")
    public ResponseEntity<String> getUniProtSearchTermsTemp() throws IOException {
        if (searchTermResponse == null) {
            InputStream in = getClass().getResourceAsStream("/search_terms-response.json");
            searchTermResponse = StreamUtils.copyToString(in, Charset.defaultCharset());
        }

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return new ResponseEntity<>(searchTermResponse, httpHeaders, HttpStatus.OK);
    }

    @Operation(
            summary = "List of search fields available to use in UniProtKB query.",
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
        return service.getUniProtSearchItems();
    }

    @Operation(
            summary = "List of annotation evidences available in UniProtKB search.",
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
            summary = "List of GO annotation evidences available in UniProtKB search.",
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
            summary = "List of databases available in UniProtKB search.",
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

    @Operation(hidden = true)
    @GetMapping("/resultfields")
    public List<FieldGroup> getResultFields() {
        return service.getResultFields();
    }

    @Operation(
            summary = "List of return fields available in UniProtKB search.",
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
            summary = "List of database details available for UniProtKB entry page.",
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
    public List<UniProtDatabaseDetail> getUniProtAllDatabase() {
        return service.getAllDatabases();
    }

    @Operation(
            summary = "List of evidence database details available for UniProtKB entry page.",
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

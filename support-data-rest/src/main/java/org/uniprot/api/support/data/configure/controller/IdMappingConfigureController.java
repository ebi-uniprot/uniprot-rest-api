package org.uniprot.api.support.data.configure.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.support.data.configure.response.IdMappingField;
import org.uniprot.api.support.data.configure.service.IdMappingConfigureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author sahmad
 * @created 15/03/2021
 */
@Tag(
        name = "Configuration",
        description = "These services provide configuration data used in UniProt website")
@RestController
@RequestMapping("/configure/idmapping")
public class IdMappingConfigureController {
    private final IdMappingConfigureService service;

    public IdMappingConfigureController(IdMappingConfigureService service) {
        this.service = service;
    }

    @Operation(
            summary = "List of fields available to use in IdMapping from and to dropdown lists.",
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
                                                                            IdMappingField.class)))
                        })
            })
    @GetMapping("/fields")
    public IdMappingField getIdMappingFields() {
        return this.service.getIdMappingFields();
    }
}

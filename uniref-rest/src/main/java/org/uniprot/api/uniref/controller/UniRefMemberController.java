package org.uniprot.api.uniref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.openapi.StreamResult;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.uniref.common.service.member.UniRefMemberService;
import org.uniprot.api.uniref.common.service.member.request.UniRefMemberRequest;
import org.uniprot.api.uniref.common.service.member.request.UniRefMemberStreamRequest;
import org.uniprot.core.uniref.UniRefMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author lgonzales
 * @since 05/01/2021
 */
@Tag(name = TAG_UNIREF, description = TAG_UNIREF_DESC)
@RestController
@Validated
@RequestMapping("/uniref")
public class UniRefMemberController extends BasicSearchController<UniRefMember> {
    private final UniRefMemberService service;

    @Autowired
    public UniRefMemberController(
            ApplicationEventPublisher eventPublisher,
            UniRefMemberService queryService,
            MessageConverterContextFactory<UniRefMember> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIREF);
        this.service = queryService;
    }

    @GetMapping(
            value = "/{id}/members",
            produces = {
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
            })
    @Operation(
            summary = ID_UNIREF_MEMBER_OPERATION,
            description = ID_UNIREF_MEMBER_OPERATION_DESC,
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
                                                                            UniRefMember.class))),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRefMember>> search(
            @Valid @ModelAttribute UniRefMemberRequest memberRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<UniRefMember> results = service.retrieveMembers(memberRequest);
        return super.getSearchResponse(results, "", request, response);
    }

    @SuppressWarnings("java:S6856")
    @GetMapping(
            value = "/{id}/members/stream",
            produces = {LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE})
    @Operation(
            summary = STREAM_ID_UNIREF_MEMBER_OPERATION,
            description = STREAM_ID_UNIREF_MEMBER_OPERATION_DESC,
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = StreamResult.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            org.uniprot.core.xml
                                                                                    .jaxb.uniref
                                                                                    .Entry.class,
                                                                    name = "entries"))),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<UniRefMember>>> stream(
            @Valid @ModelAttribute UniRefMemberStreamRequest streamRequest,
            @Parameter(hidden = true)
                    @RequestHeader(value = "Accept", defaultValue = APPLICATION_XML_VALUE)
                    MediaType contentType,
            HttpServletRequest request) {
        setBasicRequestFormat(streamRequest, request);
        return super.stream(
                () -> service.stream(streamRequest), streamRequest, contentType, request);
    }

    @Override
    protected String getEntityId(UniRefMember entity) {
        return entity.getMemberId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(
            UniRefMember entity, HttpServletRequest request) {
        return Optional.empty();
    }
}

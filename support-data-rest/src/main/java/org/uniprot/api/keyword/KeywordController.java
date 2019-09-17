package org.uniprot.api.keyword;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.keyword.request.KeywordRequestDTO;
import org.uniprot.api.keyword.service.KeywordService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.search.field.KeywordField;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.KEYWORD;

@RestController
@RequestMapping("/keyword")
@Validated
public class KeywordController extends BasicSearchController<KeywordEntry> {
    @Autowired
    private HttpServletRequest request;


    private final KeywordService keywordService;
    private static final String KEYWORD_ID_REGEX = "^KW-[0-9]{4}";

    public KeywordController(ApplicationEventPublisher eventPublisher, KeywordService keywordService,
                             @Qualifier("keywordMessageConverterContextFactory") MessageConverterContextFactory<KeywordEntry> keywordMessageConverterContextFactory,
                             ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, keywordMessageConverterContextFactory, downloadTaskExecutor, KEYWORD);
        this.keywordService = keywordService;
    }

    @Tag(name = "keyword", description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Dolor sed viverra ipsum nunc aliquet bibendum enim. In massa tempor nec feugiat. Nunc aliquet bibendum enim facilisis gravida. Nisl nunc mi ipsum faucibus vitae aliquet nec ullamcorper. Amet luctus venenatis lectus magna fringilla. Volutpat maecenas volutpat blandit aliquam etiam erat velit scelerisque in. Egestas egestas fringilla phasellus faucibus scelerisque eleifend. Sagittis orci a scelerisque purus semper eget duis. Nulla pharetra diam sit amet nisl suscipit. Sed adipiscing diam donec adipiscing tristique risus nec feugiat in. Fusce ut placerat orci nulla. Pharetra vel turpis nunc eget lorem dolor. Tristique senectus et netus et malesuada.\n" +
            "\n" +
            "Etiam tempor orci eu lobortis elementum nibh tellus molestie. Neque egestas congue quisque egestas. Egestas integer eget aliquet nibh praesent tristique. Vulputate mi sit amet mauris. Sodales neque sodales ut etiam sit. Dignissim suspendisse in est ante in. Volutpat commodo sed egestas egestas. Felis donec et odio pellentesque diam. Pharetra vel turpis nunc eget lorem dolor sed viverra. Porta nibh venenatis cras sed felis eget. Aliquam ultrices sagittis orci a. Dignissim diam quis enim lobortis. Aliquet porttitor lacus luctus accumsan. Dignissim convallis aenean et tortor at risus viverra adipiscing at.")
    @GetMapping(value = "/{keywordId}", produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    @Operation(summary = "Get Keyword by keywordId", responses = {
            @ApiResponse(content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = KeywordEntry.class)),
                    @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                    @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                    @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
            }
            )
    })
    public ResponseEntity<MessageConverterContext<KeywordEntry>> getById(@Parameter(description = "Keyword id to find")
                                                                         @PathVariable("keywordId")
                                                                         @Pattern(regexp = KEYWORD_ID_REGEX, flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "{search.keyword.invalid.id}")
                                                                                 String keywordId,
                                                                         @Parameter(description = "Comma separated list of fields to be returned in response")
                                                                                 @ValidReturnFields(fieldValidatorClazz = KeywordField.ResultFields.class)
                                                                         @RequestParam(value = "fields", required = false)
                                                                                 String fields) {

        KeywordEntry keywordEntry = this.keywordService.findById(keywordId);

        MediaType acceptHeader = getAcceptHeader(this.request);

        return super.getEntityResponse(keywordEntry, fields, acceptHeader);
    }

    @Tag(name = "keyword")
    @GetMapping(value = "/search",
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    @Operation(summary = "Search Keywords by given search criteria", responses = {
            @ApiResponse(content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = KeywordEntry.class))),
                    @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                    @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                    @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
            }
            )
    })
    public ResponseEntity<MessageConverterContext<KeywordEntry>> search(@Valid
                                                                            @ModelAttribute KeywordRequestDTO searchRequest,
                                                                        HttpServletRequest request,
                                                                        HttpServletResponse response) {
        QueryResult<KeywordEntry> results = keywordService.search(searchRequest);
        MediaType acceptHeader = getAcceptHeader(request);
        return super.getSearchResponse(results, searchRequest.getFields(), acceptHeader, request, response);
    }

    @Tag(name = "keyword")
    @GetMapping(value = "/download", produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    @Operation(summary = "Download Keywords by given search criteria", responses = {
            @ApiResponse(content = {
                    @Content(mediaType = APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = KeywordEntry.class))),
                    @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                    @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                    @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
            }
            )
    })
    public ResponseEntity<ResponseBodyEmitter> download(@Valid
                                                            @ModelAttribute
                                                                    KeywordRequestDTO searchRequest,
                                                        @RequestHeader(value = "Accept-Encoding", required = false)
                                                                String encoding,
                                                        HttpServletRequest request) {
        Stream<KeywordEntry> result = keywordService.download(searchRequest);
        MediaType acceptHeader = getAcceptHeader(request);
        return super.download(result, searchRequest.getFields(), acceptHeader, request, encoding);
    }

    @Override
    protected String getEntityId(KeywordEntry entity) {
        return entity.getKeyword().getAccession();
    }

    @Override
    protected Optional<String> getEntityRedirectId(KeywordEntry entity) {
        return Optional.empty();
    }

}

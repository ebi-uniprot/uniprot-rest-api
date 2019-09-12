package org.uniprot.api.keyword;

import io.swagger.v3.oas.annotations.Operation;
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

    @Tag(name = "Keyword")
    @GetMapping(value = "/{keywordId}", produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    @Operation(summary = "Get Keyword by keywordId", description = "Get Keyword by keywordId. If you want to search more than one you can use other api")
    public ResponseEntity<MessageConverterContext<KeywordEntry>> getById(@PathVariable("keywordId")
                                                                         @Pattern(regexp = KEYWORD_ID_REGEX, flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "{search.keyword.invalid.id}")
                                                                                 String keywordId,
                                                                                 @ValidReturnFields(fieldValidatorClazz = KeywordField.ResultFields.class)
                                                                         @RequestParam(value = "fields", required = false)
                                                                                 String fields) {

        KeywordEntry keywordEntry = this.keywordService.findById(keywordId);

        MediaType acceptHeader = getAcceptHeader(this.request);

        return super.getEntityResponse(keywordEntry, fields, acceptHeader);
    }

    @Tag(name = "Keyword")
    @GetMapping(value = "/search",
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    @Operation(summary = "Search Keywords by search criteria", description = "You can search more than one keywords based on the query. The query supports the solr format")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = KeywordEntry.class))))
    public ResponseEntity<MessageConverterContext<KeywordEntry>> search(@Valid
                                                                            @ModelAttribute KeywordRequestDTO searchRequest,
                                                                        HttpServletRequest request,
                                                                        HttpServletResponse response) {
        QueryResult<KeywordEntry> results = keywordService.search(searchRequest);
        MediaType acceptHeader = getAcceptHeader(request);
        return super.getSearchResponse(results, searchRequest.getFields(), acceptHeader, request, response);
    }

    @Tag(name = "Keyword")
    @GetMapping(value = "/download", produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    @Operation(summary = "Download Keywords by search criteria", description = "You can download more than one keywords based on the query. The query supports the solr format")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = KeywordEntry.class))))
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

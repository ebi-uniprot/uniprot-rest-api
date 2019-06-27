package uk.ac.ebi.uniprot.api.keyword;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.keyword.request.KeywordRequestDTO;
import uk.ac.ebi.uniprot.api.keyword.service.KeywordService;
import uk.ac.ebi.uniprot.api.rest.controller.BasicSearchController;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.cv.keyword.KeywordEntry;
import uk.ac.ebi.uniprot.search.field.KeywordField;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.*;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.KEYWORD;

@RestController
@RequestMapping("/keyword")
@Validated
public class KeywordController extends BasicSearchController<KeywordEntry> {

    private final KeywordService keywordService;
    private static final String KEYWORD_ID_REGEX = "^KW-[0-9]{4}";

    public KeywordController(ApplicationEventPublisher eventPublisher, KeywordService keywordService,
                             @Qualifier("keywordMessageConverterContextFactory") MessageConverterContextFactory<KeywordEntry> keywordMessageConverterContextFactory,
                             ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, keywordMessageConverterContextFactory, downloadTaskExecutor, KEYWORD);
        this.keywordService = keywordService;
    }

    @GetMapping(value = "/{keywordId}", produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<KeywordEntry>> getById(@PathVariable("keywordId")
                                                                         @Pattern(regexp = KEYWORD_ID_REGEX, flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "{search.keyword.invalid.id}")
                                                                                 String keywordId,
                                                                                 @ValidReturnFields(fieldValidatorClazz = KeywordField.ResultFields.class)
                                                                         @RequestParam(value = "fields", required = false)
                                                                                 String fields,
                                                                         @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                 MediaType contentType) {

        KeywordEntry keywordEntry = this.keywordService.findById(keywordId);
        return super.getEntityResponse(keywordEntry, fields, contentType);
    }


    @RequestMapping(value = "/search", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<KeywordEntry>> search(@Valid
                                                                                KeywordRequestDTO searchRequest,
                                                                        @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                MediaType contentType,
                                                                        HttpServletRequest request,
                                                                        HttpServletResponse response) {
        QueryResult<KeywordEntry> results = keywordService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), contentType, request, response);
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<ResponseBodyEmitter> download(@Valid
                                                                KeywordRequestDTO searchRequest,
                                                        @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                MediaType contentType,
                                                        @RequestHeader(value = "Accept-Encoding", required = false)
                                                                String encoding,
                                                        HttpServletRequest request) {
        Stream<KeywordEntry> result = keywordService.download(searchRequest);
        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
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

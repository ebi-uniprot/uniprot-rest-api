package uk.ac.ebi.uniprot.api.literature;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.literature.request.LiteratureRequestDTO;
import uk.ac.ebi.uniprot.api.literature.service.LiteratureService;
import uk.ac.ebi.uniprot.api.rest.controller.BasicSearchController;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.search.field.LiteratureField;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.*;
import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.LITERATURE;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
public class LiteratureController extends BasicSearchController<LiteratureEntry> {

    private final LiteratureService literatureService;
    private static final String LITERATURE_ID_REGEX = "^[0-9]+$";

    public LiteratureController(ApplicationEventPublisher eventPublisher, LiteratureService literatureService,
                                @Qualifier("literatureMessageConverterContextFactory") MessageConverterContextFactory<LiteratureEntry> literatureMessageConverterContextFactory,
                                ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, literatureMessageConverterContextFactory, downloadTaskExecutor, LITERATURE);
        this.literatureService = literatureService;
    }

    @GetMapping(value = "/{literatureId}", produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<LiteratureEntry>> getById(@PathVariable("literatureId")
                                                                            @Pattern(regexp = LITERATURE_ID_REGEX, flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "{search.literature.invalid.id}")
                                                                                    String literatureId,
                                                                            @ValidReturnFields(fieldValidatorClazz = LiteratureField.ResultFields.class)
                                                                            @RequestParam(value = "fields", required = false)
                                                                                    String fields,
                                                                            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                    MediaType contentType) {

        LiteratureEntry literatureEntry = this.literatureService.findById(literatureId);
        return super.getEntityResponse(literatureEntry, fields, contentType);
    }


    @RequestMapping(value = "/search", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<LiteratureEntry>> search(@Valid
                                                                                   LiteratureRequestDTO searchRequest,
                                                                           @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                   MediaType contentType,
                                                                           HttpServletRequest request,
                                                                           HttpServletResponse response) {
        QueryResult<LiteratureEntry> results = literatureService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), contentType, request, response);
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<ResponseBodyEmitter> download(@Valid
                                                                LiteratureRequestDTO searchRequest,
                                                        @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                MediaType contentType,
                                                        @RequestHeader(value = "Accept-Encoding", required = false)
                                                                String encoding,
                                                        HttpServletRequest request) {
        Stream<LiteratureEntry> result = literatureService.download(searchRequest);
        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
    }

    @Override
    protected String getEntityId(LiteratureEntry entity) {
        return entity.getPubmedId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(LiteratureEntry entity) {
        return Optional.empty();
    }

}

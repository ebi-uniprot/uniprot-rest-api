package org.uniprot.api.disease;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.store.search.field.DiseaseField;

@RestController
@RequestMapping("/disease")
@Validated
public class DiseaseController extends BasicSearchController<Disease> {
    @Autowired private DiseaseService diseaseService;
    public static final String ACCESSION_REGEX = "DI-(\\d{5})";

    protected DiseaseController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<Disease> diseaseMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {

        super(
                eventPublisher,
                diseaseMessageConverterContextFactory,
                downloadTaskExecutor,
                MessageConverterContextFactory.Resource.DISEASE);
    }

    @GetMapping(
            value = "/{accessionId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<Disease>> getByAccession(
            @PathVariable("accessionId")
                    @Pattern(
                            regexp = ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.disease.invalid.id}")
                    String accession,
            @ValidReturnFields(fieldValidatorClazz = DiseaseField.ResultFields.class)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType) {

        Disease disease = this.diseaseService.findByAccession(accession);
        return super.getEntityResponse(disease, fields, contentType);
    }

    @GetMapping(
            value = "/search",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<Disease>> searchCursor(
            @Valid DiseaseSearchRequest searchRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            HttpServletRequest request,
            HttpServletResponse response) {

        QueryResult<Disease> results = this.diseaseService.search(searchRequest);
        return super.getSearchResponse(
                results, searchRequest.getFields(), contentType, request, response);
    }

    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<ResponseBodyEmitter> download(
            @Valid DiseaseSearchRequest searchRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {

        Stream<Disease> result = this.diseaseService.download(searchRequest);

        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
    }

    @Override
    protected String getEntityId(Disease entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(Disease entity) {
        return Optional.empty();
    }
}

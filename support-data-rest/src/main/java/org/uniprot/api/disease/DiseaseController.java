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
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.store.config.UniProtDataType;

@RestController
@RequestMapping("/disease")
@Validated
public class DiseaseController extends BasicSearchController<DiseaseEntry> {
    @Autowired private DiseaseService diseaseService;
    public static final String ACCESSION_REGEX = "DI-(\\d{5})";

    protected DiseaseController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<DiseaseEntry> diseaseMessageConverterContextFactory,
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
    public ResponseEntity<MessageConverterContext<DiseaseEntry>> getByAccession(
            @PathVariable("accessionId")
                    @Pattern(
                            regexp = ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.disease.invalid.id}")
                    String accession,
            @ValidReturnFields(uniProtDataType = UniProtDataType.DISEASE)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        DiseaseEntry disease = this.diseaseService.findByUniqueId(accession);
        return super.getEntityResponse(disease, fields, request);
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
    public ResponseEntity<MessageConverterContext<DiseaseEntry>> searchCursor(
            @Valid DiseaseSearchRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<DiseaseEntry> results = this.diseaseService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
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
    public DeferredResult<ResponseEntity<MessageConverterContext<DiseaseEntry>>> download(
            @Valid DiseaseSearchRequest searchRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {

        Stream<DiseaseEntry> result = this.diseaseService.download(searchRequest);

        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
    }

    @Override
    protected String getEntityId(DiseaseEntry entity) {
        return entity.getId();
    }

    @Override
    protected Optional<String> getEntityRedirectId(DiseaseEntry entity) {
        return Optional.empty();
    }
}

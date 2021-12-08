package org.uniprot.api.idmapping.service;

import static java.util.Arrays.asList;
import static org.uniprot.api.idmapping.model.PredefinedIdMappingStatus.ENRICHMENT_WARNING;
import static org.uniprot.api.idmapping.model.PredefinedIdMappingStatus.LIMIT_EXCEED_ERROR;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.model.IdMappingWarningError;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.PredefinedIdMappingStatus;
import org.uniprot.api.idmapping.service.impl.IdMappingJobServiceImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class PIRResponseConverter {
    private static final Pattern UNIREF_ID_PATTERN =
            Pattern.compile(
                    SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF)
                            .getSearchFieldItemByName("id")
                            .getValidRegex());
    private static final Pattern UNIPARC_ID_PATTERN =
            Pattern.compile(
                    SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC)
                            .getSearchFieldItemByName("upi")
                            .getValidRegex());
    private static final Pattern UNIPROTKB_ID_PATTERN =
            Pattern.compile(
                    SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                            .getSearchFieldItemByName("accession_id")
                            .getValidRegex());
    private static final String NO_MATCHES_PIR_RESPONSE = "MSG: 200 -- No Matches.";

    static boolean isValidIdPattern(String to, String toValue) {
        to = to.strip();
        if (to.startsWith("UniRef")) {
            return UNIREF_ID_PATTERN.matcher(toValue).matches();
        } else if (to.equals("UniParc")) {
            return UNIPARC_ID_PATTERN.matcher(toValue).matches();
        } else if (to.equals("UniProtKB")) {
            return UNIPROTKB_ID_PATTERN.matcher(toValue).matches();
        } else {
            return true;
        }
    }


    public IdMappingResult convertToIDMappings(
            IdMappingJobRequest request, Integer maxToUniProtIdsAllowed, Integer maxToIdsAllowed,
            ResponseEntity<String> response) {
        IdMappingResult.IdMappingResultBuilder builder = IdMappingResult.builder();
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.equals(HttpStatus.OK)) {
            if (response.hasBody()) {
                response.getBody()
                        .lines()
                        .filter(line -> !line.startsWith("Taxonomy ID:"))
                        .filter(Utils::notNullNotEmpty)
                        //                        .filter(line -> !line.startsWith("MSG:"))
                        .forEach(line -> convertLine(line, request, builder));
                // populate  error  if needed
                Optional<IdMappingWarningError> optError = getOptionalLimitError(maxToIdsAllowed, builder.build());
                if(optError.isEmpty()) { // populate warning if needed
                    Optional<IdMappingWarningError> optWarning = getOptionalEnrichmentWarning(request,
                            maxToUniProtIdsAllowed, builder.build());
                    if (optWarning.isPresent()) {
                        builder.warning(optWarning.get());
                    }
                } else  {
                    builder.clearMappedIds();
                    builder.clearUnmappedIds();
                    builder.error(optError.get());
                }
            }
        } else {
            throw new HttpServerErrorException(statusCode, "PIR id-mapping service error");
        }

        return builder.build();
    }

    private void convertLine(
            String line,
            IdMappingJobRequest request,
            IdMappingResult.IdMappingResultBuilder builder) {
        if (line.startsWith("MSG:")) {
            if (line.endsWith(NO_MATCHES_PIR_RESPONSE)) {
                builder.unmappedIds(asList(request.getIds().split(",")));
            }
        } else {
            String[] rowParts = line.split("\t");
            if (rowParts.length == 1) {
                builder.unmappedId(rowParts[0]);
            } else {
                String fromValue = rowParts[0];
                Arrays.stream(rowParts[1].split(";"))
                        // filter based on valid to
                        .filter(toValue -> isValidIdPattern(request.getTo(), toValue))
                        .map(
                                toValue ->
                                        IdMappingStringPair.builder()
                                                .from(fromValue)
                                                .to(toValue)
                                                .build())
                        .forEach(builder::mappedId);
            }
        }
    }

    private Optional<IdMappingWarningError> getOptionalLimitError(Integer maxToIdsAllowed, IdMappingResult result) {
        if(Utils.notNullNotEmpty(result.getMappedIds()) && result.getMappedIds().size() > maxToIdsAllowed){
            return Optional.of(new IdMappingWarningError(LIMIT_EXCEED_ERROR.getCode(),
                    LIMIT_EXCEED_ERROR.getMessage() + maxToIdsAllowed));
        }
        return Optional.empty();
    }

    private Optional<IdMappingWarningError> getOptionalEnrichmentWarning(IdMappingJobRequest request, Integer  maxToUniProtIdsAllowed,
                                                                             IdMappingResult result) {
        if (isMappedToUniProtDBId(request.getTo()) &&
                Utils.notNullNotEmpty(result.getMappedIds()) &&
                result.getMappedIds().size() > maxToUniProtIdsAllowed) {
            return Optional.of(new IdMappingWarningError(ENRICHMENT_WARNING.getCode(),
                    ENRICHMENT_WARNING.getMessage() + maxToUniProtIdsAllowed));
        }
        return Optional.empty();
    }

    private boolean isMappedToUniProtDBId(String toDB) {
        return IdMappingJobServiceImpl.UNIREF_SET.contains(toDB) ||
                IdMappingJobServiceImpl.UNIPARC.contains(toDB) ||
                IdMappingJobServiceImpl.UNIPROTKB_SET.contains(toDB);
    }
}

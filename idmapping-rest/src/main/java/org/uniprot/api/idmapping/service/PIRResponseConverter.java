package org.uniprot.api.idmapping.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.impl.PIRServiceImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

import java.util.Arrays;
import java.util.regex.Pattern;

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
            IdMappingJobRequest request, ResponseEntity<String> response) {
        IdMappingResult.IdMappingResultBuilder builder = IdMappingResult.builder();
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.equals(HttpStatus.OK)) {
            if (response.hasBody()) {
                response.getBody()
                        .lines()
                        .filter(line -> !line.startsWith("Taxonomy ID:"))
                        .filter(Utils::notNullNotEmpty)
                        .filter(line -> !line.startsWith("MSG:"))
                        .forEach(
                                line -> {
                                    String[] rowParts = line.split("\t");
                                    if (rowParts.length == 1) {
                                        builder.unmappedId(rowParts[0]);
                                    } else {
                                        String fromValue = rowParts[0];
                                        Arrays.stream(rowParts[1].split(";"))
                                                // filter based on valid to
                                                .filter(
                                                        toValue ->
                                                                isValidIdPattern(
                                                                        request.getTo(), toValue))
                                                .map(
                                                        toValue ->
                                                                IdMappingStringPair.builder()
                                                                        .from(fromValue)
                                                                        .to(toValue)
                                                                        .build())
                                                .forEach(builder::mappedId);
                                    }
                                });
            }
        } else {
            throw new HttpServerErrorException(
                    statusCode,
                    "PIR id-mapping service error: " + PIRServiceImpl.PIR_ID_MAPPING_URL);
        }

        return builder.build();
    }
}

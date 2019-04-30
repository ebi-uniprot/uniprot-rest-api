package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import uk.ac.ebi.uniprot.api.proteome.service.GeneCentricService;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.search.field.validator.FieldValueValidator;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
 */
@RestController
@Api(tags = { "genecentric" })
@Validated
@RequestMapping("/genecentric")
public class GeneCentricController {
	private final GeneCentricService service;

	@Autowired
	public GeneCentricController(GeneCentricService service) {
		this.service = service;
	}

	@RequestMapping(value = "/upid/{upid}", produces = { APPLICATION_JSON_VALUE })
	public ResponseEntity<List<CanonicalProtein>> getByUpId(
			@PathVariable("upid") @Pattern(regexp = FieldValueValidator.PROTEOME_ID_REX, flags = {
					Pattern.Flag.CASE_INSENSITIVE }, message = "{search.invalid.upid.value}") String upid) {
		return ResponseEntity.ok().body(service.getByUpId(upid));
	}

	@RequestMapping(value = "/{accession}", produces = { APPLICATION_JSON_VALUE })
	public ResponseEntity<CanonicalProtein> getByAccession(
			@PathVariable("accession") @Pattern(regexp = FieldValueValidator.ACCESSION_REGEX, flags = {
					Pattern.Flag.CASE_INSENSITIVE }, message = "{search.invalid.accession.value}") String accession) {
		return ResponseEntity.ok().body(service.getByAccession(accession));
	}
}

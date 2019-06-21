package uk.ac.ebi.uniprot.api.support_data.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.uniprot.api.configure.service.ProteomeConfigureService;
import uk.ac.ebi.uniprot.api.configure.service.UniParcConfigureService;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.FieldGroup;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
*/
@RestController
@RequestMapping("/uniparc")
public class UniParcConfigureController {
	private UniParcConfigureService service;

	public UniParcConfigureController(UniParcConfigureService service) {
		this.service = service;
	}
	@GetMapping("/resultfields")
	public  List<FieldGroup>  getResultFields(){
		List<FieldGroup> resultFields = service.getResultFields();
		return resultFields;
	}
}


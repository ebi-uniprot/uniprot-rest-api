package org.uniprot.api.support_data.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.configure.service.UniParcConfigureService;
import org.uniprot.api.configure.uniprot.domain.FieldGroup;

import java.util.List;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
*/
@RestController
@RequestMapping("/configure/uniparc")
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


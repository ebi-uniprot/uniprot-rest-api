package org.uniprot.api.support_data.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.configure.service.UniRefConfigureService;
import org.uniprot.store.search.domain.FieldGroup;

/**
 *
 * @author jluo
 * @date: 20 Aug 2019
 *
*/
@RestController
@RequestMapping("/configure/uniref")
public class UniRefConfigureController {
	private UniRefConfigureService service;

	public UniRefConfigureController(UniRefConfigureService service) {
		this.service = service;
	}
	@GetMapping("/resultfields")
	public  List<FieldGroup>  getResultFields(){
		List<FieldGroup> resultFields = service.getResultFields();
		return resultFields;
	}
}


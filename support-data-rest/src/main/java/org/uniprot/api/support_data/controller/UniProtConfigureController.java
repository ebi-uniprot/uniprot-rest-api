package org.uniprot.api.support_data.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.configure.service.UniProtConfigureService;
import org.uniprot.api.configure.uniprot.domain.DatabaseGroup;
import org.uniprot.api.configure.uniprot.domain.EvidenceGroup;
import org.uniprot.api.configure.uniprot.domain.FieldGroup;
import org.uniprot.api.configure.uniprot.domain.SearchItem;

import java.util.List;

@RestController
@RequestMapping("/configure/uniprotkb")
public class UniProtConfigureController {

	private UniProtConfigureService service;

	public UniProtConfigureController(UniProtConfigureService service) {
		this.service = service;
	}

	@GetMapping("/search_terms")
	public List<SearchItem> getUniProtSearchTerms() {
		return service.getUniProtSearchItems();
	}

	@GetMapping("/annotation_evidences")
	public  List<EvidenceGroup>  getUniProtAnnotationEvidences() {
		return service.getAnnotationEvidences();
	}
	
	@GetMapping("/go_evidences")
	public  List<EvidenceGroup>  getUniProtGoEvidences() {
		return service.getGoEvidences();
	}
	@GetMapping("/databases")
	public  List<DatabaseGroup>  getUniProtDatabase() {
		return service.getDatabases();
	}
	
	@GetMapping("/resultfields")
	public  List<FieldGroup>  getResultFields(){
		List<FieldGroup> resultFields = service.getResultFields();
		resultFields.addAll(service.getDatabaseFields());
		return resultFields;
	}
}

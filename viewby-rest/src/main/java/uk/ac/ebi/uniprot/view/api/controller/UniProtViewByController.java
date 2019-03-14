package uk.ac.ebi.uniprot.view.api.controller;


import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import uk.ac.ebi.uniprot.view.api.model.ViewBy;
import uk.ac.ebi.uniprot.view.api.service.UniProtViewByService;

@RestController
@Api(tags = {"view"})
@RequestMapping("/view")
public class UniProtViewByController {
	
	 private final UniProtViewByService viewByService;

	    @Autowired
	    public UniProtViewByController(UniProtViewByService viewByService) {
	        this.viewByService = viewByService;
	    }
	    @RequestMapping(value = "/ec", produces = {APPLICATION_JSON_VALUE})
	public List<ViewBy> getEC(
			  @RequestParam(value = "query", required = true) String query, 
			  @RequestParam(value = "parent", required = false) String parent){
		return viewByService.getEC(query, parent);
	}
	
}

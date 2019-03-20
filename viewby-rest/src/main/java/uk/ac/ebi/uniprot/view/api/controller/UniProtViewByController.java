package uk.ac.ebi.uniprot.view.api.controller;


import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import uk.ac.ebi.uniprot.view.api.model.ViewBy;
import uk.ac.ebi.uniprot.view.api.service.UniProtViewByECService;
import uk.ac.ebi.uniprot.view.api.service.UniProtViewByGoService;
import uk.ac.ebi.uniprot.view.api.service.UniProtViewByKeywordService;
import uk.ac.ebi.uniprot.view.api.service.UniProtViewByPathwayService;
import uk.ac.ebi.uniprot.view.api.service.UniProtViewByTaxonomyService;

@RestController
@Api(tags = {"view"})
@RequestMapping("/view")
public class UniProtViewByController {
	
	 private final UniProtViewByECService viewByECService;
	 private final UniProtViewByKeywordService viewByKeywordService;
	 private final UniProtViewByPathwayService viewByPathwayService;
	 private final UniProtViewByGoService viewByGoService;
	 private final UniProtViewByTaxonomyService viewByTaxonomyService;
	    @Autowired
	    public UniProtViewByController(UniProtViewByECService viewByECService,
	    		UniProtViewByKeywordService viewByKeywordService,
	    		 UniProtViewByPathwayService viewByPathwayService,
	    		 UniProtViewByGoService viewByGoService,
	    		 UniProtViewByTaxonomyService viewByTaxonomyService
	    		) {
	        this.viewByECService = viewByECService;
	        this.viewByKeywordService =viewByKeywordService;
	        this.viewByPathwayService = viewByPathwayService;
	        this.viewByGoService = viewByGoService;
	        this.viewByTaxonomyService = viewByTaxonomyService;
	    }
	@RequestMapping(value = "/ec", produces = {APPLICATION_JSON_VALUE})
	public List<ViewBy> getEC(
			  @RequestParam(value = "query", required = true) String query, 
			  @RequestParam(value = "parent", required = false) String parent){
		return viewByECService.get(query, parent);
	}
	
	@RequestMapping(value = "/keyword", produces = {APPLICATION_JSON_VALUE})
	public List<ViewBy> getKeyword(
			  @RequestParam(value = "query", required = true) String query, 
			  @RequestParam(value = "parent", required = false) String parent){
		return viewByKeywordService.get(query, parent);
	}
	@RequestMapping(value = "/pathway", produces = {APPLICATION_JSON_VALUE})
	public List<ViewBy> getPathway(
			  @RequestParam(value = "query", required = true) String query, 
			  @RequestParam(value = "parent", required = false) String parent){
		return viewByPathwayService.get(query, parent);
	}
	
	@RequestMapping(value = "/go", produces = {APPLICATION_JSON_VALUE})
	public List<ViewBy> getGo(
			  @RequestParam(value = "query", required = true) String query, 
			  @RequestParam(value = "parent", required = false) String parent){
		return viewByGoService.get(query, parent);
	}
	@RequestMapping(value = "/taxonomy", produces = {APPLICATION_JSON_VALUE})
	public List<ViewBy> getTaxonomy(
			  @RequestParam(value = "query", required = true) String query, 
			  @RequestParam(value = "parent", required = false) String parent){
		return viewByTaxonomyService.get(query, parent);
	}
}

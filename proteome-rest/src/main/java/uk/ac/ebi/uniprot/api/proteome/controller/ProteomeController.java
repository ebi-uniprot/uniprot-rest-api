package uk.ac.ebi.uniprot.api.proteome.controller;


import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;

/**
 *
 * @author jluo
 * @date: 24 Apr 2019
 *
*/
@RestController
@Api(tags = {"proteome"})
@Validated
@RequestMapping("/proteome")
public class ProteomeController {
}


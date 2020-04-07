package org.uniprot.api.support_data.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.support_data.configure.service.UniRefConfigureService;
import org.uniprot.store.config.returnfield.model.ReturnField;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@RestController
@RequestMapping("/configure/uniref")
public class UniRefConfigureController {
    private UniRefConfigureService service;

    public UniRefConfigureController(UniRefConfigureService service) {
        this.service = service;
    }

    @GetMapping("/resultfields")
    public List<ReturnField> getResultFields() {
        return service.getResultFields();
    }
}

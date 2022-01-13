package org.uniprot.api.help.centre.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.help.centre.model.ContactForm;
import org.uniprot.api.help.centre.model.Token;
import org.uniprot.api.help.centre.service.ContactService;

@RestController
@Validated
@RequestMapping("/contact")
public class ContactController {

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    @GetMapping(
            value = "/token",
            produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<Token> generateToken(
            HttpServletRequest request,
            @RequestParam(value = "key", required = true) String subjectKey) {
        Token token = service.generateToken(subjectKey);
        return ResponseEntity.ok().body(token);
    }

    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactForm> postContact(
            HttpServletRequest request, @Valid ContactForm contactForm) {
        String tokenKey = contactForm.getSubject();
        if (service.validToken(contactForm.getToken(), tokenKey)) {
            service.sendEmail(contactForm);
        }
        return ResponseEntity.ok().body(contactForm);
    }
}

package org.uniprot.api.help.centre.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.uniprot.api.rest.validation.IsEmpty;

@Getter
@Setter
public class ContactForm {

    @NotNull(message = "{search.helpcentre.contact.required.email}")
    @Email(message = "{search.helpcentre.contact.invalid.email}")
    private String email;

    @NotNull(message = "{search.helpcentre.contact.required.subject}")
    private String subject;

    @NotNull(message = "{search.helpcentre.contact.required.message}")
    private String message;

    @NotNull(message = "{search.helpcentre.contact.required.token}")
    private String token;

    // HoneyPot functionality to block robots
    @IsEmpty(message = "{search.helpcentre.contact.robot}")
    private String requiredForRobots;

    public static void main(String[] args) throws Exception {}
}

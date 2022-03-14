package org.uniprot.api.help.centre.service;

import static org.junit.jupiter.api.Assertions.*;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.help.centre.model.ContactForm;
import org.uniprot.api.help.centre.model.Token;

import java.util.Properties;

class ContactServiceTest {

    @Test
    void canGenerateToken() {
        ContactConfig config = new ContactConfig();
        config.setHost("test");
        ContactService service = new ContactService(config);
        Token token = service.generateToken("tokenkey");
        assertNotNull(token);
    }

    @Test
    void canValidateToken() {
        ContactConfig config = new ContactConfig();
        config.setHost("test");
        config.setTokenExpiresInSecs(2L);
        ContactService service = new ContactService(config);
        String tokenKey = "tokenkey";
        Token token = service.generateToken(tokenKey);
        assertNotNull(token);
        assertTrue(service.validToken(token.getToken(), tokenKey));
    }

    @Test
    void throwErrorWhenInvalidFormatValidToken() {
        ContactConfig config = new ContactConfig();
        config.setHost("test");
        config.setTokenExpiresInSecs(2L);
        ContactService service = new ContactService(config);
        ImportantMessageServiceException exception =
                assertThrows(
                        ImportantMessageServiceException.class,
                        () -> service.validToken("INVALID", "VALID"));
        assertEquals("Invalid token format", exception.getMessage());
    }

    @Test
    void throwErrorWhenInvalidKeyValidToken() {
        ContactConfig config = new ContactConfig();
        config.setHost("test");
        config.setTokenExpiresInSecs(2L);
        ContactService service = new ContactService(config);
        String tokenKey = "tokenkey";
        Token token = service.generateToken(tokenKey);
        assertNotNull(token);
        final String tokenStr = token.getToken();
        assertNotNull(tokenStr);
        ImportantMessageServiceException exception =
                assertThrows(
                        ImportantMessageServiceException.class,
                        () -> service.validToken(tokenStr, "INVALID"));
        assertEquals("Invalid token key format", exception.getMessage());
    }

    @Test
    void throwErrorWhenExpiredTokenValidToken() throws InterruptedException {
        ContactConfig config = new ContactConfig();
        config.setHost("test");
        config.setTokenExpiresInSecs(1L);
        ContactService service = new ContactService(config);
        String tokenKey = "tokenkey";
        Token token = service.generateToken(tokenKey);
        assertNotNull(token);
        final String tokenStr = token.getToken();
        assertNotNull(tokenStr);
        Thread.sleep(2000L);
        ImportantMessageServiceException exception =
                assertThrows(
                        ImportantMessageServiceException.class,
                        () -> service.validToken(tokenStr, tokenKey));
        assertEquals("The provided token is expired", exception.getMessage());
    }

    @Test
    void sendEmailFillTheEmailMessageCorrectly() throws Exception {
        String subject = "subjectValue";
        String host = "hostValue";
        String toEmail = "to@email.com";
        String fromEmail = "from@email.com";
        String messageFormat = "messageFormatValue";
        String message = "messageValue";

        ContactConfig config = new ContactConfig();
        config.setHost(host);
        config.setTo(toEmail);
        config.setMessageFormat(messageFormat);

        FakeContactService service = new FakeContactService(config);

        ContactForm contactForm = new ContactForm();
        contactForm.setEmail(fromEmail);
        contactForm.setMessage(message);

        contactForm.setSubject(subject);
        service.sendEmail(contactForm);
        MimeMessage sentMessage = (MimeMessage) service.getSentMessage();
        assertNotNull(sentMessage);
        assertEquals(subject, sentMessage.getSubject());
        assertEquals(fromEmail, sentMessage.getFrom()[0].toString());
        assertEquals(toEmail, sentMessage.getAllRecipients()[0].toString());
        String sentContent =
                ((MimeMultipart) sentMessage.getContent()).getBodyPart(0).getContent().toString();
        assertEquals(message, sentContent);
    }

    @Test
    void sendEmailWithInvalidData() {
        ContactConfig config = new ContactConfig();
        config.setHost("test");
        config.setTo("to@email.com");
        config.setMessageFormat("messageFormatValue");
        ContactService service = new ContactService(config);

        ContactForm contactForm = new ContactForm();
        contactForm.setEmail("emailValue");
        contactForm.setMessage("messageValue");
        contactForm.setSubject("subjectValue");
        ServiceException serviceException =
                assertThrows(ServiceException.class, () -> service.sendEmail(contactForm));
        assertNotNull(serviceException);
        assertEquals("Unable to send Email", serviceException.getMessage());
        assertNotNull(serviceException.getCause());
    }
}

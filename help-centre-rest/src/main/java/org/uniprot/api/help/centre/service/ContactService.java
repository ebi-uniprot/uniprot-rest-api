package org.uniprot.api.help.centre.service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.help.centre.model.ContactForm;
import org.uniprot.api.help.centre.model.Token;

@Service
@Profile("live")
public class ContactService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    private static final String DELIMITER = "-;;-";
    private static final String TOKEN_PREFIX = "contactToken";
    private static final int TOKEN_RADIX = 36;
    private final ContactConfig contactConfig;
    private final Properties emailProperties;

    public ContactService(ContactConfig contactConfig) {
        this.contactConfig = contactConfig;

        Properties emailProp = new Properties();
        emailProp.put("mail.smtp.host", contactConfig.getHost());
        emailProp.put("mail.smtp.port", contactConfig.getPort());
        this.emailProperties = emailProp;
    }

    public Token generateToken(String subjectKey) {
        String date = LocalDateTime.now().format(formatter);
        String concatenatedToken = TOKEN_PREFIX + DELIMITER + subjectKey + DELIMITER + date;
        String encryptedToken = new BigInteger(concatenatedToken.getBytes()).toString(TOKEN_RADIX);
        return new Token(encryptedToken);
    }

    public boolean validToken(String token, String subjectKey) {
        byte[] bytes = new BigInteger(token, TOKEN_RADIX).toByteArray();
        String extractedToken = new String(bytes);
        String[] parsedToken = extractedToken.split(DELIMITER);
        if (parsedToken.length == 3) {
            String tokenKey = parsedToken[1];
            if (!subjectKey.contains(tokenKey)) {
                throw new ImportantMessageServiceException("Invalid token key format");
            }
            LocalDateTime tokenDate = LocalDateTime.parse(parsedToken[2], formatter);
            if (tokenDate
                    .plusSeconds(contactConfig.getTokenExpireInSecs())
                    .isBefore(LocalDateTime.now())) {
                throw new ImportantMessageServiceException("The provided token is expired");
            }
        } else {
            throw new ImportantMessageServiceException("Invalid token format");
        }
        return true;
    }

    public void sendEmail(ContactForm contactForm) {
        try {
            Session session = Session.getInstance(emailProperties);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(contactForm.getEmail()));
            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(contactConfig.getTo()));
            message.setSubject(contactForm.getSubject());

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(contactForm.getMessage(), contactConfig.getMessageFormat());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);

            sendMail(message);
        } catch (MessagingException messagingException) {
            throw new ServiceException("Unable to send Email", messagingException);
        }
    }

    protected void sendMail(Message message) throws MessagingException {
        Transport.send(message);
    }
}

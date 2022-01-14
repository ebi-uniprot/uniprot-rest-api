package org.uniprot.api.help.centre.service;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("offline")
@Service
public class FakeContactService extends ContactService {

    private Message sentMessage;

    public FakeContactService(ContactConfig contactConfig) {
        super(contactConfig);
    }

    @Override
    protected void sendMail(Message message) throws MessagingException {
        sentMessage = message;
    }

    Message getSentMessage() {
        return sentMessage;
    }
}

package org.uniprot.api.rest.download.queue;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.download.file.AsyncDownloadFileHandler;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Service("uniProtKB")
@Slf4j
@Profile({"asyncDownload"})
public class UniProtKBRabbitProducerMessageService extends RabbitProducerMessageService {

    public UniProtKBRabbitProducerMessageService(
            MessageConverter converter,
            @Qualifier("uniProtKBRabbitTemplate") RabbitTemplate rabbitTemplate,
            DownloadJobRepository downloadJobRepository,
            HashGenerator<DownloadRequest> hashGenerator,
            AsyncDownloadSubmissionRules asyncDownloadSubmissionRules,
            AsyncDownloadFileHandler asyncDownloadFileHandler) {
        super(
                converter,
                rabbitTemplate,
                downloadJobRepository,
                hashGenerator,
                asyncDownloadSubmissionRules,
                asyncDownloadFileHandler);
    }
}

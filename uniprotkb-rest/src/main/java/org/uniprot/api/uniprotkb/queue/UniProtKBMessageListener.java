package org.uniprot.api.uniprotkb.queue;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Service;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Service("Consumer")
public class UniProtKBMessageListener implements MessageListener {

    @Override
    public void onMessage(Message message) {
//        StreamRequest streamRequest = message.getBody();
        System.out.println("Message processed" + message.getBody());
        // talk to redis
        // talk to solr
        // write to nfs
        // acknowledge the queue with failure/success
    }
}

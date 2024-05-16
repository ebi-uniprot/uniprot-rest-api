package org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.uniprotkb.UniProtKBSolrIdBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.ResultStreamerFacade;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.uniprotkb.UniProtKBListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.uniprotkb.UniProtKBRDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPROTKB;

@Component
public class UniProtKBResultStreamerFacade extends ResultStreamerFacade<UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {


    public UniProtKBResultStreamerFacade(UniProtKBRDFResultStreamer rdfResultStreamer, UniProtKBListResultStreamer listResultStreamer, UniProtKBSolrIdBatchResultStreamer batchResultStreamer, MessageConverterContextFactory<UniProtKBEntry> converterContextFactory) {
        super(rdfResultStreamer, listResultStreamer, batchResultStreamer, converterContextFactory);
    }

    @Override
    protected Resource getResource() {
        return UNIPROTKB;
    }
}

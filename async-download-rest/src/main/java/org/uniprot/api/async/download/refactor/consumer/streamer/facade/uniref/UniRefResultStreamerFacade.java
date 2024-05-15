package org.uniprot.api.async.download.refactor.consumer.streamer.facade.uniref;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.uniref.UniRefBatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.ResultStreamerFacade;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.uniref.UniRefListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.uniref.UniRefRDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefResultStreamerFacade
        extends ResultStreamerFacade<UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {

    public UniRefResultStreamerFacade(
            UniRefRDFResultStreamer rdfResultStreamer,
            UniRefListResultStreamer listResultStreamer,
            UniRefBatchResultStreamer batchResultStreamer,
            MessageConverterContextFactory<UniRefEntryLight> converterContextFactory) {
        super(rdfResultStreamer, listResultStreamer, batchResultStreamer, converterContextFactory);
    }

    @Override
    protected Resource getResource() {
        return UNIREF;
    }
}

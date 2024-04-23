package org.uniprot.api.async.download.refactor.consumer.processor.idresult;

import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.IdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.ResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;

public class IdResultRequestProcessor<T extends DownloadRequest, R extends DownloadJob, S> implements RequestProcessor<T> {
    private final IdRequestProcessor<T> idRequestProcessor;
    private final ResultRequestProcessor<T, R, S> resultRequestProcessor;

    public IdResultRequestProcessor(IdRequestProcessor<T> idRequestProcessor, ResultRequestProcessor<T, R, S> resultRequestProcessor) {
        this.idRequestProcessor = idRequestProcessor;
        this.resultRequestProcessor = resultRequestProcessor;
    }

    @Override
    public void process(T request) {
        idRequestProcessor.process(request);
        resultRequestProcessor.process(request);
    }
}

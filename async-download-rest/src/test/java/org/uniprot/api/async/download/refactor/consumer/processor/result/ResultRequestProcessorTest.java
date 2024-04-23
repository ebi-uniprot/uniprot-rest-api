package org.uniprot.api.async.download.refactor.consumer.processor.result;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.ResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class ResultRequestProcessorTest<T extends DownloadRequest, R extends DownloadJob, S> {
    protected T request;
    protected DownloadConfigProperties downloadConfigProperties;
    protected HeartbeatProducer heartbeatProducer;
    protected List<HttpMessageConverter<?>> messageConverters;
    protected AsyncDownloadFileHandler fileHandler;
    protected ResultStreamerFacade<T, R, S> resultStreamerFacade;
    protected ResultRequestProcessor<T,R,S> resultRequestProcessor;

    @Test
    void process() {

    }
}
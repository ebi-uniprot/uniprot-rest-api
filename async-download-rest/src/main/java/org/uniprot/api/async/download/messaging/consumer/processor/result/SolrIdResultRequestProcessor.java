package org.uniprot.api.async.download.messaging.consumer.processor.result;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.streamer.facade.SolrIdResultStreamerFacade;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;

public abstract class SolrIdResultRequestProcessor<
                T extends DownloadRequest, R extends DownloadJob, S>
        implements RequestProcessor<T> {
    private final DownloadConfigProperties downloadConfigProperties;
    private final HeartbeatProducer heartbeatProducer;
    private final SolrIdResultStreamerFacade<T, R, S> solrIdResultStreamerFacade;
    private final UUWMessageConverterFactory uuwMessageConverterFactory;

    protected SolrIdResultRequestProcessor(
            DownloadConfigProperties downloadConfigProperties,
            HeartbeatProducer heartbeatProducer,
            SolrIdResultStreamerFacade<T, R, S> solrIdResultStreamerFacade,
            UUWMessageConverterFactory uuwMessageConverterFactory) {
        this.downloadConfigProperties = downloadConfigProperties;
        this.heartbeatProducer = heartbeatProducer;
        this.solrIdResultStreamerFacade = solrIdResultStreamerFacade;
        this.uuwMessageConverterFactory = uuwMessageConverterFactory;
    }

    @Override
    public void process(T request) {
        try {
            MediaType contentType = UniProtMediaType.valueOf(request.getFormat());

            String id = request.getId();
            String fileNameWithExt = id + FileType.GZIP.getExtension();
            Path resultPath =
                    Paths.get(downloadConfigProperties.getResultFilesFolder(), fileNameWithExt);

            OutputStream outputStream =
                    Files.newOutputStream(
                            resultPath,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

            MessageConverterContext<S> context =
                    solrIdResultStreamerFacade.getConvertedResult(request);
            AbstractUUWHttpMessageConverter<S, S> outputWriter =
                    (AbstractUUWHttpMessageConverter<S, S>)
                            uuwMessageConverterFactory.getOutputWriter(contentType, getType());
            outputWriter.writeContents(
                    context, gzipOutputStream, Instant.now(), new AtomicInteger());
        } catch (Exception ex) {
            throw new ResultProcessingException(ex.getMessage());
        } finally {
            heartbeatProducer.stop(request.getId());
        }
    }

    protected abstract Type getType();
}

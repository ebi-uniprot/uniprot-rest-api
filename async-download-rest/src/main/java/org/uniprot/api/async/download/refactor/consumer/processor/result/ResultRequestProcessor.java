package org.uniprot.api.async.download.refactor.consumer.processor.result;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.ResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public abstract class ResultRequestProcessor<T extends DownloadRequest, R extends DownloadJob, S> implements RequestProcessor<T> {
    private final DownloadConfigProperties downloadConfigProperties;
    private final HeartbeatProducer heartbeatProducer;
    private final AsyncDownloadFileHandler fileHandler;
    private final ResultStreamerFacade<T, R, S> resultStreamerFacade;
    private final UUWMessageConverterFactory uuwMessageConverterFactory;

    protected ResultRequestProcessor(DownloadConfigProperties downloadConfigProperties, HeartbeatProducer heartbeatProducer, AsyncDownloadFileHandler fileHandler, ResultStreamerFacade<T, R, S> resultStreamerFacade, UUWMessageConverterFactory uuwMessageConverterFactory) {
        this.downloadConfigProperties = downloadConfigProperties;
        this.heartbeatProducer = heartbeatProducer;
        this.fileHandler = fileHandler;
        this.resultStreamerFacade = resultStreamerFacade;
        this.uuwMessageConverterFactory = uuwMessageConverterFactory;
    }

    @Override
    public void process(T request) {
        try {
            MediaType contentType = UniProtMediaType.valueOf(request.getFormat());

            String jobId = request.getJobId();
            String fileNameWithExt = jobId + FileType.GZIP.getExtension();
            Path resultPath = Paths.get(downloadConfigProperties.getResultFilesFolder(), fileNameWithExt);
            AbstractUUWHttpMessageConverter<S, S> outputWriter = (AbstractUUWHttpMessageConverter<S, S>) uuwMessageConverterFactory.getOutputWriter(contentType, getType());

            Stream<String> ids = Files.lines(fileHandler.getIdFile(jobId));
            OutputStream outputStream =
                    Files.newOutputStream(
                            resultPath,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

            MessageConverterContext<S> context = resultStreamerFacade.getConvertedResult(request, ids);

            outputWriter.writeContents(context, gzipOutputStream, Instant.now(), new AtomicInteger());

        } catch (Exception ex) {
            throw new ResultProcessingException(ex.getMessage());
        } finally {
            heartbeatProducer.stop(request.getJobId());
        }

    }

    protected abstract Type getType();
}

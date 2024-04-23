package org.uniprot.api.async.download.refactor.consumer.processor.result;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.ResultStreamerFacade;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public abstract class ResultRequestProcessor<T extends DownloadRequest, R extends DownloadJob, S> implements RequestProcessor<T> {
    private final DownloadConfigProperties downloadConfigProperties;
    private final HeartbeatProducer heartbeatProducer;
    private final List<HttpMessageConverter<?>> messageConverters;
    private final AsyncDownloadFileHandler fileHandler;
    private final ResultStreamerFacade<T, R, S> resultStreamerFacade;

    protected ResultRequestProcessor(DownloadConfigProperties downloadConfigProperties, HeartbeatProducer heartbeatProducer, List<HttpMessageConverter<?>> messageConverters, AsyncDownloadFileHandler fileHandler, ResultStreamerFacade<T, R, S> resultStreamerFacade) {
        this.downloadConfigProperties = downloadConfigProperties;
        this.heartbeatProducer = heartbeatProducer;
        this.messageConverters = messageConverters;
        this.fileHandler = fileHandler;
        this.resultStreamerFacade = resultStreamerFacade;
    }

    @Override
    public void process(T request) {
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());

        if (!UniProtMediaType.HDF5_MEDIA_TYPE.equals(contentType)) {
            String jobId = request.getJobId();
            String fileNameWithExt = jobId + FileType.GZIP.getExtension();
            Path resultPath = Paths.get(downloadConfigProperties.getResultFilesFolder(), fileNameWithExt);
            AbstractUUWHttpMessageConverter<S, S> outputWriter = getOutputWriter(contentType, getType());

            try (Stream<String> ids = Files.lines(fileHandler.getIdFile(jobId));
                 OutputStream output =
                         Files.newOutputStream(
                                 resultPath,
                                 StandardOpenOption.WRITE,
                                 StandardOpenOption.CREATE,
                                 StandardOpenOption.TRUNCATE_EXISTING);
                 GZIPOutputStream gzipOutputStream = new GZIPOutputStream(output)) {

                MessageConverterContext<S> context = resultStreamerFacade.getConvertedResult(request, ids);

                outputWriter.writeContents(context, gzipOutputStream, Instant.now(), new AtomicInteger());

            } catch (Exception exception) {
                throw new ResultProcessorException(exception.getMessage());
            } finally {
                heartbeatProducer.stop(request.getJobId());
            }
        }

    }

    private AbstractUUWHttpMessageConverter<S, S> getOutputWriter(
            MediaType contentType, Type type) {
        return messageConverters.stream()
                .filter(AbstractUUWHttpMessageConverter.class::isInstance)
                .filter(
                        converter ->
                                ((AbstractUUWHttpMessageConverter<?, ?>) converter)
                                        .canWrite(type, MessageConverterContext.class, contentType))
                .map(converter -> (AbstractUUWHttpMessageConverter<S, S>) converter)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Unable to find Message converter"));
    }

    protected abstract Type getType();
}

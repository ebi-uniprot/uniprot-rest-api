package org.uniprot.api.async.download.refactor.consumer.processor.result;

import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.IdMappingResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingServiceUtils;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

public abstract class IdMappingResultRequestProcessor<Q, P extends EntryPair<Q>>
        implements RequestProcessor<IdMappingDownloadRequest> {
    private final DownloadConfigProperties downloadConfigProperties;
    private final HeartbeatProducer heartbeatProducer;
    private final AsyncDownloadFileHandler fileHandler;
    private final IdMappingResultStreamerFacade<Q, P> solrIdResultStreamerFacade;
    private final UUWMessageConverterFactory uuwMessageConverterFactory;
    private final IdMappingJobCacheService idMappingJobCacheService;
    private final MessageConverterContextFactory<P> converterContextFactory;

    protected IdMappingResultRequestProcessor(
            DownloadConfigProperties downloadConfigProperties,
            HeartbeatProducer heartbeatProducer,
            AsyncDownloadFileHandler fileHandler,
            IdMappingResultStreamerFacade<Q, P> solrIdResultStreamerFacade,
            UUWMessageConverterFactory uuwMessageConverterFactory, IdMappingJobCacheService idMappingJobCacheService, MessageConverterContextFactory<P> converterContextFactory) {
        this.downloadConfigProperties = downloadConfigProperties;
        this.heartbeatProducer = heartbeatProducer;
        this.fileHandler = fileHandler;
        this.solrIdResultStreamerFacade = solrIdResultStreamerFacade;
        this.uuwMessageConverterFactory = uuwMessageConverterFactory;
        this.idMappingJobCacheService = idMappingJobCacheService;
        this.converterContextFactory = converterContextFactory;
    }

    @Override
    public void process(IdMappingDownloadRequest request) {
        try {
            MediaType contentType = UniProtMediaType.valueOf(request.getFormat());

            String jobId = request.getJobId();
            String fileNameWithExt = jobId + FileType.GZIP.getExtension();
            Path resultPath =
                    Paths.get(downloadConfigProperties.getResultFilesFolder(), fileNameWithExt);

            OutputStream outputStream =
                    Files.newOutputStream(
                            resultPath,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

            MessageConverterContext<P> context = solrIdResultStreamerFacade.getConvertedResult(request);
            AbstractUUWHttpMessageConverter<P, Q> outputWriter =
                    (AbstractUUWHttpMessageConverter<P, Q>)
                            uuwMessageConverterFactory.getOutputWriter(contentType, getType());
            outputWriter.writeContents(
                    context, gzipOutputStream, Instant.now(), new AtomicInteger());

        } catch (Exception ex) {
            throw new ResultProcessingException(ex.getMessage());
        } finally {
            heartbeatProducer.stop(request.getJobId());
        }
    }

    protected abstract Resource getResource();

    protected abstract Type getType();
}

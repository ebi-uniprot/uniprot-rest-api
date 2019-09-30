package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

/**
 * @author jluo
 * @date: 1 May 2019
 */
public abstract class AbstractTsvMessagerConverter<T>
        extends AbstractEntityHttpMessageConverter<T> {

    public AbstractTsvMessagerConverter(Class<T> messageConverterEntryClass) {
        super(UniProtMediaType.TSV_MEDIA_TYPE, messageConverterEntryClass);
    }

    protected abstract List<String> entry2TsvStrings(T entity);

    protected abstract List<String> getHeader();

    protected abstract void initBefore(MessageConverterContext<T> context);

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        initBefore(context);
        outputStream.write(
                getHeader().stream().collect(Collectors.joining("\t", "", "\n")).getBytes());
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
        List<String> result = entry2TsvStrings(entity);
        outputStream.write(result.stream().collect(Collectors.joining("\t", "", "\n")).getBytes());
    }
}

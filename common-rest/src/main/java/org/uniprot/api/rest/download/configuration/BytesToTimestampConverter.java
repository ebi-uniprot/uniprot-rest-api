package org.uniprot.api.rest.download.configuration;

import java.sql.Timestamp;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
public class BytesToTimestampConverter implements Converter<byte[], Timestamp> {
    @Override
    public Timestamp convert(final byte[] source) {
        String value = new String(source);
        return new Timestamp(Long.parseLong(value));
    }
}

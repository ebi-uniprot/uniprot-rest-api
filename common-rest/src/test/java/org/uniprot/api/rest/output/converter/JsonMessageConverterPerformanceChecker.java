package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniref.UniRefEntryLightJsonConfig;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.xml.XmlChainIterator;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

/**
 * Created 07/10/2021
 *
 * @author Edd
 */
public class JsonMessageConverterPerformanceChecker {
    private static JsonMessageConverter<UniRefEntryLight> jsonMessageConverter;

    @BeforeAll
    static void init() {
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF);
        ObjectMapper objectMapper =
                UniRefEntryLightJsonConfig.getInstance().getSimpleObjectMapper();
        jsonMessageConverter =
                new JsonMessageConverter<>(objectMapper, UniRefEntryLight.class, returnFieldConfig);
    }

    @Test
    void doIt() throws IOException {
        UniRefEntryLightConverter converter = new UniRefEntryLightConverter();
        String file = "/UniRef90_Q87VJ8.xml";
        InputStream is = JsonMessageConverterPerformanceChecker.class.getResourceAsStream(file);

        assertNotNull(is);

        List<InputStream> iss = Collections.singletonList(is);

        XmlChainIterator<Entry, Entry> chainingIterators =
                new XmlChainIterator<>(iss.iterator(), Entry.class, "entry", Function.identity());
        assertNotNull(chainingIterators);
        assertTrue(chainingIterators.hasNext());
        Entry xmlEntry = chainingIterators.next();
        assertNotNull(xmlEntry);
        UniRefEntryLight entry = converter.fromXml(xmlEntry);

        List<UniRefEntryLight> entities = new ArrayList<>();
        entities.add(entry);
        MessageConverterContext<UniRefEntryLight> messageContext =
                //
                // MessageConverterContext.<UniRefEntryLight>builder().fields("id,name,types,count,length,identity,organism,organism_id").build();
                MessageConverterContext.<UniRefEntryLight>builder().build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        jsonMessageConverter.before(messageContext, outputStream);
        Stopwatch watch = Stopwatch.createStarted();
        jsonMessageConverter.writeEntities(
                entities.stream(), outputStream, Instant.now(), new AtomicInteger(0));
        watch.stop();
        System.out.printf("Writing entry took %d ms", watch.elapsed(TimeUnit.MILLISECONDS));
        jsonMessageConverter.after(messageContext, outputStream);
        String result = outputStream.toString("UTF-8");

        System.out.println(result);
    }
}

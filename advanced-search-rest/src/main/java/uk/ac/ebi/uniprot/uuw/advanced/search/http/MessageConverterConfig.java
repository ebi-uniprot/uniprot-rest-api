package uk.ac.ebi.uniprot.uuw.advanced.search.http;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.xml.jaxb.uniprot.Entry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.EntryXmlConverterImpl;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.converter.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.converter.XmlMessageConverter.XML_MEDIA_TYPE;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@ConfigurationProperties(prefix = "download")
@Getter @Setter
public class MessageConverterConfig {
    private TaskExecutorProperties taskExecutor = new TaskExecutorProperties();

    @Bean
    public ThreadPoolTaskExecutor downloadTaskExecutor(ThreadPoolTaskExecutor configurableTaskExecutor) {
        configurableTaskExecutor.setCorePoolSize(taskExecutor.getCorePoolSize());
        configurableTaskExecutor.setMaxPoolSize(taskExecutor.getMaxPoolSize());
        configurableTaskExecutor.setQueueCapacity(taskExecutor.getQueueCapacity());
        configurableTaskExecutor.setKeepAliveSeconds(taskExecutor.getKeepAliveSeconds());
        configurableTaskExecutor.setAllowCoreThreadTimeOut(taskExecutor.isAllowCoreThreadTimeout());
        configurableTaskExecutor.setWaitForTasksToCompleteOnShutdown(taskExecutor.isWaitForTasksToCompleteOnShutdown());
        return configurableTaskExecutor;
    }

    @Bean
    public ThreadPoolTaskExecutor configurableTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    /*
     * Add to the supported message converters.
     * Add more message converters for additional response types.
     */
    @Bean
    public WebMvcConfigurer extendedMessageConverters() {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new FlatFileMessageConverter());
                converters.add(new ListMessageConverter());
                converters.add(new UniProtXmlMessageConverter());
                converters.add(new XmlMessageConverter());
            }
        };
    }

    @Bean
    public MessageConverterContextFactory messageConverterContextFactory() {
        MessageConverterContextFactory contextFactory = new MessageConverterContextFactory();

        contextFactory.addMessageConverterContexts(singletonList(uniProtXmlMessageConverterContext()));

        return contextFactory;
    }

    // TODO: 07/09/18 add uniprotFlatFile, uniprotList contexts
    // may need to provide a supplier/method that does the conversion of entry -> flatfile string
    private XmlMessageConverterContext uniProtXmlMessageConverterContext() {
        XmlMessageConverterContext<UniProtEntry, Entry> converter = new XmlMessageConverterContext<>();
        converter.setHeader("<uniprot>");
        converter.setFooter("</uniprot>");
        converter.setResource(Resource.UNIPROT);
        converter.setContext("uk.ac.ebi.kraken.xml.jaxb.uniprot");
        converter.setConverter(entry -> new EntryXmlConverterImpl().convert(entry));
        converter.setContentType(XML_MEDIA_TYPE);

        return converter;
    }

    // TODO: 07/09/18 extract class
    public static class MessageConverterContextFactory {
        private final Map<Pair, MessageConverterContext> converters = new HashMap<>();

        public void addMessageConverterContexts(List<MessageConverterContext> converters) {
            converters.forEach(converter -> {
                Pair pair = Pair.builder().contentType(converter.getContentType()).resource(converter.getResource())
                        .build();
                this.converters.put(pair, converter);
            });
        }

        public MessageConverterContext get(Resource resource, MediaType contentType) {
            Pair pair = Pair.builder().contentType(contentType).resource(resource).build();

            MessageConverterContext messageConverterContext = converters.get(pair);

            return messageConverterContext.asCopy();
        }
    }

    @Data
    @Builder
    private static class Pair {
        private Resource resource;
        private MediaType contentType;
    }

    public enum Resource {
        UNIPROT, UNIREF, UNIPARC
    }
}

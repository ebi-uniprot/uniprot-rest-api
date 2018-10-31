package uk.ac.ebi.uniprot.uuw.advanced.search.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2.ListMessageConverter;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2.uniprotkb.*;

import java.util.List;

import static java.util.Arrays.asList;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType.*;

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
//                converters.add(new FlatFileMessageConverter());
//                converters.add(new ListMessageConverter());
//                converters.add(0, new XmlMessageConverter());
//                converters.add(1, new JsonMessageConverter());
//                converters.add(new TSVMessageConverter());
//                converters.add(new XlsMessageConverter());
//                converters.add(new UniProtFastaMessageConverter());
//                converters.add(new GffMessageConverter());
                converters.add(new UniProtKBFlatFileMessageConverter());
                converters.add(new UniProtKBFastaMessageConverter());
                converters.add(new ListMessageConverter());
                converters.add(new UniProtKBGffMessageConverter());
                converters.add(new UniProtKBTsvMessageConverter());
                converters.add(new UniProtKBXslMessageConverter());
                converters.add(0, new UniProtKBXmlMessageConverter());
                converters.add(1, new UniProtKBJsonMessageConverter());
            }
        };
    }

    @Bean
    public MessageConverterContextFactory messageConverterContextFactory() {
        MessageConverterContextFactory contextFactory = new MessageConverterContextFactory();

        contextFactory.addMessageConverterContexts(asList(
                uniProtListMessageConverterContext(),
                uniProtFlatFileMessageConverterContext(),
                uniProtXmlMessageConverterContext(),
                uniprotJsonMessageConverterContext(),
                uniprotTSVMessageConverterContext(),
                uniprotFastaMessageConverterContext(),
                uniprotXlsMessageConverterContext(),
                uniProtGffMessageConverterContext()));

        return contextFactory;
    }

    private MessageConverterContext uniprotFastaMessageConverterContext() {
        MessageConverterContext converter = new MessageConverterContext();
        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
        converter.setContentType(FASTA_MEDIA_TYPE);
//        converter.setType(UniProtEntry.class);

        return converter;
    }

    private MessageConverterContext uniprotTSVMessageConverterContext() {
        MessageConverterContext converter = new MessageConverterContext();
        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
        converter.setContentType(TSV_MEDIA_TYPE);

        return converter;
    }

    private MessageConverterContext uniprotXlsMessageConverterContext() {
        MessageConverterContext converter = new MessageConverterContext();
        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
        converter.setContentType(XLS_MEDIA_TYPE);

        return converter;
    }

    private MessageConverterContext uniprotJsonMessageConverterContext() {
//        JsonMessageConverterContext converter = new JsonMessageConverterContext();
//        converter.setHeader("{\"results\" : [");
//        converter.setFooter("]}");

        MessageConverterContext converter = new MessageConverterContext();
        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
        converter.setContentType(MediaType.APPLICATION_JSON);

        return converter;
    }

    private MessageConverterContext uniProtXmlMessageConverterContext() {
//        XmlMessageConverterContext<UniProtEntry, Entry> converter = new XmlMessageConverterContext<>();
//        converter.setHeader("<uniprot xmlns=\"http://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniprot http://www.uniprot.org/support/docs/uniprot.xsd\">\n");
//        converter.setFooter("<copyright>\n" +
//                                    "Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms Distributed under the Creative Commons Attribution (CC BY 4.0) License\n" +
//                                    "</copyright>\n" +
//                                    "</uniprot>");
//        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
//        converter.setContext("uk.ac.ebi.kraken.xml.jaxb.uniprot");
//        final EntryXmlConverterImpl entryXmlConverter = new EntryXmlConverterImpl();
//        converter.setConverter(entryXmlConverter::convert);


        MessageConverterContext converter = new MessageConverterContext();
        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
        converter.setContentType(MediaType.APPLICATION_XML);

        return converter;
    }

    private MessageConverterContext uniProtFlatFileMessageConverterContext() {
        MessageConverterContext converter = new MessageConverterContext();
        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
        converter.setContentType(FF_MEDIA_TYPE);

        return converter;
    }

    private MessageConverterContext uniProtListMessageConverterContext() {
        MessageConverterContext converter = new MessageConverterContext();
        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
        converter.setContentType(LIST_MEDIA_TYPE);

        return converter;
    }

    private MessageConverterContext uniProtGffMessageConverterContext() {
        MessageConverterContext converter = new MessageConverterContext();
        converter.setResource(MessageConverterContextFactory.Resource.UNIPROT);
        converter.setContentType(GFF_MEDIA_TYPE);

        return converter;
    }
}

package uk.ac.ebi.uniprot.api.proteome.output;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ebi.uniprot.api.common.concurrency.TaskExecutorProperties;
import uk.ac.ebi.uniprot.api.proteome.output.converter.ProteomeJsonMessageConverter;
import uk.ac.ebi.uniprot.api.proteome.output.converter.ProteomeTsvMessageConverter;
import uk.ac.ebi.uniprot.api.proteome.output.converter.ProteomeXmlMessageConverter;
import uk.ac.ebi.uniprot.api.proteome.output.converter.ProteomeXslMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.rest.output.converter.ErrorMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.ErrorMessageXMLConverter;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
*/

@Configuration
@ConfigurationProperties(prefix = "download")
@Getter
@Setter
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
    @Bean
    public WebMvcConfigurer extendedMessageConverters() {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXMLConverter()); // to handle xml error messages
                converters.add(0, new ProteomeXmlMessageConverter());
                converters.add(1, new ProteomeJsonMessageConverter());
                converters.add(1, new ProteomeTsvMessageConverter());
                converters.add(1, new ProteomeXslMessageConverter());
            }
        };
    }
    @Bean
    public MessageConverterContextFactory<ProteomeEntry> messageConverterContextFactory() {
        MessageConverterContextFactory<ProteomeEntry> contextFactory = new MessageConverterContextFactory<>();

        asList(context(LIST_MEDIA_TYPE),
               context(APPLICATION_XML),
               context(APPLICATION_JSON),
               context(TSV_MEDIA_TYPE),
               context(XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<ProteomeEntry> context(MediaType contentType) {
        return MessageConverterContext.<ProteomeEntry>builder()
                .resource(MessageConverterContextFactory.Resource.PROTEOME)
                .contentType(contentType)
                .build();
    }
}

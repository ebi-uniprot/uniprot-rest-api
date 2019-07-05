package uk.ac.ebi.uniprot.api.support_data.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.ac.ebi.uniprot.api.common.concurrency.TaskExecutorProperties;
import uk.ac.ebi.uniprot.api.keyword.output.converter.KeywordJsonMessageConverter;
import uk.ac.ebi.uniprot.api.keyword.output.converter.KeywordTsvMessageConverter;
import uk.ac.ebi.uniprot.api.keyword.output.converter.KeywordXlsMessageConverter;
import uk.ac.ebi.uniprot.api.literature.output.converter.LiteratureJsonMessageConverter;
import uk.ac.ebi.uniprot.api.literature.output.converter.LiteratureXlsMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.ErrorMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.ListMessageConverter;
import uk.ac.ebi.uniprot.api.taxonomy.output.converter.TaxonomyJsonMessageConverter;
import uk.ac.ebi.uniprot.api.taxonomy.output.converter.TaxonomyTsvMessageConverter;
import uk.ac.ebi.uniprot.api.taxonomy.output.converter.TaxonomyXlsMessageConverter;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "download")
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
                converters.add(new ListMessageConverter());

                converters.add(new LiteratureXlsMessageConverter());
                converters.add(new LiteratureXlsMessageConverter());
                converters.add(0, new LiteratureJsonMessageConverter());

                converters.add(new TaxonomyXlsMessageConverter());
                converters.add(new TaxonomyTsvMessageConverter());
                converters.add(0, new TaxonomyJsonMessageConverter());

                converters.add(new KeywordXlsMessageConverter());
                converters.add(new KeywordTsvMessageConverter());
                converters.add(0, new KeywordJsonMessageConverter());
            }
        };
    }
}

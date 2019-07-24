package uk.ac.ebi.uniprot.api.support_data.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.ac.ebi.uniprot.api.common.concurrency.TaskExecutorProperties;
import uk.ac.ebi.uniprot.api.disease.response.converter.DiseaseOBOMessageConverter;
import uk.ac.ebi.uniprot.api.disease.response.converter.DiseaseTsvMessageConverter;
import uk.ac.ebi.uniprot.api.disease.response.converter.DiseaseXlsMessageConverter;
import uk.ac.ebi.uniprot.api.keyword.output.converter.KeywordTsvMessageConverter;
import uk.ac.ebi.uniprot.api.keyword.output.converter.KeywordXlsMessageConverter;
import uk.ac.ebi.uniprot.api.literature.output.converter.LiteratureTsvMessageConverter;
import uk.ac.ebi.uniprot.api.literature.output.converter.LiteratureXlsMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.ErrorMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.JsonMessageConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.ListMessageConverter;
import uk.ac.ebi.uniprot.api.taxonomy.output.converter.TaxonomyTsvMessageConverter;
import uk.ac.ebi.uniprot.api.taxonomy.output.converter.TaxonomyXlsMessageConverter;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.cv.keyword.KeywordEntry;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.json.parser.crossref.CrossRefJsonConfig;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.json.parser.keyword.KeywordJsonConfig;
import uk.ac.ebi.uniprot.json.parser.literature.LiteratureJsonConfig;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;
import uk.ac.ebi.uniprot.search.field.*;

import java.util.Arrays;
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
                converters.add(new LiteratureTsvMessageConverter());

                converters.add(new TaxonomyXlsMessageConverter());
                converters.add(new TaxonomyTsvMessageConverter());

                converters.add(new KeywordXlsMessageConverter());
                converters.add(new KeywordTsvMessageConverter());

                // add Json message converter first in the list because it is the most used
                JsonMessageConverter<LiteratureEntry> litJsonConverter =
                        new JsonMessageConverter<>(LiteratureJsonConfig.getInstance().getSimpleObjectMapper(),
                                LiteratureEntry.class, Arrays.asList(LiteratureField.ResultFields.values()));

                converters.add(0, litJsonConverter);

                JsonMessageConverter<KeywordEntry> keywordJsonConverter =
                        new JsonMessageConverter<>(KeywordJsonConfig.getInstance().getSimpleObjectMapper(),
                                KeywordEntry.class, Arrays.asList(KeywordField.ResultFields.values()));

                converters.add(0, keywordJsonConverter);

                JsonMessageConverter<TaxonomyEntry> taxonomyJsonConverter =
                        new JsonMessageConverter<>(TaxonomyJsonConfig.getInstance().getSimpleObjectMapper(),
                                TaxonomyEntry.class, Arrays.asList(TaxonomyField.ResultFields.values()));

                converters.add(0, taxonomyJsonConverter);

                converters.add(new DiseaseXlsMessageConverter());
                converters.add(new DiseaseTsvMessageConverter());

                JsonMessageConverter<Disease> diseaseJsonConverter =
                        new JsonMessageConverter(DiseaseJsonConfig.getInstance().getSimpleObjectMapper(),
                                Disease.class, Arrays.asList(DiseaseField.ResultFields.values()));
                converters.add(0, diseaseJsonConverter);
                converters.add(1, new DiseaseOBOMessageConverter());

                JsonMessageConverter<CrossRefEntry> xrefJsonConverter =
                        new JsonMessageConverter<>(CrossRefJsonConfig.getInstance().getSimpleObjectMapper(),
                                CrossRefEntry.class, Arrays.asList(CrossRefField.ResultFields.values()));

                converters.add(0, xrefJsonConverter);
            }
        };
    }
}

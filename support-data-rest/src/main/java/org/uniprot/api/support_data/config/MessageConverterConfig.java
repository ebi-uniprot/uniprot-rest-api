package org.uniprot.api.support_data.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.TaskExecutorProperties;
import org.uniprot.api.disease.response.converter.DiseaseOBOMessageConverter;
import org.uniprot.api.disease.response.converter.DiseaseTsvMessageConverter;
import org.uniprot.api.disease.response.converter.DiseaseXlsMessageConverter;
import org.uniprot.api.keyword.output.converter.KeywordTsvMessageConverter;
import org.uniprot.api.keyword.output.converter.KeywordXlsMessageConverter;
import org.uniprot.api.literature.output.converter.LiteratureTsvMessageConverter;
import org.uniprot.api.literature.output.converter.LiteratureXlsMessageConverter;
import org.uniprot.api.rest.output.converter.ErrorMessageConverter;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.output.converter.ListMessageConverter;
import org.uniprot.api.subcell.output.converter.SubcellularLocationOBOMessageConverter;
import org.uniprot.api.subcell.output.converter.SubcellularLocationTsvMessageConverter;
import org.uniprot.api.subcell.output.converter.SubcellularLocationXlsMessageConverter;
import org.uniprot.api.taxonomy.output.converter.TaxonomyTsvMessageConverter;
import org.uniprot.api.taxonomy.output.converter.TaxonomyXlsMessageConverter;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.core.json.parser.crossref.CrossRefJsonConfig;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.json.parser.subcell.SubcellularLocationJsonConfig;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.search.field.*;

@Configuration
@ConfigurationProperties(prefix = "download")
public class MessageConverterConfig {
    private TaskExecutorProperties taskExecutor = new TaskExecutorProperties();

    @Bean
    public ThreadPoolTaskExecutor downloadTaskExecutor(
            ThreadPoolTaskExecutor configurableTaskExecutor) {
        configurableTaskExecutor.setCorePoolSize(taskExecutor.getCorePoolSize());
        configurableTaskExecutor.setMaxPoolSize(taskExecutor.getMaxPoolSize());
        configurableTaskExecutor.setQueueCapacity(taskExecutor.getQueueCapacity());
        configurableTaskExecutor.setKeepAliveSeconds(taskExecutor.getKeepAliveSeconds());
        configurableTaskExecutor.setAllowCoreThreadTimeOut(taskExecutor.isAllowCoreThreadTimeout());
        configurableTaskExecutor.setWaitForTasksToCompleteOnShutdown(
                taskExecutor.isWaitForTasksToCompleteOnShutdown());
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

                converters.add(new SubcellularLocationXlsMessageConverter());
                converters.add(new SubcellularLocationTsvMessageConverter());
                converters.add(new SubcellularLocationOBOMessageConverter());

                converters.add(new DiseaseXlsMessageConverter());
                converters.add(new DiseaseTsvMessageConverter());
                converters.add(new DiseaseOBOMessageConverter());

                // add Json message converter first in the list because it is the most used
                JsonMessageConverter<LiteratureEntry> litJsonConverter =
                        new JsonMessageConverter<>(
                                LiteratureJsonConfig.getInstance().getSimpleObjectMapper(),
                                LiteratureEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                                UniProtDataType.LITERATURE)
                                        .getReturnFields());
                converters.add(0, litJsonConverter);

                JsonMessageConverter<KeywordEntry> keywordJsonConverter =
                        new JsonMessageConverter<>(
                                KeywordJsonConfig.getInstance().getSimpleObjectMapper(),
                                KeywordEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                                UniProtDataType.KEYWORD)
                                        .getReturnFields());
                converters.add(0, keywordJsonConverter);

                JsonMessageConverter<TaxonomyEntry> taxonomyJsonConverter =
                        new JsonMessageConverter<>(
                                TaxonomyJsonConfig.getInstance().getSimpleObjectMapper(),
                                TaxonomyEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                                UniProtDataType.TAXONOMY)
                                        .getReturnFields());
                converters.add(0, taxonomyJsonConverter);

                JsonMessageConverter<SubcellularLocationEntry> subcellJsonConverter =
                        new JsonMessageConverter<>(
                                SubcellularLocationJsonConfig.getInstance().getSimpleObjectMapper(),
                                SubcellularLocationEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                                UniProtDataType.SUBCELLLOCATION)
                                        .getReturnFields());
                converters.add(0, subcellJsonConverter);

                JsonMessageConverter<DiseaseEntry> diseaseJsonConverter =
                        new JsonMessageConverter<>(
                                DiseaseJsonConfig.getInstance().getSimpleObjectMapper(),
                                DiseaseEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                                UniProtDataType.DISEASE)
                                        .getReturnFields());
                converters.add(0, diseaseJsonConverter);

                JsonMessageConverter<CrossRefEntry> xrefJsonConverter =
                        new JsonMessageConverter<>(
                                CrossRefJsonConfig.getInstance().getSimpleObjectMapper(),
                                CrossRefEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                                UniProtDataType.CROSSREF)
                                        .getReturnFields());
                converters.add(0, xrefJsonConverter);
            }
        };
    }
}

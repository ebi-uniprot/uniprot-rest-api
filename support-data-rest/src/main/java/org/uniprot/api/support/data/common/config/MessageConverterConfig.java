package org.uniprot.api.support.data.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.TaskExecutorProperties;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.api.support.data.disease.response.DiseaseOBOMessageConverter;
import org.uniprot.api.support.data.subcellular.response.SubcellularLocationOBOMessageConverter;
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
import org.uniprot.core.parser.tsv.disease.DiseaseEntryValueMapper;
import org.uniprot.core.parser.tsv.keyword.KeywordEntryValueMapper;
import org.uniprot.core.parser.tsv.literature.LiteratureEntryValueMapper;
import org.uniprot.core.parser.tsv.subcell.SubcellularLocationEntryValueMapper;
import org.uniprot.core.parser.tsv.taxonomy.TaxonomyEntryValueMapper;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

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

                ReturnFieldConfig litReturnConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.LITERATURE);
                converters.add(
                        new XlsMessageConverter<>(
                                LiteratureEntry.class,
                                litReturnConfig,
                                new LiteratureEntryValueMapper()));
                converters.add(
                        new TsvMessageConverter<>(
                                LiteratureEntry.class,
                                litReturnConfig,
                                new LiteratureEntryValueMapper()));

                ReturnFieldConfig taxReturnConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.TAXONOMY);
                converters.add(
                        new XlsMessageConverter<>(
                                TaxonomyEntry.class,
                                taxReturnConfig,
                                new TaxonomyEntryValueMapper()));
                converters.add(
                        new TsvMessageConverter<>(
                                TaxonomyEntry.class,
                                taxReturnConfig,
                                new TaxonomyEntryValueMapper()));

                ReturnFieldConfig kwReturnFields =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.KEYWORD);
                converters.add(
                        new XlsMessageConverter<>(
                                KeywordEntry.class, kwReturnFields, new KeywordEntryValueMapper()));
                converters.add(
                        new TsvMessageConverter<>(
                                KeywordEntry.class, kwReturnFields, new KeywordEntryValueMapper()));

                ReturnFieldConfig subcellReturnFields =
                        ReturnFieldConfigFactory.getReturnFieldConfig(
                                UniProtDataType.SUBCELLLOCATION);
                converters.add(
                        new XlsMessageConverter<>(
                                SubcellularLocationEntry.class,
                                subcellReturnFields,
                                new SubcellularLocationEntryValueMapper()));
                converters.add(
                        new TsvMessageConverter<>(
                                SubcellularLocationEntry.class,
                                subcellReturnFields,
                                new SubcellularLocationEntryValueMapper()));
                converters.add(new SubcellularLocationOBOMessageConverter());

                ReturnFieldConfig diseaseReturnFields =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.DISEASE);
                converters.add(
                        new XlsMessageConverter<>(
                                DiseaseEntry.class,
                                diseaseReturnFields,
                                new DiseaseEntryValueMapper()));
                converters.add(
                        new TsvMessageConverter<>(
                                DiseaseEntry.class,
                                diseaseReturnFields,
                                new DiseaseEntryValueMapper()));
                converters.add(new DiseaseOBOMessageConverter());

                // add Json message converter first in the list because it is the most used
                JsonMessageConverter<LiteratureEntry> litJsonConverter =
                        new JsonMessageConverter<>(
                                LiteratureJsonConfig.getInstance().getSimpleObjectMapper(),
                                LiteratureEntry.class,
                                litReturnConfig);
                converters.add(0, litJsonConverter);

                JsonMessageConverter<KeywordEntry> keywordJsonConverter =
                        new JsonMessageConverter<>(
                                KeywordJsonConfig.getInstance().getSimpleObjectMapper(),
                                KeywordEntry.class,
                                kwReturnFields);
                converters.add(0, keywordJsonConverter);

                JsonMessageConverter<TaxonomyEntry> taxonomyJsonConverter =
                        new JsonMessageConverter<>(
                                TaxonomyJsonConfig.getInstance().getSimpleObjectMapper(),
                                TaxonomyEntry.class,
                                taxReturnConfig);
                converters.add(0, taxonomyJsonConverter);

                JsonMessageConverter<SubcellularLocationEntry> subcellJsonConverter =
                        new JsonMessageConverter<>(
                                SubcellularLocationJsonConfig.getInstance().getSimpleObjectMapper(),
                                SubcellularLocationEntry.class,
                                subcellReturnFields);
                converters.add(0, subcellJsonConverter);

                JsonMessageConverter<DiseaseEntry> diseaseJsonConverter =
                        new JsonMessageConverter<>(
                                DiseaseJsonConfig.getInstance().getSimpleObjectMapper(),
                                DiseaseEntry.class,
                                diseaseReturnFields);
                converters.add(0, diseaseJsonConverter);

                JsonMessageConverter<CrossRefEntry> xrefJsonConverter =
                        new JsonMessageConverter<>(
                                CrossRefJsonConfig.getInstance().getSimpleObjectMapper(),
                                CrossRefEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.CROSSREF));
                converters.add(0, xrefJsonConverter);
            }
        };
    }
}

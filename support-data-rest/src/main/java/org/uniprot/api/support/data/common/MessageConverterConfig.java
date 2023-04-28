package org.uniprot.api.support.data.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.api.support.data.disease.response.DiseaseOBOMessageConverter;
import org.uniprot.api.support.data.keyword.response.KeywordOBOMessageConverter;
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

import java.util.List;

@Configuration
public class MessageConverterConfig {
    @Bean
    public WebMvcConfigurer extendedMessageConverters(Gatekeeper downloadGatekeeper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                int index = 0;

                converters.add(index++, new ErrorMessageConverter());
                converters.add(index++, new ErrorMessageXlsConverter());
                converters.add(index++, new ErrorMessageTurtleConverter());
                converters.add(index++, new ErrorMessageNTriplesConverter());
                converters.add(index++, new ListMessageConverter(downloadGatekeeper));

                ReturnFieldConfig litReturnConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.LITERATURE);
                converters.add(
                        index++,
                        new XlsMessageConverter<>(
                                LiteratureEntry.class,
                                litReturnConfig,
                                new LiteratureEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                LiteratureEntry.class,
                                litReturnConfig,
                                new LiteratureEntryValueMapper(),
                                downloadGatekeeper));

                ReturnFieldConfig taxReturnConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.TAXONOMY);
                converters.add(
                        index++,
                        new XlsMessageConverter<>(
                                TaxonomyEntry.class,
                                taxReturnConfig,
                                new TaxonomyEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                TaxonomyEntry.class,
                                taxReturnConfig,
                                new TaxonomyEntryValueMapper(),
                                downloadGatekeeper));

                ReturnFieldConfig kwReturnFields =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.KEYWORD);
                converters.add(
                        index++,
                        new XlsMessageConverter<>(
                                KeywordEntry.class,
                                kwReturnFields,
                                new KeywordEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                KeywordEntry.class,
                                kwReturnFields,
                                new KeywordEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(index++, new KeywordOBOMessageConverter(downloadGatekeeper));

                ReturnFieldConfig subcellReturnFields =
                        ReturnFieldConfigFactory.getReturnFieldConfig(
                                UniProtDataType.SUBCELLLOCATION);
                converters.add(
                        index++,
                        new XlsMessageConverter<>(
                                SubcellularLocationEntry.class,
                                subcellReturnFields,
                                new SubcellularLocationEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                SubcellularLocationEntry.class,
                                subcellReturnFields,
                                new SubcellularLocationEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        index++, new SubcellularLocationOBOMessageConverter(downloadGatekeeper));

                ReturnFieldConfig diseaseReturnFields =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.DISEASE);
                converters.add(
                        index++,
                        new XlsMessageConverter<>(
                                DiseaseEntry.class,
                                diseaseReturnFields,
                                new DiseaseEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(
                        index++,
                        new TsvMessageConverter<>(
                                DiseaseEntry.class,
                                diseaseReturnFields,
                                new DiseaseEntryValueMapper(),
                                downloadGatekeeper));
                converters.add(index++, new DiseaseOBOMessageConverter(downloadGatekeeper));

                // add Json message converter first in the list because it is the most used
                JsonMessageConverter<LiteratureEntry> litJsonConverter =
                        new JsonMessageConverter<>(
                                LiteratureJsonConfig.getInstance().getSimpleObjectMapper(),
                                LiteratureEntry.class,
                                litReturnConfig,
                                downloadGatekeeper);
                converters.add(index++, litJsonConverter);

                JsonMessageConverter<KeywordEntry> keywordJsonConverter =
                        new JsonMessageConverter<>(
                                KeywordJsonConfig.getInstance().getSimpleObjectMapper(),
                                KeywordEntry.class,
                                kwReturnFields,
                                downloadGatekeeper);
                converters.add(index++, keywordJsonConverter);

                JsonMessageConverter<TaxonomyEntry> taxonomyJsonConverter =
                        new JsonMessageConverter<>(
                                TaxonomyJsonConfig.getInstance().getSimpleObjectMapper(),
                                TaxonomyEntry.class,
                                taxReturnConfig,
                                downloadGatekeeper);
                converters.add(index++, taxonomyJsonConverter);

                JsonMessageConverter<SubcellularLocationEntry> subcellJsonConverter =
                        new JsonMessageConverter<>(
                                SubcellularLocationJsonConfig.getInstance().getSimpleObjectMapper(),
                                SubcellularLocationEntry.class,
                                subcellReturnFields,
                                downloadGatekeeper);
                converters.add(index++, subcellJsonConverter);

                JsonMessageConverter<DiseaseEntry> diseaseJsonConverter =
                        new JsonMessageConverter<>(
                                DiseaseJsonConfig.getInstance().getSimpleObjectMapper(),
                                DiseaseEntry.class,
                                diseaseReturnFields,
                                downloadGatekeeper);
                converters.add(index++, diseaseJsonConverter);

                JsonMessageConverter<CrossRefEntry> xrefJsonConverter =
                        new JsonMessageConverter<>(
                                CrossRefJsonConfig.getInstance().getSimpleObjectMapper(),
                                CrossRefEntry.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.CROSSREF),
                                downloadGatekeeper);
                converters.add(index++, xrefJsonConverter);
                converters.add(index, new RdfMessageConverter(downloadGatekeeper));
                converters.add(index, new TurtleMessageConverter(downloadGatekeeper));
                converters.add(index, new NTriplesMessageConverter(downloadGatekeeper));
            }
        };
    }
}

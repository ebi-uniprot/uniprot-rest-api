package uk.ac.ebi.uniprot.api.uniprotkb.output.converter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import uk.ac.ebi.uniprot.api.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.api.uniprotkb.service.filters.EntryFilters;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.parser.tsv.uniprot.EntryMap;

public class UniProtKBTsvMessageConverter extends AbstractTsvMessagerConverter<UniProtEntry> {
    private ThreadLocal<Map<String, List<String>>> tlFilters = new ThreadLocal<>();
    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

    
    @Override
	protected void initBefore(MessageConverterContext<UniProtEntry> context) {
    	   tlFilters.set(FieldsParser.parseForFilters(context.getFields()));
           tlFields.set(FieldsParser.parse(context.getFields()));
	}

	@Override
	protected List<String> getHeader() {
		List<String> fields = tlFields.get();
		return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());

	}

	@Override
	protected List<String> entry2TsvStrings(UniProtEntry entity) {
		Map<String, List<String>> filterParams=tlFilters.get();
		
		List<String> fields =tlFields.get();
		  if ((filterParams != null) && !filterParams.isEmpty())
	            EntryFilters.filterEntry(entity, filterParams);
	        EntryMap dlEntry = new EntryMap(entity, fields);
	        return dlEntry.getData();
		
	
	}

    private String getFieldDisplayName(String field) {
        Optional<Field> opField = UniProtResultFields.INSTANCE.getField(field);
        if (opField.isPresent())
            return opField.get().getLabel();
        else
            return field;
    }

}

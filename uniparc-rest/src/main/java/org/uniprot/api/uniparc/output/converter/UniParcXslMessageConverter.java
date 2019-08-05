package org.uniprot.api.uniparc.output.converter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.uniprot.api.configure.uniparc.UniParcResultFields;
import org.uniprot.api.configure.uniprot.domain.Field;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXslMessegerConverter;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.uniparc.request.UniParcRequest;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryMap;
import org.uniprot.core.uniparc.UniParcEntry;

/**
 *
 * @author jluo
 * @date: 25 Jun 2019
 *
*/

public class UniParcXslMessageConverter extends AbstractXslMessegerConverter<UniParcEntry> {
	    private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

	    public UniParcXslMessageConverter() {
	        super(UniParcEntry.class);
	    }

	    @Override
	    protected void initBefore(MessageConverterContext<UniParcEntry> context) {
	    	tlFields.set(OutputFieldsParser.parse(context.getFields(), UniParcRequest.DEFAULT_FIELDS));
	    }

	    @Override
	    protected List<String> getHeader() {
	        List<String> fields = tlFields.get();
	        return fields.stream().map(this::getFieldDisplayName).collect(Collectors.toList());

	    }

	    @Override
	    protected List<String> entry2TsvStrings(UniParcEntry entity) {
	    	return new UniParcEntryMap(entity, tlFields.get()).getData();
	    }

	    private String getFieldDisplayName(String field) {
			Optional<Field> opField = UniParcResultFields.INSTANCE.getField(field);
			if (opField.isPresent())
				return opField.get().getLabel();
			else
				return field;
		}

}


package uk.ac.ebi.uniprot.api.uniparc.output.converter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import uk.ac.ebi.uniprot.api.configure.uniparc.UniParcResultFields;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractTsvMessagerConverter;
import uk.ac.ebi.uniprot.api.rest.output.converter.OutputFieldsParser;
import uk.ac.ebi.uniprot.api.uniparc.request.UniParcRequest;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.parser.tsv.uniparc.UniParcEntryMap;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class UniParcTsvMessageConverter extends AbstractTsvMessagerConverter<UniParcEntry> {
	private ThreadLocal<List<String>> tlFields = new ThreadLocal<>();

	public UniParcTsvMessageConverter() {
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

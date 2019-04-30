package uk.ac.ebi.uniprot.api.proteome.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
*/

public class ProteomeXslMessageConverter extends AbstractEntityHttpMessageConverter<ProteomeEntry> {
	   public ProteomeXslMessageConverter() {
	        super(UniProtMediaType.XLS_MEDIA_TYPE);
	    }

	@Override
	protected void writeEntity(ProteomeEntry entity, OutputStream outputStream) throws IOException {
		// TODO Auto-generated method stub
		
	}

}


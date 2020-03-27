package org.uniprot.api.unisave.service.flatfile;

import com.google.inject.ImplementedBy;
import org.uniprot.api.unisave.repository.domain.Entry;
import org.uniprot.api.unisave.service.flatfile.impl.StringEntryBuilderImpl;

/**
 * Build Entry from a plain string.
 *
 * @author wudong
 */
@ImplementedBy(StringEntryBuilderImpl.class)
public interface StringEntryBuilder {
	Entry build(String content);
    String getSequence();
}

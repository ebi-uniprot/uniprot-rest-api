package org.uniprot.api.unisave.repository.domain;

import com.google.inject.ImplementedBy;
import org.uniprot.api.unisave.repository.domain.impl.EntryBuilderImpl;

@ImplementedBy(EntryBuilderImpl.class)
public interface EntryBuilder {
	void setDatabase(DatabaseEnum d);

	void setIdentifier(String i);

	void setSequenceVersion(int s);

	void setEntryVersion(int e);

	void setEntryMD5(String md5);

	void setSequenceMD5(String smd5);

	void setEntryContent(EntryContent c);

    void setName(String name);

	public boolean asseccionSet();

	public void reset();

	Entry build();

}

package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.Entry;
import org.uniprot.api.unisave.repository.domain.EntryBuilder;
import org.uniprot.api.unisave.repository.domain.EntryContent;

import java.util.ArrayList;
import java.util.List;

public class EntryBuilderImpl implements EntryBuilder {

	private DatabaseEnum database;

	private String identifier;
    private String name;
	private int sequenceVersion;
	private int entryVersion;
	private String entryMD5;
	private String sequenceMD5;
	private EntryContent entryContent;

	private List<String> secondaryAcc = new ArrayList<String>();

	// private String sequence;

	public void setDatabase(DatabaseEnum database) {
		this.database = database;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public void setSequenceVersion(int sequenceVersion) {
		this.sequenceVersion = sequenceVersion;
	}

	public void setEntryVersion(int entryVersion) {
		this.entryVersion = entryVersion;
	}

	public void setEntryMD5(String entryMD5) {
		this.entryMD5 = entryMD5;
	}

	public void setSequenceMD5(String sequenceMD5) {
		this.sequenceMD5 = sequenceMD5;
	}

	public void setEntryContent(EntryContent entryContent) {
		this.entryContent = entryContent;
	}

    public void setName(String name){
        this.name = name;
    }

	@Override
	public Entry build() {
		if (entryContent == null)
			throw new RuntimeException("Entry content cannot be empty.");

		if (identifier == null || identifier.isEmpty())
			throw new RuntimeException("Entry accession cannot be empty: "
					+ entryContent);

        if (name == null || name.isEmpty())
            throw new RuntimeException("Entry accession cannot be empty: "
                    + entryContent);

		if (sequenceMD5 == null || sequenceMD5.isEmpty())
			throw new RuntimeException("Entry Sequence MD5 cannot be empty: "
					+ entryContent);

		if (entryMD5 == null || entryMD5.isEmpty())
			throw new RuntimeException("Entry MD5 cannot be empty: "
					+ entryContent);

		if (sequenceVersion == 0) {

		}

		if (entryVersion == 0) {
			//TODO
		}

		EntryImpl defaultEntryImpl = new EntryImpl();
		defaultEntryImpl.setDatabase(database);
		defaultEntryImpl.setEntryContent(entryContent);
		defaultEntryImpl.setEntryMD5(entryMD5);
		defaultEntryImpl.setEntryVersion(entryVersion);
		defaultEntryImpl.setAccession(identifier);
		defaultEntryImpl.setSequenceMD5(sequenceMD5);
		defaultEntryImpl.setSequenceVersion(sequenceVersion);
        defaultEntryImpl.setName(name);
		return defaultEntryImpl;
	}

	@Override
	public void reset() {
		this.database = null;
		this.identifier = null;
		this.sequenceVersion = 0;
		this.entryContent = null;
		this.entryMD5 = null;
		this.entryVersion = 0;
        this.name = null;
		// this.sequence = null;
		this.sequenceMD5 = null;
	}

	@Override
	public boolean asseccionSet() {
		return this.identifier != null;
	}

}

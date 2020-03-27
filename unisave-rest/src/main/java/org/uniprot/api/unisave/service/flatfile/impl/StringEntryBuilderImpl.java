package org.uniprot.api.unisave.service.flatfile.impl;

import com.google.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;
import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.Entry;
import org.uniprot.api.unisave.repository.domain.EntryBuilder;
import org.uniprot.api.unisave.repository.domain.impl.EntryContentImpl;
import org.uniprot.api.unisave.service.flatfile.StringEntryBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class StringEntryBuilderImpl implements StringEntryBuilder {

	// private String accession;

	private final EntryBuilder builder;
    private String sequence;

	@Inject
	public StringEntryBuilderImpl(EntryBuilder builder) {
		this.builder = builder;
	}

	/**
	 * <pre>
	 * ID   001R_FRG3G              Reviewed;         256 AA.
	 * AC   Q6GZX4;
	 * DT   28-JUN-2011, integrated into UniProtKB/Swiss-Prot.
	 * DT   19-JUL-2004, sequence version 1.
	 * DT   21-SEP-2011, entry version 23.
	 *
	 * </pre>
	 */
	@Override
	public Entry build(String content) {
		EntryBuilder entryImpl = builder;
		entryImpl.reset();
		entryImpl.setEntryMD5(digest(content));
		EntryContentImpl entryContentImpl = new EntryContentImpl();
		entryContentImpl.setFullcontent(content);
		entryImpl.setEntryContent(entryContentImpl);

		BufferedReader br = new BufferedReader(new StringReader(content));
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
                if (line.startsWith("ID")) {
                    processIdLine(entryImpl,line);
                } else if (line.startsWith("AC")) {
					processACLine(entryImpl, line);
				} else if (line.startsWith("DT")) {
					processDTLine(entryImpl, line);
				} else if (line.startsWith("SQ")) {
					processSQLine(entryImpl, br);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error in parse the content", e);
		}
		return entryImpl.build();
	}

    @Override
    public String getSequence() {
        return this.sequence;
    }

    private void processSQLine(EntryBuilder entryImpl, BufferedReader br) {
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			while ((line = br.readLine()) != null && !line.equals("//")) {
				sb.append(line.trim());
			}
		} catch (IOException e) {
			throw new RuntimeException("Error in parse the content", e);
		}

		assert line.equals("//") : "Error in parse the content";

		String string = sb.toString();
		String replaceAll = string.replaceAll("\\s+", "");
        this.sequence =replaceAll;
		// entryImpl.setSequence(replaceAll);
		entryImpl.setSequenceMD5(digest(replaceAll));
	}

	private void processACLine(EntryBuilder entryImpl, String line) {
		//only the first is set.
		if (entryImpl.asseccionSet())
			return;
		String substring = line.substring(3).trim();
		String[] split = substring.split(";");
		entryImpl.setIdentifier(split[0].trim());
	/*	if (split.length > 1) {
	    for (int i = 1; i < split.length; i++)
		entryImpl.appendSecondaryAcc(split[i].trim());
	}*/

	}

    //ID   001R_FRG3G              Reviewed;         256 AA.
    private void processIdLine(EntryBuilder entryImpl, String line) {
        //only the first is set.
        int index = line.indexOf(" ", 5);
        String substring = line.substring(5, index).trim();
        entryImpl.setName(substring);
    }

	// sequence version 1.
	// entry version 23.
	private void processDTLine(EntryBuilder entryImpl, String line) {
		int index;
		if ((index = line.indexOf("sequence version")) > 0) {
			String substring = line.substring(
					index + "sequence version".length() + 1, line.length() - 1)
					.trim();
			entryImpl.setSequenceVersion(Integer.parseInt(substring));
		} else if ((index = line.indexOf("entry version")) > 0) {
			String substring = line.substring(
					index + "entry version".length() + 1, line.length() - 1)
					.trim();
			entryImpl.setEntryVersion(Integer.parseInt(substring));
		} else if (line.indexOf("Swiss-Prot") > 0) {
			entryImpl.setDatabase(DatabaseEnum.Swissprot);
		} else if (line.indexOf("TrEMBL") > 0) {
			entryImpl.setDatabase(DatabaseEnum.Trembl);
		}
	}

	private String digest(String s) {
		return DigestUtils.md5Hex(s);
	}

}

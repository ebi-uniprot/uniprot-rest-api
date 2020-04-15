package org.uniprot.api.unisave.repository.domain;

import java.util.List;

import org.uniprot.api.unisave.repository.domain.impl.EntryIndexImpl;

import com.google.inject.ImplementedBy;

/**
 * The Entry's Index includes the information of the entry's content like MD5 and its position in
 * the flatfile.
 *
 * @author wudong
 */
@ImplementedBy(EntryIndexImpl.class)
public interface EntryIndex {

    /**
     * The starts position of the Entry in the FF that is being loaded.
     *
     * @return
     */
    public abstract long getStartPosition();

    /**
     * The length of the entry.
     *
     * @return
     */
    public abstract int getLength();

    /**
     * The accession of the entry.
     *
     * @return
     */
    public abstract String getAccession();

    /**
     * The Database of the entry.
     *
     * @return
     */
    public abstract DatabaseEnum getDb();

    /**
     * The MD5 of the entry content.
     *
     * @return
     */
    public abstract String getMd5();

    /**
     * The secondary accession of this entry.
     *
     * @return
     */
    public abstract List<String> getSecAccs();

    public abstract String toCSVString();

    int getVersion();
}

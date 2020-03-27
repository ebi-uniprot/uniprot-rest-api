package org.uniprot.api.unisave.repository.domain;

import java.util.Date;

/**
 * Keep track of the loading process in the database. It represents the loading
 * of a single FF into the unisave database. A release can (and normally will)
 * have more than one load.
 * <p/>
 * <p/>
 * A load is phased. and each phase can be started sperately, but they should be
 * executed in the predefined order. By examine the load information we should
 * know how the loading is going and if it fail, where we can resume.
 *
 * @author wudong
 */
public interface Load {

	/**
	 * The file path being load.
	 *
	 * @return
	 */
	String getFilePath();

	/**
	 * The MD5 of the file being load.
	 *
	 * @return
	 */
	String getFileMD5();

	/**
	 * When the load started.
	 *
	 * @return
	 */
	Date startTime();

	/**
	 * When the load finished.
	 *
	 * @return
	 */
	Date finishTime();

	Release getRelease();

	/**
	 * Starting the given phase in the load.
	 *
	 * @param phase
	 */
	void startPhase(LoadPhase phase);

	/**
	 * Finish the given phase in the load.
	 *
	 * @param phase
	 */
	void finishPhase(LoadPhase phase);

}

package org.uniprot.api.unisave.repository.domain;

/**
 * The loading phases of a single unisave loading procedure.
 *
 * @author wudong
 */
public enum LoadPhase {
	//not started yet.
	NotStarted,
	//started loading Index.
	LoadIndex,
	//finished loading index and started build index table.
	BuildIndexTable,
	//finished build index table and start query the index on which entry need to be executed.
	QueryIndex,
	//finished the query and use the query result to load entry.
	LoadEntry,
	//finished load and start updating the status of Identifier.
	UpdateIdentifier,
	//load has been finished by this stage.
	Done
}

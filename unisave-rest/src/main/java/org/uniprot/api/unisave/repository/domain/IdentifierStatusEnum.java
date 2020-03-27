package org.uniprot.api.unisave.repository.domain;

public enum IdentifierStatusEnum {
	/**
	 * The Identifier is currently an active primary identifier.
	 */
	A,
	/**
	 * The identifier has been merged to a primary identifier, which makes it a
	 * secondary identifier.
	 */
	M,
	/**
	 * The Identifier has been marked as deleted.
	 */
	D,
	/**
	 * The Identifier has been with withdrawn from public domain.
	 */
	W;
}

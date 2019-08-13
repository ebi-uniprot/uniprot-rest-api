package org.uniprot.api.uniprotkb.view.service;

public class UniProtViewByServiceException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public UniProtViewByServiceException() {
		super();
	}
	public UniProtViewByServiceException(String message) {
		super(message);
	}

	public UniProtViewByServiceException(String message, Throwable throwable) {
		super(message, throwable);
	}
	public UniProtViewByServiceException(Throwable throwable) {
		super(throwable);
	}

}

package documentgeneration.implementation.exceptions;

import com.mendix.systemwideinterfaces.MendixRuntimeException;

import documentgeneration.implementation.Logging;

public class DocGenException extends MendixRuntimeException{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4333679816180750217L;

	public DocGenException(String message) {
		super(message);
		Logging.logNode.error(message);
	}
}

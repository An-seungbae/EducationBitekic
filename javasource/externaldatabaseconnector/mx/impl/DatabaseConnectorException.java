package externaldatabaseconnector.mx.impl;

import externaldatabaseconnector.exceptions.MxException;

public class DatabaseConnectorException extends MxException {
	public DatabaseConnectorException(String msg) {
		super(msg, null);
	}
	
	public DatabaseConnectorException(String msg, Exception e) {
		super(msg, e);
	}
}

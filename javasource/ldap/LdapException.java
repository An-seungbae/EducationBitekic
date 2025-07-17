package ldap;

import com.mendix.systemwideinterfaces.MendixException;



public class LdapException extends MendixException {


	private static final long serialVersionUID = 1468946376108926089L;

	public LdapException() {
		super();
	}
	
	public LdapException(String errMsg) 
	{
		super(errMsg);
	}
	
	public LdapException(String errMsg, Throwable cause) 
	{
		super(errMsg, cause);
	}
	
	public LdapException(Throwable cause) 
	{
		super(cause);
	}


}

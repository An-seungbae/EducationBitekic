package ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.springframework.ldap.core.NameClassPairCallbackHandler;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import communitycommons.XPath;
import ldap.proxies.LdapGroup;
import ldap.proxies.LdapPath;
import ldap.proxies.LdapServer;
import ldap.proxies.ServerConfigType;
import ldap.proxies.TestAttribute;
import ldap.proxies.TestEntry;

public class TestImportItems {

	// MWE: Note, some fields are not private to make them visible in wrapped classes. But a reference to an instance of the class should 
	//never be published to outside this class, to that is safe.
	private final LdapServer ldapServer;
	final String userNameField;
	final String searchFilter;
	long importCount = 0;
	long errorCount = 0;
	final IContext	context = Core.createSystemContext();
	
	boolean	hasImportErrors = false;
	
	private static final ILogNode log = Core.getLogger(LdapModule.LDAP_LOG);
	
	public static synchronized String runImport(LdapServer ldapServer) throws Exception {
		log.info("Importing items");
		TestImportItems iu = new TestImportItems(ldapServer);
		return iu.startImport();
	}
	
	private TestImportItems(LdapServer ldapServer) throws CoreException {
		if (ldapServer == null)
			throw new IllegalArgumentException();
		
		this.ldapServer = ldapServer;
		
		this.userNameField = ldapServer.getLoginField();
		if( ldapServer.getServerConfigType() == ServerConfigType.ImportLDAPUsers ) 
			this.searchFilter = ldapServer.getLDAPDirectoryUserSearchFilter();
		else if( ldapServer.getServerConfigType() == ServerConfigType.CreateDuringSync ) 
			this.searchFilter = ldapServer.getLDAPUserCredentialsSearchFilter();
		else 
			throw new CoreException("Nothing to import, so nothing to test");
				
		if (this.userNameField == null || "".equals(this.userNameField))
			throw new IllegalArgumentException("No login field specified for ldap configuration");
	}
		
	private String startImport()  throws Exception 
	{	
		clearCurrentTestSet();
		
		switch( this.ldapServer.getSynchronizationMethod() ) {
		case SynchronizeByGroup:
			for (LdapGroup group : LdapGroup.load(this.context, "[" + LdapGroup.MemberNames.LdapGroup_LdapServer + "=" + this.ldapServer.getMendixObject().getId().toLong() + "][" + LdapGroup.MemberNames.Synchronize + "=true()]" )) {
				
				if( !LdapModule.isInvalidDn(group.getGroupName()) ) {
					String searchFilter = "(&" + this.searchFilter + "(memberOf=" + group.getGroupName() + ") )";
									
					doImportForLocation(this.ldapServer.getRoot(), searchFilter);
				}
				else 
					log.info("Skipping the Group because the DN is invalid: " + group.getGroupName());
			}
			break;
		
		case SynchronizeByPath:
		default:
			for (LdapPath path : XPath.create(this.context, LdapPath.class).eq(LdapPath.MemberNames.LdapPath_LdapServer, this.ldapServer).all()) {
				doImportForLocation(path.getLocation(), this.searchFilter);
			}
			break;
		}
		
		return "Imported " + this.importCount + " test items. " + (this.errorCount > 0 ? "Failed test items: " + this.errorCount : "") + (this.hasImportErrors ? "Some errors occurred." : "");
	}

	public void doImportForLocation( String location, String filter ) throws Exception {
		log.info("Importing test items from path " + location);
		
		if (LdapModule.isInvalidDn(location)) {
			log.warn("Skipped, invalid DN: " + location);
			return;
		}
		
		LdapModule.getInstance().searchAD(this.context, this.ldapServer, location, filter, true, true, new TestItemMapper());
		
		log.info("Importing test items.. DONE");
	}
	

	
	private void clearCurrentTestSet() throws CoreException {
		String xpath = "//" + TestEntry.getType() + "//" + TestEntry.getType() + "[" +TestEntry.MemberNames.TestEntry_LdapServer + "=" + this.ldapServer.getMendixObject().getId().toLong()+"]";
		List<IMendixObject> result = Core.createXPathQuery(xpath).execute(this.context);
		Core.delete(this.context, result);
		log.info("Removed all test items");
	}
	
	private class TestItemMapper implements NameClassPairCallbackHandler {
		public TestItemMapper()	{
			//Empty constructor
		}

		@Override
		public void handleNameClassPair(NameClassPair arg0) {
			
			Attributes attrs = ((SearchResult) arg0).getAttributes();
			
			++TestImportItems.this.importCount;			
//			if (attrs.get(TestImportItems.this.userNameField) == null) {
//			    log.warn("No distinguished name found for object, skipping..");
//				TestImportItems.this.errorCount++;
//				return;
//			}
	
			String dn;
			TestEntry testObject = new TestEntry(TestImportItems.this.context);
			testObject.setTestEntry_LdapServer(TestImportItems.this.ldapServer);
			/** Determine Distinguished name */
			try
			{
				dn = (String) attrs.get(TestImportItems.this.userNameField).get();
			}
			catch (Exception e1)
			{
				log.warn("Failed to read distinguishedname. ", e1);
				TestImportItems.this.errorCount++;
				
				dn = "User: " + TestImportItems.this.importCount;
			}
			testObject.setDistinguishedName(dn);

			log.trace("Reading " + dn);
			
			try {
				List<IMendixObject> attributes = new ArrayList<IMendixObject>();
				String[] ignoreItems = {"dNSProperty","msExchMailboxSecurityDescriptor","auditingPolicy","dnsRecord","replUpToDateVector","dSASignature"};
				/**Process all attributes */
				for (NamingEnumeration<String> n = attrs.getIDs(); n.hasMoreElements();) {
					String attrName = n.nextElement();
					try {
					    TestAttribute attribute = new TestAttribute(TestImportItems.this.context);
					    attribute.setName(attrName);
						boolean isIgnored = false;
						if( attrName.endsWith("Sid") || attrName.endsWith("SID") || attrName.endsWith("GUID") || attrName.endsWith("Hash") || attrName.endsWith("Guid") || attrName.endsWith("Certificate") || attrName.endsWith("Certificates") || attrName.endsWith("Key") ) {
							attribute.setValue("-ignored-");
							isIgnored = true;
						}
							
						if( !isIgnored ) {
							for( String item : ignoreItems ) {
								if( item.equals(attrName) ) {
									attribute.setValue("-ignored-");
									isIgnored = true;
									break;
								}
							}
						}
						
						if( !isIgnored ) {
							if( attrs.get(attrName).size() == 1 )
								attribute.setValue(String.valueOf(attrs.get(attrName).get()) );
							else {
								String value = "";
								for( int i = 0; i < 3 && i < attrs.get(attrName).size(); i++ ) {
									value += "[ " + String.valueOf(attrs.get(attrName).get(i)) + " ]";
								} 
								if( attrs.get(attrName).size() > 3 )
									value += "[ ... " + (attrs.get(attrName).size()-3) + " more ]";
								attribute.setValue( value );
							}
						}
						attribute.setTestAttribute_TestEntry(testObject); 
						attributes.add(attribute.getMendixObject());
					}
					catch (Exception e) {
						log.error("Error reading attribute '"+ attrName + "' from " + dn, e);
					}
				}
				
				testObject.commit();
				Core.commit(TestImportItems.this.context, attributes);
				
				log.trace("Processing data");
			}
			catch (Throwable e) {
				TestImportItems.this.errorCount++;
				TestImportItems.this.hasImportErrors = true;
				log.error("Error reading " + dn, e);
			}
		}
	}
}

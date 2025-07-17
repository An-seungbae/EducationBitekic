package ldap;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.springframework.ldap.core.NameClassPairCallbackHandler;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.datastorage.XPathQuery;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.AuthenticationRuntimeException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IUser;

import communitycommons.StringUtils;
import ldap.proxies.LdapAttributeMapping;
import ldap.proxies.LdapCredentialsHistory;
import ldap.proxies.LdapGroup;
import ldap.proxies.LdapPath;
import ldap.proxies.LdapServer;
import ldap.proxies.LdapUser;
import ldap.proxies.LookupUserAttr;
import ldap.proxies.LookupUserEntity;
import ldap.proxies.MultipleUserMatchConfiguration;
import ldap.proxies.ServerConfigType;
import ldap.proxies.ValidationFrequency;
import system.proxies.User;
import system.proxies.UserRole;

public class ImportUserRecord {

	// MWE: Note, some fields are not private to make them visible in wrapped
	// classes. But a
	// reference to an instance of the class should
	// never be published to outside this class, to that is safe.
	private final IContext context = Core.createSystemContext();
	private final LdapServer ldapServer;
	private final String userEntity;
	private final String userNameField;
	private String enteredUserName;

	private static final ILogNode log = Core.getLogger(LdapModule.LDAP_LOG);

	/** Map, key: MxAttributeName, value: LdapAttributeName> */
	private final Map<String, String> attributeMappings = new HashMap<String, String>();

	/** Group id -> group roles */
	final Map<LdapGroup, List<UserRole>> userRolesByLdapGroup = new HashMap<LdapGroup, List<UserRole>>();

	/** Group name -> group id */
	private final Map<String, LdapGroup> ldapGroupByName = new HashMap<String, LdapGroup>();
	private Result r;
	private searchAction userSearchAction;
	private MultipleUserMatchConfiguration multipleMatchConfig;
	/**
	 * This indicates if we are only importing the group. When true, only groups are
	 * created When false, user and group records are being created during import
	 */
	private boolean importTheGroupOnly = false;

	public static final int BATCH_SIZE = 100;

	private enum searchAction {
		ImportAll, SearchSingleUser, SearchAndCreateSingleUser
	}

	public class Result {
		List<IMendixObject> credentialHistoryList;
		private long importCount = 0;
		private long deactivated = 0;
		private boolean hasImportErrors = false;
		private Exception importError = null;

		public Result() {
			this.credentialHistoryList = new ArrayList<IMendixObject>();
		}

		public List<IMendixObject> getCredentialHistory() {
			return this.credentialHistoryList;
		}

		public void addCredentialHistory(IMendixObject credentialHistory) {
			this.credentialHistoryList.add(credentialHistory);
		}

		public String getStatistics() {
			return "Imported " + this.importCount + " users, deactivated " + this.deactivated + " users. "
					+ (this.hasImportErrors
							? "Some errors occurred, therefore groups and users where not cleaned up correctly. Please fix the errors in the log and synchronize again."
							: "");
		}

		public boolean hasImportErrors() {
			return this.hasImportErrors;
		}

		public Exception getImportError() {
			return this.importError;
		}
	}

	public static Result runImportUsersByPath(LdapServer ldapServer, String username) throws Exception {
		log.info("Importing users");
		ImportUserRecord iu = new ImportUserRecord(ldapServer, username);
		return iu.importUsersByPath();
	}

	public static Result runImportUsersByGroup(LdapServer ldapServer, String username) throws Exception {
		log.info("Importing users by groups.");
		ImportUserRecord iu = new ImportUserRecord(ldapServer, username);
		return iu.importUsersByGroup();
	}

	public static Result runImportGroups(LdapServer ldapServer) throws Exception {
		log.info("Importing groups");
		ImportUserRecord iu = new ImportUserRecord(ldapServer, null);
		iu.importTheGroupOnly = true;
		return iu.importUsersByPath();
	}

	public static Result runImportGroupsByGroup(LdapServer ldapServer) throws Exception {
		log.info("Importing groups");
		ImportUserRecord iu = new ImportUserRecord(ldapServer, null);
		iu.importTheGroupOnly = true;
		return iu.importUsersByGroup();
	}

	private ImportUserRecord(LdapServer ldapServer, String username) throws CoreException {
		if (ldapServer == null)
			throw new IllegalArgumentException("No Ldap server provided");

		this.ldapServer = ldapServer;
		if (username == null) {
			this.userSearchAction = searchAction.ImportAll;
			this.userEntity = validateUserEntity(ldapServer);
		} else {
			this.multipleMatchConfig = ldapServer.getMultipleMatchConfiguration();
			ServerConfigType type = ldapServer.getServerConfigType();
			switch (type) {
			case CreateDuringSync:
				this.userSearchAction = searchAction.SearchAndCreateSingleUser;
				this.userEntity = validateUserEntity(ldapServer);
				break;
			case OnlyAuthenticateLDAPusers:
				this.userSearchAction = searchAction.SearchSingleUser;
				this.userEntity = null;
				break;
			case ImportLDAPUsers:
				this.userSearchAction = searchAction.SearchAndCreateSingleUser;
				this.userEntity = validateUserEntity(ldapServer);
				break;
			default:
				this.userEntity = validateUserEntity(ldapServer);
			}
		}
		this.enteredUserName = username;

		this.userNameField = ldapServer.getLoginField();

		// Gather all information for the attribute mapping
		for (LdapAttributeMapping mapping : LdapAttributeMapping.load(this.context,
				"[" + LdapAttributeMapping.MemberNames.LdapAttributeMapping_LdapServer + "="
						+ ldapServer.getMendixObject().getId().toLong() + "]")) {
			LookupUserAttr attr = mapping.getLdapAttributeMapping_UserAttr();
			if (attr == null)
				throw new IllegalArgumentException("User mapping specified an unknown local attribute");
			this.attributeMappings.put(attr.getName(), mapping.getLdapAttr());
		}

		// Cache the user groups
		for (LdapGroup group : LdapGroup.load(this.context, "[" + LdapGroup.MemberNames.LdapGroup_LdapServer + "="
				+ ldapServer.getMendixObject().getId().toLong() + "]"))
			addToGroupCache(group);

		if (this.userNameField == null || "".equals(this.userNameField))
			throw new IllegalArgumentException("No login field specified for ldap configuration");

		log.trace("Import Config [action:" + this.userSearchAction + "|userSearch:" + this.userSearchAction
				+ "|userEntity:" + this.userEntity + "|userNameField:" + this.userNameField + "]");
	}

	public String validateUserEntity(LdapServer ldapServer) throws CoreException {
		if (ldapServer.getLdapServer_UserEntity() == null)
			throw new IllegalArgumentException("No user entity was specified");
		LookupUserEntity userEntity = ldapServer.getLdapServer_UserEntity();
		if (userEntity == null)
			throw new IllegalArgumentException("No user entity was found");

		String userEntityName = userEntity.getName();

		if (userEntityName == null || "".equals(userEntityName))
			throw new IllegalArgumentException("Ldap Module: User entity was not set!");

		return userEntityName;
	}

	/**
	 * Synchronizes all users of a specific ldapServer. New users will be added,
	 * existing users will not be modified (but userroles will be), non-existing
	 * imported by this ldapServer will be removed.
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private Result importUsersByPath() throws Exception {
		// Mark dirty
		this.r = new Result();

		if (ImportUserRecord.this.userSearchAction == searchAction.ImportAll)
			markAllUsersDirty();
		else {
			LdapUser user = getOrCreateLdapUser(this.enteredUserName);
			user.set_isDirty(true);
			user.commit();
		}

		for (LdapPath path : LdapPath.load(this.context, "[" + LdapPath.MemberNames.LdapPath_LdapServer + "="
				+ this.ldapServer.getMendixObject().getId().toLong() + "]")) {

			String location = path.getLocation();
			log.debug("Importing users from path " + location);

			if (LdapModule.isInvalidDn(location)) {
				log.warn("Skipped, invalid DN: " + location);
				continue;
			}

			String searchFilter = determineSearchFilter();
			if (this.importTheGroupOnly)
				LdapModule.getInstance().searchAD(this.context, this.ldapServer, location, searchFilter, true, true,
						new GroupMapper());
			else
				LdapModule.getInstance().searchAD(this.context, this.ldapServer, location, searchFilter, true, true,
						new UserMapper(this.ldapServer, location));

			log.debug("Importing users has been completed.");
		}

		if (ImportUserRecord.this.userSearchAction == searchAction.ImportAll && !this.r.hasImportErrors)
			this.r.deactivated = deactivateDirtyUsers();

		return this.r;
	}

	private Result importUsersByGroup() throws Exception {
		// Mark dirty
		this.r = new Result();

		if (this.importTheGroupOnly) {
			String searchFilter = determineSearchFilter();
			LdapModule.getInstance().searchAD(this.context, this.ldapServer, this.ldapServer.getRoot(), searchFilter,
					true, true, new GroupMapper());
			log.debug("Importing of all groups has been completed.");
		} else {
			if (ImportUserRecord.this.userSearchAction == searchAction.ImportAll)
				markAllUsersDirty();
			else {
				LdapUser luser = getOrCreateLdapUser(this.enteredUserName);
				luser.set_isDirty(true);
				luser.commit();
			}

			for (LdapGroup group : LdapGroup.load(this.context,
					"[" + LdapGroup.MemberNames.LdapGroup_LdapServer + "="
							+ this.ldapServer.getMendixObject().getId().toLong() + "]["
							+ LdapGroup.MemberNames.Synchronize + "=true()]")) {
				String groupName = group.getGroupName();
				log.debug("Importing users from group " + groupName);

				if (LdapModule.isInvalidDn(groupName)) {
					log.warn("Skipped, invalid DN: " + groupName);
					this.r.hasImportErrors = true;
					return this.r;
				}

				String searchFilter = determineSearchFilter();
//				if (ImportUserRecord.this.userSearchAction == searchAction.ImportAll)
				searchFilter = "(&" + searchFilter + "(memberOf=" + groupName + ") )";

				LdapModule.getInstance().searchAD(this.context, this.ldapServer, this.ldapServer.getRoot(),
						searchFilter, true, true, new UserMapper(this.ldapServer, groupName));
			}

			if (ImportUserRecord.this.userSearchAction == searchAction.ImportAll && !this.r.hasImportErrors)
				this.r.deactivated = deactivateDirtyUsers();
		}

		return this.r;
	}

	private class UserMapper implements NameClassPairCallbackHandler {

		private ValidationFrequency validationFreq = null;
		private String location;

		public UserMapper(LdapServer ldapServer, String location) {
			this.location = location;
			this.validationFreq = ldapServer.getValidationFrequency();
		}

		@Override
		public void handleNameClassPair(NameClassPair arg0) {

			Attributes attrs = ((SearchResult) arg0).getAttributes();

			++ImportUserRecord.this.r.importCount;
			log.trace("Importing user..");

			if (attrs.get(ImportUserRecord.this.userNameField) == null) {
				log.warn("No distinguished name(username) found for object, skipping..");
				return;
			}

			String dn;
			String username = null;
			Set<String> memberOf = new HashSet<String>();

			/** Determine Distinguished name */
			try {
				dn = (String) attrs.get(ImportUserRecord.this.userNameField).get();
			} catch (NamingException e1) {
				log.warn("Failed to read distinguishedname. ", e1);
				return;
			}

			if (ImportUserRecord.this.userSearchAction != searchAction.SearchSingleUser) {
				log.trace("Reading " + dn);

				try {

					/** Process all attributes */
					for (NamingEnumeration<String> n = attrs.getIDs(); n.hasMoreElements();) {

						String attrName = n.nextElement();
						if (attrs.get(attrName).size() == 0)
							continue;

						/** preprocess memberOf attribute */
						else if (LdapModule.MEMBER_OF_ATTR.equalsIgnoreCase(attrName)) {
							for (Enumeration<?> e = attrs.get(attrName).getAll(); e.hasMoreElements();)
								memberOf.add((String) e.nextElement());
						}
						/** Preprocass username Field */
						else if (ImportUserRecord.this.userNameField.equalsIgnoreCase(attrName))
							username = (String) attrs.get(attrName).get();
					}

					log.trace("Processing data");

					if (ImportUserRecord.this.enteredUserName != null)
						username = ImportUserRecord.this.enteredUserName;

					processUserImport(dn, username, memberOf, attrs);
				} catch (Exception e) {
					ImportUserRecord.this.r.hasImportErrors = true;
					ImportUserRecord.this.r.importError = e;
					// cl.error(e, "Error reading '%s'", dn);
				}
				// Don't know why a throwable was caught here, just in case keeping the
				// throwable as
				// well
				catch (Throwable t) {
					ImportUserRecord.this.r.hasImportErrors = true;
					log.error("Error reading " + dn, t);
				}
			}

			if (!ImportUserRecord.this.r.hasImportErrors()) {
				// If a user is potentially created now setup the result of this action
				if (ImportUserRecord.this.userSearchAction != searchAction.ImportAll) {
					if (ImportUserRecord.this.multipleMatchConfig == MultipleUserMatchConfiguration.Throw_an_error
							&& ImportUserRecord.this.r.getCredentialHistory().size() > 0)
						throw new AuthenticationRuntimeException(
								"LdapLogin FAILED: Found a second match for the entered username "
										+ ImportUserRecord.this.enteredUserName + " in ldap path: " + this.location);

					IMendixObject credentialHistory = Core.instantiate(ImportUserRecord.this.context,
							LdapCredentialsHistory.getType());
					credentialHistory.setValue(ImportUserRecord.this.context,
							LdapCredentialsHistory.MemberNames.ADUsername.toString(), dn);
					credentialHistory.setValue(ImportUserRecord.this.context,
							LdapCredentialsHistory.MemberNames.EnteredUsername.toString(),
							ImportUserRecord.this.enteredUserName);
					credentialHistory.setValue(ImportUserRecord.this.context,
							LdapCredentialsHistory.MemberNames.LdapCredentialsHistory_LdapServer.toString(),
							ImportUserRecord.this.ldapServer.getMendixObject().getId());
					credentialHistory.setValue(ImportUserRecord.this.context,
							LdapCredentialsHistory.MemberNames.LdapPath.toString(), this.location);
					ImportUserRecord.this.r.addCredentialHistory(credentialHistory);

					try {
						/*
						 * Store the ldap credentials history and attach it to the user so we can use it
						 * for future reference
						 */
						if (ValidationFrequency.OnEveryLoginButStoreDuringSession == this.validationFreq) {
							IUser u = Core.getUser(ImportUserRecord.this.context,
									ImportUserRecord.this.enteredUserName);
							credentialHistory.setValue(ImportUserRecord.this.context,
									LdapCredentialsHistory.MemberNames.LdapCredentialsHistory_User.toString(),
									u.getMendixObject().getId());
							Core.commit(ImportUserRecord.this.context, credentialHistory);
						} else if (ValidationFrequency.OnEveryLogin == this.validationFreq)
							Core.commit(ImportUserRecord.this.context, credentialHistory);
					} catch (CoreException e) {
						log.warn("Unable to store the credentials for distinguishedname: " + dn + " user: "
								+ ImportUserRecord.this.enteredUserName, e);
						return;
					}
				}
			}
		}

	}

	private class GroupMapper implements NameClassPairCallbackHandler {

		public GroupMapper() {
		}

		@Override
		public void handleNameClassPair(NameClassPair arg0) {
			Attributes attrs = ((SearchResult) arg0).getAttributes();

			++ImportUserRecord.this.r.importCount;
			log.debug("Importing groups..");

			String dn;
			/** Determine Distinguished name */
			try {
				dn = (String) attrs.get(ImportUserRecord.this.userNameField).get();
			} catch (NamingException e1) {
				log.warn("Failed to read distinguishedname. ", e1);
				return;
			}

			log.trace("Reading " + dn);
			try {

				/** Process all attributes */
				for (NamingEnumeration<String> n = attrs.getIDs(); n.hasMoreElements();) {
					String attrName = n.nextElement();
					if (attrs.get(attrName).size() == 0)
						continue;

					/** preprocess memberOf attribute */
					else if (LdapModule.MEMBER_OF_ATTR.equalsIgnoreCase(attrName)) {
						for (Enumeration<?> e = attrs.get(attrName).getAll(); e.hasMoreElements();)
							getGroupByName((String) e.nextElement());
					}
				}

			} catch (Exception e) {
				ImportUserRecord.this.r.hasImportErrors = true;
				ImportUserRecord.this.r.importError = e;
				// cl.error(e, "Error reading '%s'", dn);
			}
			// Don't know why a throwable was caught here, just in case keeping the
			// throwable as
			// well
			catch (Throwable t) {
				ImportUserRecord.this.r.hasImportErrors = true;
				log.error("Error reading " + dn, t);
			}
		}
	}

	void processUserImport(String dn, String username, Set<String> groups, Attributes attrs) {
		try {
			if (this.context.isInTransaction())
				throw new IllegalStateException();

			if (username == null) {
				log.warn("Skipping " + dn + "; no username found");
				return;
			}
			// Validate username length against the entity's maximum allowed length. Skip if
			// exceeded.
			int maxLength = Core.getMetaObject(this.userEntity).getMetaPrimitive(User.MemberNames.Name.toString())
					.getLength();
			if (username != null && username.length() > maxLength) {
				log.warn("Skipping " + dn + " - Username length (" + username.length() + ") exceeds max length ("
						+ maxLength + ") for attribute '" + User.MemberNames.Name + "' in entity '" + this.userEntity);
				return;
			} else {
				if (LdapModule.usernameLowerCase)
					username = username.toLowerCase();

				if (this.userSearchAction != searchAction.ImportAll && !this.enteredUserName.equals(username)) {
					log.warn("Invalid configuration! The entered username: " + this.enteredUserName
							+ " does not match the found username: " + dn);
					throw new CoreException("Invalid configuration! The entered username: " + this.enteredUserName
							+ " does not match the found username: " + dn);
				}
			}

			// do this in a context, to avoid empty records!
			this.context.startTransaction();
			try {
				/** Find the ldap user object */
				LdapUser luser = getOrCreateLdapUser(dn);

				/** Find or create the real user object */

				User realUser;
				// generic typing because userEntity type is run-time configurable
				List<IMendixObject> userList = Core
						.createXPathQuery("//" + this.userEntity + "[" + User.MemberNames.Name + "=$value]")
						.setVariable("value", username).execute(context);

				if (userList.size() > 0)
					realUser = User.initialize(this.context, userList.get(0));
				else {
					realUser = User.initialize(this.context, Core.instantiate(this.context, this.userEntity));
					realUser.setName(username);
				}

				/** Find the mapped groups */
				List<LdapGroup> groupsTheUserHadAfterLastSync = luser.getLdapUser_LdapGroup();

				List<LdapGroup> allGroupsTheUserIsMemberOf = new ArrayList<LdapGroup>();
				for (String memberOf : groups)
					allGroupsTheUserIsMemberOf.add(getGroupByName(memberOf));

				/** Find the mapped roles */
				List<UserRole> roles = realUser.getUserRoles();

				if (luser.get_isDirty()) {
					/*
					 * The user is dirty (first time sync). Do a full reset of all the roles that we
					 * can assign through Ldap. And rebuild these roles at a later step.
					 */
					for (LdapGroup removedGroup : groupsTheUserHadAfterLastSync)
						roles.removeAll(this.userRolesByLdapGroup.get(removedGroup));
				} else
					log.debug("Skipping user role reset, since this has been previously done. User: "
							+ this.enteredUserName);

				// Add the roles from the group to the Roles List
				for (LdapGroup newGroup : allGroupsTheUserIsMemberOf)
					roles.addAll(this.userRolesByLdapGroup.get(newGroup));

				realUser.setUserRoles(roles);

				/** Process custom attributes **/

				// finally, synchronize all attributes from the mapping.
				// all items will be overwritten: the AD is leading.
				for (Entry<String, String> e : this.attributeMappings.entrySet()) {
					Attribute ldapattr = attrs.get(e.getValue());
					if (ldapattr != null)
						realUser.getMendixObject().setValue(this.context, e.getKey(), ldapattr.get());
				}

				/** Write the results */
				if (realUser.getMendixObject().isNew())
					realUser.setPassword(StringUtils.randomStrongPassword(32, 32, 5, 5, 5)); // Should

				// Found and has roles, so, true.
				if (roles.size() > 0)
					realUser.setActive(true);

				realUser.commit();

				luser.set_isDirty(false);
				luser.setLdapUser_LdapGroup(allGroupsTheUserIsMemberOf);
				luser.setLdapUser_User(realUser);
				luser.commit();
				this.context.endTransaction();
			} catch (Throwable e) {
				if (this.context.isInTransaction())
					this.context.rollbackTransaction();
				throw e;
			}
		}

		catch (Exception e) {
			this.r.hasImportErrors = true;
			this.r.importError = e;
			log.error("Failed to import user '" + dn + "', skipping..", e);
		} catch (Throwable t) {
			this.r.hasImportErrors = true;
			log.error("Failed to import user '" + dn + "', skipping..", t);
		}

	}

	private LdapUser getOrCreateLdapUser(String dn) throws CoreException {
		LdapUser luser;
		List<LdapUser> ldapUserList = new ArrayList<>();

		List<IMendixObject> mxObjs = Core
				.createXPathQuery("//" + LdapUser.entityName + "[" + LdapUser.MemberNames.LdapUser_LdapServer
						+ "=$value1]" + "[" + LdapUser.MemberNames.DistinguishedName + "=$value2]")
				.setVariable("value1", this.ldapServer.getMendixObject().getId()).setVariable("value2", dn)
				.execute(context);

		for (IMendixObject obj : mxObjs) {
			ldapUserList.add(LdapUser.initialize(this.context, obj));
		}

		if (ldapUserList.size() > 0)
			luser = ldapUserList.get(0);
		else {
			luser = new LdapUser(this.context);
			luser.setDistinguishedName(dn);
			luser.setLdapUser_LdapServer(this.ldapServer);
			luser.commit();
		}
		return luser;
	}

	private void addToGroupCache(LdapGroup group) throws CoreException {
		if (!this.userRolesByLdapGroup.containsKey(group)) {
			this.userRolesByLdapGroup.put(group, group.getLdap_Role_Mapping());
			this.ldapGroupByName.put(getUniqueName(group.getGroupName()), group);
		}
	}

	private LdapGroup getGroupByName(String groupName) throws CoreException {
		groupName = getUniqueName(groupName);

		if (!this.ldapGroupByName.containsKey(groupName)) {
			List<LdapGroup> groupList = new ArrayList<>();

			List<IMendixObject> mxObjs = Core
					.createXPathQuery("//" + LdapGroup.entityName + "[" + LdapGroup.MemberNames.LdapGroup_LdapServer
							+ "=$value1]" + "[" + LdapGroup.MemberNames.GroupName + "=$value2]")
					.setVariable("value1", this.ldapServer.getMendixObject().getId()).setVariable("value2", groupName)
					.execute(context);

			for (IMendixObject obj : mxObjs) {
				groupList.add(LdapGroup.initialize(this.context, obj));
			}

			if (groupList.size() > 0)
				addToGroupCache(groupList.get(0));
			else {
				LdapGroup group = new LdapGroup(this.context);
				group.setGroupName(groupName);
				group.setLdapGroup_LdapServer(this.ldapServer);
				group.commit();
				;
				addToGroupCache(group);
			}
		}

		return this.ldapGroupByName.get(groupName);
	}

	/**
	 * Based on if we want to search for a single user or if we want to import the
	 * whole AD this function will return the correct search filter
	 * 
	 * @return
	 * @throws CoreException
	 */
	private String determineSearchFilter() throws CoreException {
		// Regular expression pattern for the invalid characters
		String INVALID_CHARACTERS_PATTERN = "[\\[\\]\":;|=+*?<>/\\,\\\\]";
		switch (this.userSearchAction) {
		case SearchAndCreateSingleUser:
		case SearchSingleUser:
			String searchFilter = this.ldapServer.getLDAPUserCredentialsSearchFilter();
			if (!searchFilter.contains("\"[%Username%]\""))
				throw new CoreException("The credentials search filter should contain the token: \"[%Username%]\" ");
			if (this.enteredUserName.matches(".*" + INVALID_CHARACTERS_PATTERN + ".*")) {
				throw new java.lang.IllegalStateException("Unexpected value: " + searchFilter);
			} else {
				if (this.enteredUserName.endsWith(".")) {
					throw new java.lang.IllegalStateException("Unexpected value: " + searchFilter);
				}
			}
			searchFilter = searchFilter.replace("\"[%Username%]\"", this.enteredUserName);
			return searchFilter;
		case ImportAll:
			return this.ldapServer.getLDAPDirectoryUserSearchFilter();
		}

		throw new CoreException("There is no credentials filter provided.");
	}

	private void markAllUsersDirty() throws CoreException {

		String xpathQuery = "//" + LdapUser.entityName + "[" + LdapUser.MemberNames.LdapUser_LdapServer + "="
				+ this.ldapServer.getMendixObject().getId().toLong() + "]";
		HashMap<String, String> sortMap = new HashMap<String, String>();
		sortMap.put("id", "ASC");

		List<IMendixObject> data = null;
		List<IMendixObject> commitData = new ArrayList<>();
		long currentCount = 0;
		int offset = 0;
		do {
			// retrieve new set of data
			XPathQuery query = (XPathQuery) Core.createXPathQuery(xpathQuery).setAmount(ImportUserRecord.BATCH_SIZE)
					.setOffset(offset);
			// Add sorting
			for (Map.Entry<String, String> sort : sortMap.entrySet()) {
				query.addSort(sort.getKey(), "asc".equalsIgnoreCase(sort.getValue()));
			}
			// Execute the query to get data
			data = query.execute(this.context);
			offset += ImportUserRecord.BATCH_SIZE;
			currentCount = data.size();

			for (IMendixObject item : data) {
				item.setValue(this.context, LdapUser.MemberNames._isDirty.toString(), true);
				commitData.add(item);
			}
			Core.commit(this.context, commitData);
			commitData.clear();
			data.clear();

		} while (currentCount > 0);
	}

	private long deactivateDirtyUsers() throws CoreException {
		log.info("Removing no longer available users.. ");

		/* All ldapUsers with some active user which are marked dirty */
		String xpathQuery = "//" + LdapUser.entityName + "[" + LdapUser.MemberNames.LdapUser_LdapServer + "="
				+ this.ldapServer.getMendixObject().getId().toLong() + "][" + LdapUser.MemberNames._isDirty
				+ "=true()]";
		long totalCount = Core.createXPathQuery("count(" + xpathQuery + ")").executeAggregateLong(this.context);
		long i = 0;

		HashMap<String, String> sortMap = new HashMap<String, String>();
		sortMap.put("id", "ASC");

//		// deactivation is done in batches, without moving the offset, since deactivation means the record does not comply with the constraint anymore.

		List<IMendixObject> data = null;
		List<IMendixObject> commitData = new ArrayList<>();
		List<IMendixObject> deleteData = new ArrayList<>();
		long currentCount = 0;
		int offset = 0;
		do {
			// retrieve new set of data
			XPathQuery query = (XPathQuery) Core.createXPathQuery(xpathQuery).setAmount(ImportUserRecord.BATCH_SIZE)
					.setOffset(offset);
			// Add sorting
			for (Map.Entry<String, String> sort : sortMap.entrySet()) {
				query.addSort(sort.getKey(), "asc".equalsIgnoreCase(sort.getValue()));
			}
			// Execute the query to get data
			data = query.execute(this.context);
			offset += ImportUserRecord.BATCH_SIZE;
			currentCount = data.size();

			// process users
			for (IMendixObject userRecord : data) {
				LdapUser item = LdapUser.initialize(this.context, userRecord);
				i += 1;

				log.debug("Removing users (" + i + "/" + totalCount + ")");

				try {
					User u = item.getLdapUser_User();

					log.warn("User '" + item.getDistinguishedName()
							+ "' is no longer active in ldap and will be disabled");

					// deactivate user and remove all ldap-group-mapped roles for that user
					if (u != null && u.getActive()) {
						List<UserRole> userRoles = u.getUserRoles();
						List<LdapGroup> groups = item.getLdapUser_LdapGroup();

						for (LdapGroup group : groups) {
							List<UserRole> groupRoles = ImportUserRecord.this.userRolesByLdapGroup.get(group);
							if (groupRoles != null) {
								userRoles.removeAll(groupRoles);
							}
						}

						u.setUserRoles(userRoles);
						u.setActive(false);
						commitData.add(u.getMendixObject());
					}
				} catch (CoreException e) {
					log.error("Failed to inactivate user: " + item.getDistinguishedName(), e);
				}

				deleteData.add(item.getMendixObject());
			}

			// commit changes
			Core.commit(this.context, commitData);
			commitData.clear();
			// delete the ldapusers, they are no longer necessary and this makes sure they
			// do not show up in the next retrieve
			Core.delete(this.context, deleteData);
			deleteData.clear();
		} while (currentCount > 0);

		return totalCount;
	}

	private static final String getUniqueName(String name) {
		if (name != null)
			return name.toLowerCase();

		return null;
	}
}

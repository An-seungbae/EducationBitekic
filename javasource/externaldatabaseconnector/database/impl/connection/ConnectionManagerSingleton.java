package externaldatabaseconnector.database.impl.connection;

import com.mendix.logging.ILogNode;

import externaldatabaseconnector.database.interfaces.ConnectionManager;

public final class ConnectionManagerSingleton {
	private static ConnectionManager connectionManager;

	public static synchronized ConnectionManager getInstance(ILogNode logNode) {
		if (connectionManager == null)
			connectionManager = new JdbcConnectionManager(logNode);

		return connectionManager;
	}
}

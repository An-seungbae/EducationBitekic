package externaldatabaseconnector.database.impl.connection;

import com.mendix.logging.ILogNode;

import externaldatabaseconnector.database.interfaces.ConnectionManager;
import externaldatabaseconnector.pojo.ConnectionDetails;

import java.sql.Connection;
import java.sql.SQLException;

public class MxConnection implements AutoCloseable {
  private final ConnectionManager connectionManager;
  private final ConnectionDetails connectionDetails;
  private final ILogNode logNode;

  private Connection connection;

  public MxConnection(ILogNode logNode, ConnectionDetails connectionDetails, ConnectionManager connectionManager) {
    this.logNode = logNode;
    this.connectionDetails = connectionDetails;
    this.connectionManager = connectionManager;
  }

  public MxConnection(ILogNode logNode, ConnectionDetails connectionDetails) {
    this(logNode, connectionDetails, ConnectionManagerSingleton.getInstance(logNode));
  }

  public Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
      connection = connectionManager.getConnection(this.connectionDetails);
    }
    return connection;
  }

  @Override
  public void close() throws SQLException {
    if (connection != null) {
      connection.close();
      connection = null;
    }
  }
}

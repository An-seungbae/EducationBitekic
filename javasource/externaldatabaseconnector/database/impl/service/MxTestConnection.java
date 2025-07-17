package externaldatabaseconnector.database.impl.service;

import com.mendix.logging.ILogNode;

import externaldatabaseconnector.database.impl.connection.ConnectionManagerSingleton;
import externaldatabaseconnector.database.impl.connection.MxConnection;
import externaldatabaseconnector.database.interfaces.ConnectionManager;
import externaldatabaseconnector.pojo.ConnectionDetails;

import java.sql.Connection;
import java.sql.SQLException;

public class MxTestConnection {
  private final MxConnection mxConnection;

  public MxTestConnection(ILogNode logNode, ConnectionDetails connectionDetails) {
    mxConnection = new MxConnection(logNode, connectionDetails);
  }

  public boolean execute() throws SQLException {
    try (Connection connection = mxConnection.getConnection()) {
      return connection != null;
    }
  }
}

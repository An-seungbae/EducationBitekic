package externaldatabaseconnector.database.impl.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class MxBaseQuery {
  private final Connection connection;
  private boolean featureNotSupportedExceptionOccurred = false;

  public MxBaseQuery(Connection aConnection) {
    this.connection = aConnection;
  }

  protected Connection getConnection() {
    return this.connection;
  }

  public boolean isFeatureNotSupportedExceptionOccurred(){
    return featureNotSupportedExceptionOccurred;
  }

  protected void doSetAutoCommit(boolean aAutoCommit) throws SQLException {
    try {
      // Set auto-commit to false; to start a transaction manually
      connection.setAutoCommit(aAutoCommit);
    } catch (SQLFeatureNotSupportedException e) {
      // Do nothing.
      // if the setAutoCommit(..) is not supported do not let the application fail.
      featureNotSupportedExceptionOccurred = true;
    }
  }

  protected void doRollBack() throws SQLException {
    try {
      // Roll back the transaction after successful execution to verify the query without committing changes
      connection.rollback();
    } catch (SQLFeatureNotSupportedException e) {
      // Do nothing.
      // if rollback() is not supported do not let the application fail.
      featureNotSupportedExceptionOccurred = true;
    }
  }
}

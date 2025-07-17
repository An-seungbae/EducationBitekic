package externaldatabaseconnector.database.interfaces;

import externaldatabaseconnector.pojo.ConnectionDetails;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionManager {
	Connection getConnection(ConnectionDetails connectionDetailsObject) throws SQLException;
}

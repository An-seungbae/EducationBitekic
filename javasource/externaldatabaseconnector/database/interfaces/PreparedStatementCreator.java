package externaldatabaseconnector.database.interfaces;

import externaldatabaseconnector.database.exceptions.MxDataTypeNotSupported;
import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public interface PreparedStatementCreator {
	PreparedStatement create(String query, Connection connection) throws SQLException;
	PreparedStatement create(String sql, List<QueryParameter> queryParametersList, Connection connection)
			throws SQLException, IllegalArgumentException, MxDataTypeNotSupported;
}

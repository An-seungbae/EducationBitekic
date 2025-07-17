package externaldatabaseconnector.database.interfaces;

import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.database.impl.callablestatement.StatementWrapper;
import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface CallableStatementCreator {
    StatementWrapper create(String sql, List<QueryParameter> queryParameters, Connection connection)
        throws SQLException, MxException;
}
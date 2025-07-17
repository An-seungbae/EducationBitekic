package externaldatabaseconnector.database.impl.statement;

import externaldatabaseconnector.database.interfaces.CallableStatementCreator;
import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.database.impl.callablestatement.SqlParameter;
import externaldatabaseconnector.database.impl.callablestatement.StatementWrapper;
import externaldatabaseconnector.database.impl.parameterFactory.SqlParameterFactory;
import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CallableStatementCreatorImpl implements CallableStatementCreator {

  @Override
  public StatementWrapper create(String sql, List<QueryParameter> queryParameterList, Connection connection) throws SQLException,
      MxException {
    final CallableStatement callableStatement = connection.prepareCall(sql);

    //Register stored procedure parameters. if there are any
    int index = 1;
    for (QueryParameter queryParameter : queryParameterList) {
      SqlParameter sqlParameter = SqlParameterFactory.getSqlParameter(queryParameter, index);
      sqlParameter.prepareCall(callableStatement);
      index++;
    }

    return new StatementWrapper(callableStatement);
  }
}
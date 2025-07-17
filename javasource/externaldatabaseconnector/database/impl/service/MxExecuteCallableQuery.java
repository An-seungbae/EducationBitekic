package externaldatabaseconnector.database.impl.service;

import externaldatabaseconnector.database.impl.callablestatement.StatementWrapper;
import externaldatabaseconnector.database.impl.statement.CallableStatementCreatorImpl;
import externaldatabaseconnector.database.interfaces.CallableStatementCreator;
import externaldatabaseconnector.database.utils.MxCallableParameterUtils;
import externaldatabaseconnector.database.utils.MxResultSetUtils;
import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.livepreview.responses.pojo.CallableParameterResult;
import externaldatabaseconnector.livepreview.responses.pojo.CallableResult;
import externaldatabaseconnector.livepreview.responses.pojo.TableResult;
import externaldatabaseconnector.pojo.QueryDetails;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class MxExecuteCallableQuery extends MxBaseQuery {
  private final QueryDetails queryDetails;
  private final CallableStatementCreator callableStatementCreator;


  public MxExecuteCallableQuery(Connection connection,
                                QueryDetails queryDetails,
                                CallableStatementCreator callableStatementCreator) {
    super(connection);
    this.queryDetails = queryDetails;
    this.callableStatementCreator = callableStatementCreator;
  }

  public MxExecuteCallableQuery(Connection connection, QueryDetails queryDetails) {
    this(connection, queryDetails, new CallableStatementCreatorImpl());
  }

  public CallableResult execute() throws SQLException, MxException {
    try (StatementWrapper callableStatement = callableStatementCreator.create(
        queryDetails.getQuery(),
        queryDetails.getQueryParameters(),
        getConnection())) {

      doSetAutoCommit(false);

      try (CallableStatement callableStatementResult = callableStatement.executeCallable()) {
        int rowsAffected = callableStatementResult.getUpdateCount();
        TableResult tableResult = MxResultSetUtils.buildTableResult(callableStatementResult.getResultSet());
        CallableParameterResult callableParameterResult =
            MxCallableParameterUtils.readCallableParameterValues(queryDetails.getQueryParameters(), callableStatementResult);

        doRollBack();

        return new CallableResult(rowsAffected, tableResult, callableParameterResult);
      }
    }
  }
}

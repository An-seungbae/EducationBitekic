package externaldatabaseconnector.database.impl.service;


import externaldatabaseconnector.database.impl.statement.PreparedStatementCreatorImpl;
import externaldatabaseconnector.database.interfaces.PreparedStatementCreator;
import externaldatabaseconnector.database.utils.MxResultSetUtils;
import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.livepreview.responses.pojo.TableResult;
import externaldatabaseconnector.pojo.QueryDetails;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MxExecuteSelectQuery extends MxBaseQuery {
  private final QueryDetails queryDetails;
  private final PreparedStatementCreator preparedStatementCreator;

  public MxExecuteSelectQuery(Connection connection,
                              QueryDetails queryDetails,
                              PreparedStatementCreator preparedStatementCreator) {
    super(connection);
    this.queryDetails = queryDetails;
    this.preparedStatementCreator = preparedStatementCreator;
  }

  public MxExecuteSelectQuery(Connection connection, QueryDetails queryDetails) {
    this(connection, queryDetails, new PreparedStatementCreatorImpl());
  }

  public TableResult execute() throws MxException, SQLException {
    try (PreparedStatement preparedStatement = preparedStatementCreator.
        create(
            queryDetails.getQuery(),
            queryDetails.getQueryParameters(),
            getConnection())) {

      doSetAutoCommit(false);

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        TableResult tableResult = MxResultSetUtils.buildTableResult(resultSet);

        doRollBack();

        return tableResult;
      }
    }
  }
}
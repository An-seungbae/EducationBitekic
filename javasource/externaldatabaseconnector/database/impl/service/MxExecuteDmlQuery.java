package externaldatabaseconnector.database.impl.service;

import externaldatabaseconnector.database.exceptions.MxDataTypeNotSupported;
import externaldatabaseconnector.database.impl.statement.PreparedStatementCreatorImpl;
import externaldatabaseconnector.database.interfaces.PreparedStatementCreator;
import externaldatabaseconnector.pojo.QueryDetails;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MxExecuteDmlQuery extends MxBaseQuery {
  private final QueryDetails queryDetails;
  private final PreparedStatementCreator preparedStatementCreator;

  public MxExecuteDmlQuery(Connection connection,
                           QueryDetails queryDetails,
                           PreparedStatementCreator preparedStatementCreator) {
    super(connection);
    this.queryDetails = queryDetails;
    this.preparedStatementCreator = preparedStatementCreator;
  }

  public MxExecuteDmlQuery(Connection connection, QueryDetails queryDetails) {
    this(connection, queryDetails, new PreparedStatementCreatorImpl());
  }

  public int execute() throws SQLException, MxDataTypeNotSupported {
    try (PreparedStatement preparedStatement = preparedStatementCreator.create(queryDetails.getQuery(), queryDetails.getQueryParameters(),
        getConnection())) {

      doSetAutoCommit(false);

      int numberOfAffectedRows = preparedStatement.executeUpdate();

      doRollBack();

      return numberOfAffectedRows;
    }
  }
}

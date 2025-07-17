package externaldatabaseconnector.livepreview.service;

import com.mendix.logging.ILogNode;

import externaldatabaseconnector.database.impl.service.MxExecuteDmlQuery;
import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.livepreview.responses.ExecuteStatementResponse;
import externaldatabaseconnector.pojo.ConnectionDetails;
import externaldatabaseconnector.pojo.QueryDetails;

import java.sql.SQLException;

public class MxDbExecuteDml extends MxAbstractAction {
  private final QueryDetails queryDetails;

  public MxDbExecuteDml(ILogNode logNode, ConnectionDetails connectionDetails, QueryDetails queryDetails) {
    super(new ExecuteStatementResponse(), logNode, connectionDetails);
    this.queryDetails = queryDetails;
  }

  @Override
  protected void doExecute() throws MxException, SQLException {
    ExecuteStatementResponse executeStatementResponse = (ExecuteStatementResponse) getResponse();

    //Process query and read result
    MxExecuteDmlQuery mxExecuteDmlQuery = new MxExecuteDmlQuery(getConnection(), queryDetails);
    int numberOfAffectedRows = mxExecuteDmlQuery.execute();

    //Fill response object
    executeStatementResponse.setRowsAffected(numberOfAffectedRows);
  }
}

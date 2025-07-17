package externaldatabaseconnector.livepreview.service;

import com.mendix.logging.ILogNode;

import externaldatabaseconnector.database.impl.connection.MxConnection;
import externaldatabaseconnector.livepreview.interfaces.IMxAction;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.pojo.ConnectionDetails;

import java.sql.Connection;

public abstract class MxAbstractAction implements IMxAction {

  private Connection connection;
  private final MxConnection mxConnection;
  private BaseResponse response;
  private ILogNode logNode;

  protected MxAbstractAction(BaseResponse aResponse, ILogNode logNode, ConnectionDetails connectionDetails) {
    this.response = aResponse;
    this.logNode = logNode;
    this.mxConnection = new MxConnection(logNode, connectionDetails);
  }

  @Override
  public BaseResponse execute() {
    try (MxConnection mxCon = mxConnection) {
      connection = mxCon.getConnection();
      response.setConnectionSuccess(true);

      doExecute();

      response.setActionStatus(true);
      response.setDatabaseProductName(connection.getMetaData().getDatabaseProductName());
    } catch (Exception exception) {
      logNode.error(exception.getMessage(), exception);
      response.setExceptionDetails(exception);
    }

    return response;
  }

  protected abstract void doExecute() throws Exception;

  protected BaseResponse getResponse() {
    return this.response;
  }

  protected Connection getConnection() {
    return this.connection;
  }
}

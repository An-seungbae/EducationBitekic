package externaldatabaseconnector.livepreview.service;

import com.mendix.logging.ILogNode;

import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.livepreview.responses.TestConnectionResponse;
import externaldatabaseconnector.pojo.ConnectionDetails;

public class MxDbTestConnection extends MxAbstractAction {
  public MxDbTestConnection(ILogNode logNode, ConnectionDetails connectionDetails) {
    super(new TestConnectionResponse(), logNode, connectionDetails);
  }

  public void doExecute() throws MxException {
    // everything is handled in the MxAbstractAction
  }
}

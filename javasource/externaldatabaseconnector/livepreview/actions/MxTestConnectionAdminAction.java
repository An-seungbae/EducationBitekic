package externaldatabaseconnector.livepreview.actions;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import externaldatabaseconnector.livepreview.constants.IMxAdminActions;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.livepreview.service.MxDbTestConnection;
import externaldatabaseconnector.proxies.constants.Constants;

public class MxTestConnectionAdminAction extends MxAbstractAdminAction {

  private final ILogNode logNode = Core.getLogger(Constants.getLogNode());

  public MxTestConnectionAdminAction() {
    super(IMxAdminActions.TEST_CONNECTION);
  }

  @Override
  protected BaseResponse executeAction() {
    MxDbTestConnection testConnection = new MxDbTestConnection(logNode, getConnectionDetails());
    return testConnection.execute();
  }
}

package externaldatabaseconnector.livepreview.actions;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import externaldatabaseconnector.livepreview.constants.IMxAdminActions;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.livepreview.service.MxDbExecuteQuery;
import externaldatabaseconnector.proxies.constants.Constants;

public class MxExecuteQueryAdminAction extends MxAbstractAdminAction {

  private final ILogNode logNode = Core.getLogger(Constants.getLogNode());

  public MxExecuteQueryAdminAction() {
    super(IMxAdminActions.EXECUTE_SELECT);
  }

  @Override
  protected BaseResponse executeAction() {
    MxDbExecuteQuery mxDbExecuteSelect = new MxDbExecuteQuery(logNode, getConnectionDetails(), getQueryDetails());
    return mxDbExecuteSelect.execute();
  }
}

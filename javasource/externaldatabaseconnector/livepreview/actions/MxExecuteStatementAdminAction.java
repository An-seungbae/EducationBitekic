package externaldatabaseconnector.livepreview.actions;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import externaldatabaseconnector.livepreview.constants.IMxAdminActions;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.livepreview.service.MxDbExecuteDml;
import externaldatabaseconnector.proxies.constants.Constants;

public class MxExecuteStatementAdminAction extends MxAbstractAdminAction {
  private final ILogNode logNode = Core.getLogger(Constants.getLogNode());

  public MxExecuteStatementAdminAction() {
    super(IMxAdminActions.EXECUTE_DML);
  }

  @Override
  protected BaseResponse executeAction() {
    MxDbExecuteDml executeDml = new MxDbExecuteDml(logNode, getConnectionDetails(), getQueryDetails());
    return executeDml.execute();
  }
}

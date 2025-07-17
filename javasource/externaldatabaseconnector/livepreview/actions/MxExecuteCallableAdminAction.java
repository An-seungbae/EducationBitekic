package externaldatabaseconnector.livepreview.actions;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import externaldatabaseconnector.livepreview.constants.IMxAdminActions;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.livepreview.service.MxDbExecuteCallable;
import externaldatabaseconnector.proxies.constants.Constants;

public class MxExecuteCallableAdminAction extends MxAbstractAdminAction {

  private final ILogNode logNode = Core.getLogger(Constants.getLogNode());

  public MxExecuteCallableAdminAction() {
    super(IMxAdminActions.EXECUTE_CALLABLE);
  }

  @Override
  protected BaseResponse executeAction() {
    MxDbExecuteCallable mxDbExecuteCallable = new MxDbExecuteCallable(logNode, getConnectionDetails(), getQueryDetails());
    return mxDbExecuteCallable.execute();
  }
}

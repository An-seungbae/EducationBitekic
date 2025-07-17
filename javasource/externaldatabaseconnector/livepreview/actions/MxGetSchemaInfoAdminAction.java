package externaldatabaseconnector.livepreview.actions;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import externaldatabaseconnector.livepreview.constants.IMxAdminActions;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.livepreview.service.MxDbGetSchemaInfo;
import externaldatabaseconnector.proxies.constants.Constants;

public class MxGetSchemaInfoAdminAction extends MxAbstractAdminAction {

  private final ILogNode logNode = Core.getLogger(Constants.getLogNode());

  public MxGetSchemaInfoAdminAction() {
    super(IMxAdminActions.GET_SCHEMA_INFO);
  }

  @Override
  protected BaseResponse executeAction() {
    MxDbGetSchemaInfo getSchemaInfo = new MxDbGetSchemaInfo(logNode, getConnectionDetails());
    return getSchemaInfo.execute();
  }
}

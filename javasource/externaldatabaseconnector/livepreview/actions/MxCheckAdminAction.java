package externaldatabaseconnector.livepreview.actions;

import externaldatabaseconnector.livepreview.constants.IMxAdminActions;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.livepreview.service.MxCheck;

public class MxCheckAdminAction extends MxAbstractAdminAction {
  public MxCheckAdminAction() {
    super(IMxAdminActions.CHECK_ADMIN_ACTION);
  }

  @Override
  protected BaseResponse executeAction() {
    return new MxCheck().execute();
  }
}

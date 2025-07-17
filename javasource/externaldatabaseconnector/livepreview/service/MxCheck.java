package externaldatabaseconnector.livepreview.service;

import externaldatabaseconnector.livepreview.interfaces.IMxAction;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.livepreview.responses.CheckAdminActionResponse;

public class MxCheck implements IMxAction {

  @Override
  public BaseResponse execute() {
    CheckAdminActionResponse response = new CheckAdminActionResponse();
    response.setActionStatus(true);
    return response;
  }
}

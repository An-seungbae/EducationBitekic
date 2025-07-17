package externaldatabaseconnector.livepreview.responses;

import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxResponses;
import externaldatabaseconnector.livepreview.responses.pojo.CallableResult;

public class ExecuteCallableResponse extends BaseResponse {
  private CallableResult callableResult;

  public CallableResult getCallableResult() {
    return callableResult;
  }

  public void setCallableResult(CallableResult callableResult) {
    this.callableResult = callableResult;
  }

  public JSONObject toJson() {
    JSONObject jsonObject = super.toJson();
    if (this.callableResult != null) {
      jsonObject.put(IMxResponses.CALLABLE_RESULT, this.callableResult.toJson());
    }
    return jsonObject;
  }

}

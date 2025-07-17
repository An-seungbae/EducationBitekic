package externaldatabaseconnector.livepreview.actions;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.AdminAction;
import com.mendix.m2ee.api.AdminException;
import com.mendix.thirdparty.org.json.JSONException;
import com.mendix.thirdparty.org.json.JSONObject;

import externaldatabaseconnector.livepreview.constants.IMxAdminActions;
import externaldatabaseconnector.livepreview.responses.BaseResponse;
import externaldatabaseconnector.pojo.ConnectionDetails;
import externaldatabaseconnector.pojo.MxDatabase;
import externaldatabaseconnector.pojo.QueryDetails;
import externaldatabaseconnector.proxies.constants.Constants;
import externaldatabaseconnector.utils.JsonUtil;
import externaldatabaseconnector.utils.QueryUtils;

public abstract class MxAbstractAdminAction extends AdminAction {
  private final ILogNode logNode = Core.getLogger(Constants.getLogNode());
  protected MxDatabase mxDatabase;

  public MxAbstractAdminAction(String name) {
    super(IMxAdminActions.PREFIX + name);
  }

  private void init(JSONObject jsonObject) {
    this.mxDatabase = JsonUtil.fromJson(jsonObject.getString(IMxAdminActions.INPUT_PARAMS), MxDatabase.class);
  }

  protected ConnectionDetails getConnectionDetails() {
    return this.mxDatabase.getConnectionDetails();
  }

  protected QueryDetails getQueryDetails() {
    QueryUtils queryUtils = new QueryUtils(this.mxDatabase, logNode);
    return queryUtils.getUpdatedQueryDetails();
  }

  @Override
  public JSONObject execute(JSONObject params) throws AdminException, JSONException {
    init(params);
    BaseResponse response = executeAction();

    if (response == null)
      return null;

    return response.toJson();
  }

  protected abstract BaseResponse executeAction();
}

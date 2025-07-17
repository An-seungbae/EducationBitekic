package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.database.exceptions.MxInvalidParameterModeException;
import externaldatabaseconnector.exceptions.IMxErrorMessages;
import externaldatabaseconnector.exceptions.MxException;
import externaldatabaseconnector.mx.impl.DatabaseConnectorException;
import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class SqlParameter {
    protected QueryParameter queryParameter;

    public SqlParameter(QueryParameter queryParameter) {
        this.queryParameter = queryParameter;
    }

    public void prepareCall(CallableStatement cStatement) throws SQLException, MxException {
        switch (this.queryParameter.getParameterMode()) {
            case IN:
                prepareInput(cStatement);
                break;
            case OUT:
                prepareOutput(cStatement);
                break;
            case INOUT:
                prepareInput(cStatement);
                prepareOutput(cStatement);
                break;
            default:
                String errorMessage = String.format(IMxErrorMessages.INVALID_PARAMETER_MODE, this.queryParameter.getParameterMode());
                String messageCode = "E071";

                Map<String, String> messageDataMap = new HashMap<>();
                messageDataMap.put("parameterMode", this.queryParameter.getParameterMode().toString());

                throw new MxInvalidParameterModeException(errorMessage,  messageCode, messageDataMap);
        }
    }


    /**
     * Method to register the output value from an executed CallableStatement based on parameter type.
     */
    protected abstract void prepareOutput(CallableStatement cStatement) throws SQLException, DatabaseConnectorException;

    /**
     * Method to set the input value in a CallableStatement based on parameter type.
     */
    protected abstract void prepareInput(CallableStatement cStatement) throws SQLException, DatabaseConnectorException;
}

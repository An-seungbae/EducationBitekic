package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;
import externaldatabaseconnector.utils.ParameterUtil;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * Wrapper class for all basic primitives. As they all mostly behave the same,
 * aside from <code>SQL_TYPE</code> integer and which methods to call to set or
 * get values in the CallableStatement.
 * <p>
 * If necessary, some conversion might be made from the value stored in Mendix
 * to the database (for instance, with Dates, or when reading numbers).
 */
public abstract class SqlParameterPrimitiveValue<T> extends SqlParameter {
    protected final int SQL_TYPE;

    protected final int parameterIndex;

    protected SqlParameterPrimitiveValue(QueryParameter queryParameter, int SQL_TYPE, int index) {
        super(queryParameter);
        this.SQL_TYPE = SQL_TYPE;
        this.parameterIndex = index;
    }

    public void prepareInput(CallableStatement callableStatement) throws SQLException {
        String paramObjectValue = this.queryParameter.getValue();

        // if the paramObjectValue equals to null, then set the preparedStatement to null for current queryParameter
        if (paramObjectValue.equals("null")) {
            callableStatement.setNull(parameterIndex, SQL_TYPE);
        } else {
            // check if the paramObjectValue equals GUID_FOR_HARDCODED_NULL_STRING, then make the value as "null"
            ParameterUtil.CheckForHardCodedNullString(paramObjectValue, this.queryParameter);

            setStatementValue(callableStatement, this.queryParameter.getValue());
        }
    }

    public void prepareOutput(CallableStatement callableStatement) throws SQLException {
        callableStatement.registerOutParameter(parameterIndex, SQL_TYPE);
    }

    public abstract T getStatementValue(CallableStatement cStatement) throws SQLException;

    public abstract void setStatementValue(CallableStatement cStatement, String value) throws SQLException;

}

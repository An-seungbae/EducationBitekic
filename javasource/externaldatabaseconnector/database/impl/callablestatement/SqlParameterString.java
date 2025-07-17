package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;

public class SqlParameterString extends SqlParameterPrimitiveValue<String> {
    public SqlParameterString(QueryParameter queryParameter, int index, int sqlType) {
        super(queryParameter, sqlType, index);
    }

    public String getStatementValue(CallableStatement callableStatement) throws SQLException {
        return callableStatement.getString(this.parameterIndex);
    }

    public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
        callableStatement.setString(this.parameterIndex, value);
    }
}

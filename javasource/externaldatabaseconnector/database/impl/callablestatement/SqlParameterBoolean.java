package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

public class SqlParameterBoolean extends SqlParameterPrimitiveValue<Boolean> {
    public SqlParameterBoolean(QueryParameter queryParameter, int index) {
        super(queryParameter, Types.BOOLEAN, index);
    }

    public Boolean getStatementValue(CallableStatement callableStatement) throws SQLException {
        return callableStatement.getBoolean(this.parameterIndex);
    }

    public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
        callableStatement.setBoolean(this.parameterIndex, Boolean.parseBoolean(value));
    }
}

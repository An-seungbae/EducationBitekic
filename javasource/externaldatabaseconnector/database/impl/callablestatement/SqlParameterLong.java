package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

public class SqlParameterLong extends SqlParameterPrimitiveValue<Long> {
    public SqlParameterLong(QueryParameter queryParameter, int index) {
        super(queryParameter, Types.BIGINT, index);
    }

    public Long getStatementValue(CallableStatement callableStatement) throws SQLException {
        return callableStatement.getLong(this.parameterIndex);
    }

    public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
        callableStatement.setLong(this.parameterIndex, Long.parseLong(value));
    }
}

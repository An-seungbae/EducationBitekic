package externaldatabaseconnector.database.impl.callablestatement;

import externaldatabaseconnector.pojo.QueryParameter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

public class SqlParameterDatetime extends SqlParameterPrimitiveValue<Date> {
    public SqlParameterDatetime(QueryParameter queryParameter, int index) {
        super(queryParameter, Types.DATE, index);
    }

    public Date getStatementValue(CallableStatement callableStatement) throws SQLException {
        return callableStatement.getDate(this.parameterIndex);
    }

    public void setStatementValue(CallableStatement callableStatement, String value) throws SQLException {
        long epochTime = Long.parseLong(value);
        Date date = new Date(epochTime);
        callableStatement.setDate(this.parameterIndex, new java.sql.Date(date.getTime()));
    }
}

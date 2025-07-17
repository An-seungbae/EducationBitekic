package externaldatabaseconnector.pojo;

import java.util.List;

public class CallableColumnMapping {
    public ColumnMapping OutputEntity;
    public List<ColumnMapping> ResultSetEntityList;

    public ColumnMapping getOutputEntity() {
        return OutputEntity;
    }

    public void setOutputEntity(ColumnMapping outputEntity) {
        OutputEntity = outputEntity;
    }

    public List<ColumnMapping> getResultSetEntityList() {
        return ResultSetEntityList;
    }

    public void setResultSetEntityList(List<ColumnMapping> resultSetEntityList) {
        ResultSetEntityList = resultSetEntityList;
    }
}

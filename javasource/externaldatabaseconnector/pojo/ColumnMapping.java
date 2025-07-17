package externaldatabaseconnector.pojo;

import java.util.Map;

public class ColumnMapping {
    private String EntityName;
    private Map<String, String> ColumnAttributeMapping;

    // Getters and setters
    public String getEntityName() {
        return EntityName;
    }

    public void setEntityName(String entityName) {
        this.EntityName = entityName;
    }

    public Map<String, String> getColumnAttributeMapping() {
        return ColumnAttributeMapping;
    }

    public void setColumnAttributeMapping(Map<String, String> columnAttributeMapping) {
        this.ColumnAttributeMapping = columnAttributeMapping;
    }
}
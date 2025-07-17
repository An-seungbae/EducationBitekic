package externaldatabaseconnector.database.impl.dbschema.provider;

import externaldatabaseconnector.database.constants.IMxDatabaseSchema;
import externaldatabaseconnector.database.impl.dbschema.helper.SchemaMetaDataCollector;
import externaldatabaseconnector.livepreview.responses.pojo.CallableMetaData;
import externaldatabaseconnector.livepreview.responses.pojo.SchemaMetaData;
import externaldatabaseconnector.livepreview.responses.pojo.TableViewMetaData;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class MxBaseSchemaInfoProvider {
  protected final Connection connection;
  protected final String catalog;
  protected final String schema;
  protected final DatabaseMetaData metaData;
  protected final String resolvedSchemaName;

  protected MxBaseSchemaInfoProvider(Connection connection) throws SQLException {
    this.connection = connection;
    this.catalog = connection.getCatalog();
    this.schema = connection.getSchema();
    this.metaData = connection.getMetaData();
    this.resolvedSchemaName = Optional.ofNullable(this.schema)
        .orElse(Optional.ofNullable(this.catalog).orElse(IMxDatabaseSchema.MAIN));
  }

  protected abstract List<TableViewMetaData> getTableMetaData() throws SQLException;

  protected abstract List<TableViewMetaData> getViewMetaData() throws SQLException;

  protected abstract List<CallableMetaData> getProcedureMetaData() throws SQLException;

  protected abstract List<CallableMetaData> getFunctionMetaData() throws SQLException;

  public String getDefaultSchema() {
    return resolvedSchemaName;
  }

  public List<SchemaMetaData> getSchemaMetaData() throws SQLException {

    // prepare a map of metadata group by their schema as key
    SchemaMetaDataCollector<TableViewMetaData> tableProcessor =
        new SchemaMetaDataCollector<>(getTableMetaData(), TableViewMetaData::getSchemaName, TableViewMetaData::getName);

    SchemaMetaDataCollector<TableViewMetaData> viewProcessor =
        new SchemaMetaDataCollector<>(getViewMetaData(), TableViewMetaData::getSchemaName, TableViewMetaData::getName);

    SchemaMetaDataCollector<CallableMetaData> procedureProcessor =
        new SchemaMetaDataCollector<>(getProcedureMetaData(), CallableMetaData::getSchemaName, CallableMetaData::getName);

    SchemaMetaDataCollector<CallableMetaData> functionProcessor =
        new SchemaMetaDataCollector<>(getFunctionMetaData(), CallableMetaData::getSchemaName, CallableMetaData::getName);

    // get all the unique schema names from all the maps
    Set<String> allSchemas =
        SchemaMetaDataCollector.gatherUniqueSchemas(tableProcessor, viewProcessor, procedureProcessor, functionProcessor);

    // group all the list of metadata into schemaMetaData based on the schema name
    return allSchemas.stream()
        .map(aSchema -> new SchemaMetaData(
            aSchema,
            tableProcessor.getMetadataForSchema(aSchema),
            viewProcessor.getMetadataForSchema(aSchema),
            procedureProcessor.getMetadataForSchema(aSchema),
            functionProcessor.getMetadataForSchema(aSchema)
        ))
        .sorted(Comparator.comparing(SchemaMetaData::getName))
        .collect(Collectors.toList());
  }
}

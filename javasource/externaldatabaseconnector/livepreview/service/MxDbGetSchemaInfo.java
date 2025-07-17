package externaldatabaseconnector.livepreview.service;

import com.mendix.logging.ILogNode;

import externaldatabaseconnector.database.exceptions.MxByodClassNotFoundException;
import externaldatabaseconnector.database.impl.dbschema.factory.MxDbSchemaProviderFactory;
import externaldatabaseconnector.database.impl.dbschema.provider.MxBaseSchemaInfoProvider;
import externaldatabaseconnector.livepreview.responses.MxGetSchemaInfoResponse;
import externaldatabaseconnector.livepreview.responses.pojo.SchemaMetaData;
import externaldatabaseconnector.pojo.ConnectionDetails;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class MxDbGetSchemaInfo extends MxAbstractAction {
  private final String databaseType;

  private final Map<String, String> additionalConnectionProperties;

  public MxDbGetSchemaInfo(ILogNode logNode, ConnectionDetails connectionDetails) {
    super(new MxGetSchemaInfoResponse(), logNode, connectionDetails);
    databaseType = connectionDetails.getDatabaseType();
    additionalConnectionProperties = connectionDetails.getAdditionalProperties();
  }

  @Override
  protected void doExecute() throws SQLException, MxByodClassNotFoundException {

    MxBaseSchemaInfoProvider schemaInfoProvider =
        MxDbSchemaProviderFactory.getSchemaInfoProvider(databaseType, getConnection(), additionalConnectionProperties);

    String defaultSchema = schemaInfoProvider.getDefaultSchema();
    List<SchemaMetaData> schemaMetaDataList = schemaInfoProvider.getSchemaMetaData();

    MxGetSchemaInfoResponse getSchemaInfoResponse = (MxGetSchemaInfoResponse) getResponse();
    getSchemaInfoResponse.setDefaultSchema(defaultSchema);
    getSchemaInfoResponse.setSchemaMetaDataList(schemaMetaDataList);
  }
}

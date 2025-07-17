package externaldatabaseconnector.database.impl.dbsource.helper;

import com.zaxxer.hikari.HikariDataSource;
import externaldatabaseconnector.database.impl.dbsource.strategy.MxBaseDataSourceStrategy;

public class MxDataSourceContext {
  private MxBaseDataSourceStrategy dataSourceStrategy;

  public MxDataSourceContext(MxBaseDataSourceStrategy strategy) {
    this.dataSourceStrategy = strategy;
  }

  public HikariDataSource createDataSource() throws Exception {
    return dataSourceStrategy.configureDataSource();
  }
}

package externaldatabaseconnector.database.impl.dbschema.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SchemaMetaDataCollector<T> {
  private final Map<String, List<T>> groupedMetadata;

  public SchemaMetaDataCollector(List<T> metadataList, Function<T, String> schemaExtractor, Function<T, String> nameExtractor) {
    this.groupedMetadata = groupAndSortBySchema(metadataList, schemaExtractor, nameExtractor);
  }

  private Map<String, List<T>> groupAndSortBySchema(List<T> metadataList, Function<T, String> schemaExtractor,
                                                    Function<T, String> nameExtractor) {
    return metadataList.stream()
        .collect(Collectors.groupingBy(
            schemaExtractor,
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.stream()
                    .sorted(Comparator.comparing(nameExtractor))
                    .collect(Collectors.toList())
            )
        ));
  }

  public List<T> getMetadataForSchema(String schema) {
    return groupedMetadata.getOrDefault(schema, Collections.emptyList());
  }

  private Map<String, List<T>> getGroupedMetadata() {
    return groupedMetadata;
  }

  public static Set<String> gatherUniqueSchemas(SchemaMetaDataCollector<?>... processors) {
    return Arrays.stream(processors)
        .flatMap(processor -> processor.getGroupedMetadata().keySet().stream())
        .collect(Collectors.toSet());
  }
}
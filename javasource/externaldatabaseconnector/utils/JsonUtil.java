package externaldatabaseconnector.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import externaldatabaseconnector.database.enums.CallableParameterMode;
import externaldatabaseconnector.pojo.ColumnMapping;
import externaldatabaseconnector.pojo.QueryParameter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonUtil {

  private static final Gson gsonObject =
      new GsonBuilder().registerTypeAdapter(CallableParameterMode.class, new TypeAdapter<CallableParameterMode>() {
        public void write(JsonWriter out, CallableParameterMode value) throws IOException {
          out.value(value.name().toLowerCase());
        }

        public CallableParameterMode read(JsonReader in) throws IOException {
          String inputValue = in.nextString().trim();

          if (inputValue.matches("\\d+")) {
            int ordinal = Integer.parseInt(inputValue);
            CallableParameterMode[] values = CallableParameterMode.values();
            if (ordinal >= 0 && ordinal < values.length) {
              return values[ordinal];
            }
          }

          return CallableParameterMode.valueOf(inputValue.toUpperCase());
        }
      }).create();

  public static <T> T fromJson(String jsonString, Type resultType) {
    return gsonObject.fromJson(jsonString, resultType);
  }

  //Deserialize and return column attribute mapping
  public static ColumnMapping GetColumnAttributeMapping(String columnAttributeMappingJson) {
    Type columnMapType = new TypeToken<ColumnMapping>() {}.getType();
    return fromJson(columnAttributeMappingJson, columnMapType);
  }

  public static List<QueryParameter> getQueryParameters(String aQueryParameters) {
    Type queryParameterMapType = new TypeToken<Map<String, QueryParameter>>() {}.getType();
    Map<String, QueryParameter> queryParameterMap = fromJson(aQueryParameters, queryParameterMapType);
    return new ArrayList<>(queryParameterMap.values());
  }
}
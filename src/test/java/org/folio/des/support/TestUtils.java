package org.folio.des.support;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.des.domain.dto.BursarExportFilterAge;
import org.folio.des.domain.dto.BursarExportFilterCondition;
import org.folio.des.domain.dto.BursarExportFilterPatronGroup;
import org.folio.des.domain.dto.BursarExportJob;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@UtilityClass
public class TestUtils {

  private static final String CLASSPATH_PREFIX = "classpath:";
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  public static ExportConfig getBursarExportConfig() {
    return new ExportConfig()
      .id(UUID.randomUUID().toString())
      .configName(DEFAULT_CONFIG_NAME)
      .exportTypeSpecificParameters(new ExportTypeSpecificParameters()
        .bursarFeeFines(new BursarExportJob()
          .filter(new BursarExportFilterCondition()
            .criteria(new ArrayList<>(List.of(
              new BursarExportFilterAge().numDays(1),
              new BursarExportFilterPatronGroup().patronGroupId(UUID.fromString("0000-00-00-00-000000")))))
            .operation(BursarExportFilterCondition.OperationEnum.AND))));
  }

  public static void setInternalState(Object target, String field, Object value) {
    Class<?> c = target.getClass();
    try {
      var f = getDeclaredFieldRecursive(field, c);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(
        "Unable to set internal state on a private field. [...]", e);
    }
  }

  private static Field getDeclaredFieldRecursive(String field, Class<?> cls) {
    try {
      return cls.getDeclaredField(field);
    } catch (NoSuchFieldException e) {
      if (cls.getSuperclass() != null) {
        return getDeclaredFieldRecursive(field, cls.getSuperclass());
      }
      throw new RuntimeException(String.format("Unable to find field: %s for class: %s", field, cls.getName()), e);
    }
  }

  @SneakyThrows
  public static <T> T loadData(String path, Class<T> cls) {
    return MAPPER.readValue(readContent(path), cls);
  }

  @SneakyThrows
  public static JsonNode loadData(String path) {
    return MAPPER.readTree(readContent(path));
  }

  @SneakyThrows
  private static String readContent(String path) {
    val fullPath = path.startsWith(CLASSPATH_PREFIX) ? path : CLASSPATH_PREFIX + path;
    val resource = new PathMatchingResourcePatternResolver().getResource(fullPath);
    return new BufferedReader(new InputStreamReader(resource.getInputStream()))
      .lines().collect(Collectors.joining(System.lineSeparator()));
  }

}

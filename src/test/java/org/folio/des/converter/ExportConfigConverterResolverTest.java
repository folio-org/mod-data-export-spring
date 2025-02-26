package org.folio.des.converter;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.config.scheduling.QuartzSchemaInitializer;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ModelConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {JacksonConfiguration.class, ServiceConfiguration.class})
@EnableAutoConfiguration(exclude = {BatchAutoConfiguration.class})
class ExportConfigConverterResolverTest {

  @Autowired
  private ExportConfigConverterResolver resolver;
  @MockitoBean
  private ConfigurationClient client;
  @MockitoBean
  private Scheduler scheduler;
  @MockitoBean
  private QuartzSchemaInitializer quartzSchemaInitializer;

  @Test
  @DisplayName("Should retrieve converter for specific export type if converter is registered in the resolver")
  void shouldRetrieveConverterForSpecifiedExportTypeIfConverterIsRegisteredInTheResolver() {
    Converter<ExportConfig, ModelConfiguration> converter = resolver.resolve(ExportType.BURSAR_FEES_FINES);
    assertSame(DefaultExportConfigToModelConfigConverter.class, converter.getClass());
  }

  @Test
  @DisplayName("Should retrieve default converter for specific export type if converter is not registered in the resolver")
  void shouldRetrieveDefaultConverterForSpecifiedExportTypeIfConverterIsNotRegisteredInTheResolver() {
    Converter<ExportConfig, ModelConfiguration> converter = resolver.resolve(ExportType.CIRCULATION_LOG);
    assertSame(DefaultExportConfigToModelConfigConverter.class, converter.getClass());
  }

  @Test
  @DisplayName("Should retrieve default converter if export type is not provided")
  void shouldRetrieveDefaultConverterIfExportTypeIsNotProvided() {
    Converter<ExportConfig, ModelConfiguration> converter = resolver.resolve(null);
    assertSame(DefaultExportConfigToModelConfigConverter.class, converter.getClass());
  }
}

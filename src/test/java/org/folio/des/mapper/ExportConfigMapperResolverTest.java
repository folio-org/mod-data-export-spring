package org.folio.des.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.EnumMap;

import org.folio.des.CopilotGenerated;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.mapper.acquisition.ClaimsExportConfigMapper;
import org.folio.des.mapper.acquisition.EdifactExportConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@CopilotGenerated(model = "Claude Sonnet 4.5")
@ExtendWith(MockitoExtension.class)
class ExportConfigMapperResolverTest {

  @Mock
  private EdifactExportConfigMapper edifactMapper;
  @Mock
  private ClaimsExportConfigMapper claimsMapper;
  @Mock
  private DefaultExportConfigMapper defaultMapper;

  private ExportConfigMapperResolver resolver;
  private EnumMap<ExportType, BaseExportConfigMapper> mappers;

  @BeforeEach
  void setUp() {
    mappers = new EnumMap<>(ExportType.class);
  }

  @Test
  @DisplayName("Should resolve default mapper when export type is not in map")
  void testResolveDefaultMapperWhenTypeNotInMap() {
    // Given
    resolver = new ExportConfigMapperResolver(mappers, defaultMapper);
    ExportType exportType = ExportType.BURSAR_FEES_FINES;

    // When
    BaseExportConfigMapper result = resolver.resolve(exportType);

    // Then
    assertNotNull(result);
    assertSame(defaultMapper, result);
  }

  @Test
  @DisplayName("Should resolve specific mapper when export type is in map")
  void testResolveSpecificMapperWhenTypeInMap() {
    // Given
    ExportType exportType = ExportType.EDIFACT_ORDERS_EXPORT;
    mappers.put(exportType, edifactMapper);
    resolver = new ExportConfigMapperResolver(mappers, defaultMapper);

    // When
    BaseExportConfigMapper result = resolver.resolve(exportType);

    // Then
    assertNotNull(result);
    assertSame(edifactMapper, result);
  }

  @Test
  @DisplayName("Should resolve different mappers for different export types")
  void testResolveDifferentMappersForDifferentTypes() {
    // Given
    mappers.put(ExportType.EDIFACT_ORDERS_EXPORT, edifactMapper);
    mappers.put(ExportType.CIRCULATION_LOG, claimsMapper);
    resolver = new ExportConfigMapperResolver(mappers, defaultMapper);

    // When
    BaseExportConfigMapper edifactResult = resolver.resolve(ExportType.EDIFACT_ORDERS_EXPORT);
    BaseExportConfigMapper claimsResult = resolver.resolve(ExportType.CIRCULATION_LOG);
    BaseExportConfigMapper defaultResult = resolver.resolve(ExportType.BURSAR_FEES_FINES);

    // Then
    assertSame(edifactMapper, edifactResult);
    assertSame(claimsMapper, claimsResult);
    assertSame(defaultMapper, defaultResult);
  }

  @Test
  @DisplayName("Should handle null export type gracefully and return default mapper")
  void testHandleNullExportType() {
    // Given
    resolver = new ExportConfigMapperResolver(mappers, defaultMapper);

    // When
    BaseExportConfigMapper result = resolver.resolve(null);

    // Then
    assertNotNull(result);
    assertSame(defaultMapper, result);
  }

  @Test
  @DisplayName("Should return default mapper when map is empty")
  void testReturnDefaultMapperWhenMapIsEmpty() {
    // Given
    resolver = new ExportConfigMapperResolver(new EnumMap<>(ExportType.class), defaultMapper);

    // When
    BaseExportConfigMapper result = resolver.resolve(ExportType.EDIFACT_ORDERS_EXPORT);

    // Then
    assertNotNull(result);
    assertSame(defaultMapper, result);
  }

  @Test
  @DisplayName("Should consistently return same mapper for same export type")
  void testConsistentlyReturnSameMapperForSameType() {
    // Given
    ExportType exportType = ExportType.EDIFACT_ORDERS_EXPORT;
    mappers.put(exportType, edifactMapper);
    resolver = new ExportConfigMapperResolver(mappers, defaultMapper);

    // When
    BaseExportConfigMapper result1 = resolver.resolve(exportType);
    BaseExportConfigMapper result2 = resolver.resolve(exportType);
    BaseExportConfigMapper result3 = resolver.resolve(exportType);

    // Then
    assertSame(result1, result2);
    assertSame(result2, result3);
    assertSame(edifactMapper, result1);
  }

  @Test
  @DisplayName("Should not invoke any methods on the mappers during resolution")
  void testResolverDoesNotInvokeMapperMethods() {
    // Given
    mappers.put(ExportType.EDIFACT_ORDERS_EXPORT, edifactMapper);
    resolver = new ExportConfigMapperResolver(mappers, defaultMapper);

    // When
    resolver.resolve(ExportType.EDIFACT_ORDERS_EXPORT);
    resolver.resolve(ExportType.BURSAR_FEES_FINES);

    // Then - no methods should be invoked on the mappers
    verifyNoInteractions(edifactMapper);
    verifyNoInteractions(defaultMapper);
  }

  @Test
  @DisplayName("Should handle multiple export types in resolver")
  void testHandleMultipleExportTypesInResolver() {
    // Given
    BaseExportConfigMapper mapper1 = mock(BaseExportConfigMapper.class);
    BaseExportConfigMapper mapper2 = mock(BaseExportConfigMapper.class);
    BaseExportConfigMapper mapper3 = mock(BaseExportConfigMapper.class);

    mappers.put(ExportType.EDIFACT_ORDERS_EXPORT, mapper1);
    mappers.put(ExportType.CIRCULATION_LOG, mapper2);
    mappers.put(ExportType.BATCH_VOUCHER_EXPORT, mapper3);

    resolver = new ExportConfigMapperResolver(mappers, defaultMapper);

    // When
    BaseExportConfigMapper resolvedMapper1 = resolver.resolve(ExportType.EDIFACT_ORDERS_EXPORT);
    BaseExportConfigMapper resolvedMapper2 = resolver.resolve(ExportType.CIRCULATION_LOG);
    BaseExportConfigMapper resolvedMapper3 = resolver.resolve(ExportType.BATCH_VOUCHER_EXPORT);
    BaseExportConfigMapper resolvedDefault = resolver.resolve(ExportType.BURSAR_FEES_FINES);

    // Then
    assertSame(mapper1, resolvedMapper1);
    assertSame(mapper2, resolvedMapper2);
    assertSame(mapper3, resolvedMapper3);
    assertSame(defaultMapper, resolvedDefault);
  }

}


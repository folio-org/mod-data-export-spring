package org.folio.des.support;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.folio.des.domain.dto.BursarExportFilterAge;
import org.folio.des.domain.dto.BursarExportFilterCondition;
import org.folio.des.domain.dto.BursarExportFilterPatronGroup;
import org.folio.des.domain.dto.BursarExportJob;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {

	public static ExportConfig getBursarExportConfig() {
		return new ExportConfig()
				.id(UUID.randomUUID().toString())
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

}

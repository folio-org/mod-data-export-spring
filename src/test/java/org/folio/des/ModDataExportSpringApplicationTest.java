package org.folio.des;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Test;

class ModDataExportSpringApplicationTest {

  @Test
  void exceptionOnMissingSystemUserPassword() {
    var e = assertThrows(IllegalArgumentException.class, () -> ModDataExportSpringApplication.main(null));
    assertThat(e.getMessage(), containsString(ModDataExportSpringApplication.SYSTEM_USER_PASSWORD));
  }

}

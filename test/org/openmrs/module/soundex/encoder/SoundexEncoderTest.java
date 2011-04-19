/*
  */
package org.openmrs.module.soundex.encoder;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class SoundexEncoderTest extends BaseContextSensitiveTest {

  @Test
  public void testSoundexCodesFromDBTable() {
    SessionFactory sf = Context.getRegisteredComponents(SessionFactory.class).get(0);

    assertNotNull(sf);
  }
}

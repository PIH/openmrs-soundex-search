/*
  */
package org.openmrs.module.soundex.encoder;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Role;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.TestUtil;
import org.openmrs.util.OpenmrsConstants;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class SoundexEncoderTest extends BaseContextSensitiveTest {

  private class NameRecord {

    int person_name_id;
    String given_name;
    String middle_name;
    String family_name;
  }

  @Override
  public Boolean useInMemoryDatabase() {
    return false;
  }

  @Override
  public Properties getRuntimeProperties() {
    if (runtimeProperties == null) {
      runtimeProperties = TestUtil.getRuntimeProperties(getWebappName());

    // if we're using the in-memory hypersonic database, add those
    // connection properties here to override what is in the runtime
    // properties
      runtimeProperties.setProperty(Environment.DIALECT, MySQL5InnoDBDialect.class.getName());
      runtimeProperties.setProperty(Environment.URL, "jdbc:mysql://localhost/openmrs?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=utf8");
      runtimeProperties.setProperty(Environment.DRIVER, "com.mysql.jdbc.Driver");
      runtimeProperties.setProperty(Environment.USER, "openmrs");
      runtimeProperties.setProperty(Environment.PASS, "openmrs");

      // these two properties need to be set in case the user has this exact
      // phrasing in their runtime file.
      runtimeProperties.setProperty("connection.username", "openmrs");
      runtimeProperties.setProperty("connection.password", "openmrs");

      runtimeProperties.setProperty("junit.username", "cneumann");
      runtimeProperties.setProperty("junit.password", "cneumann2");

    }

    return runtimeProperties;

  }

  @Test
  public void testSoundexCodesFromDBTable() throws Exception {

    // authenticate wertet die runtime properties junit.username und junit.password aus falls vorhanden
    // falls nicht, wird ein Dialog zum Eingeben der Credentials angezeigt
    authenticate();

    final String queryString = "SELECT person_name_id, given_name, middle_name, family_name " +
            "FROM person_name " +
            "WHERE person_name_id>'16960' " +
            "AND person_name_id<'16990';";
    final SQLQuery personNameQuery = getCurrentSession().createSQLQuery(queryString);
    final Iterator personNameIterator = personNameQuery.list().iterator();

    while (personNameIterator.hasNext()) {
      Object[] record = (Object[])personNameIterator.next();
      NameRecord nameRecord = new NameRecord();
      nameRecord.person_name_id = (Integer)record[0];
      nameRecord.given_name     = (String)record[1];
      nameRecord.middle_name    = (String)record[2];
      nameRecord.family_name    = (String)record[3];

      NameRecord codeRecord = getCodesFromDB(nameRecord);

      /*
      System.out.println("person_name_id " + nameRecord.person_name_id + ": "
                          + "\tgiven_name=" + nameRecord.given_name + "(" + codeRecord.given_name +"), "
                          + "\tfamily_name=" + nameRecord.family_name + "(" + codeRecord.family_name +")");
*/
      System.out.println("checking person_name_id " + nameRecord.person_name_id + "...");
      checkEncoder(new ChichewaSoundexEncoder(), nameRecord, codeRecord);
//      checkEncoder(new KinyarwandaSoundexEncoder(), nameRecord, codeRecord);
    }
  }

  private void checkEncoder(StringEncoder encoder, NameRecord nameRecord, NameRecord codeRecord) throws EncoderException {
    assertEquals(encoder.getClass().getName() + " error", codeRecord.given_name, encoder.encode(nameRecord.given_name));
    assertEquals(encoder.getClass().getName() + " error", codeRecord.middle_name, encoder.encode(nameRecord.middle_name));
    assertEquals(encoder.getClass().getName() + " error", codeRecord.family_name, encoder.encode(nameRecord.family_name));
  }
  
  private static Session getCurrentSession() {
    SessionFactory sf = Context.getRegisteredComponents(SessionFactory.class).get(0);
    return sf.getCurrentSession();
  }

  private NameRecord getCodesFromDB(NameRecord nameRecord) {
    final SQLQuery query = getCurrentSession().createSQLQuery(getNameCodeQueryById(nameRecord.person_name_id));
    final Iterator iterator = query.list().iterator();

    if (iterator.hasNext()) {
      Object[] result = (Object[]) iterator.next();
      NameRecord codeRecord = new NameRecord();
      codeRecord.person_name_id = nameRecord.person_name_id;
      codeRecord.given_name = (String) result[0];
      codeRecord.middle_name = (String) result[1];
      codeRecord.family_name = (String) result[2];
      return codeRecord;
    }

    return null;
  }

  private String getNameCodeQueryById(int person_name_id) {
    return "SELECT given_name_code, middle_name_code, family_name_code " +
            "FROM person_name_code " +
            "WHERE person_name_id='" + person_name_id + "';";
  }
}

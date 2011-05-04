/*
  */
package org.openmrs.module.soundex.encoder;

import junit.framework.Assert;
import org.apache.commons.codec.EncoderException;
import org.hibernate.HibernateException;
import org.hibernate.PropertyAccessException;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.soundex.advisor.PatientServiceAroundAdvisor;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.TestUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class SoundexEncoderDBTest extends BaseContextSensitiveTest {

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

    // authenticate reads runtime properties junit.username and junit.password if available
    // if not, a login dialog is presented
    authenticate();

    // select all non-voided patients
    final String queryString = "SELECT pn.person_name_id, pn.given_name, pn.middle_name, pn.family_name " +
            "FROM person_name pn, patient p " +
            "WHERE p.patient_id = pn.person_id " +
            "AND p.voided = '0' " +
            "AND pn.voided = '0';";

    final SQLQuery personNameQuery = getCurrentSession().createSQLQuery(queryString);
    final Iterator personNameIterator = personNameQuery.list().iterator();

    int counter = 0;
    Object[] record;
    NameRecord nameRecord = new NameRecord();

    while (personNameIterator.hasNext()) {
      record = (Object[]) personNameIterator.next();
      nameRecord.person_name_id = (Integer) record[0];
      nameRecord.given_name = (String) record[1];
      nameRecord.middle_name = (String) record[2];
      nameRecord.family_name = (String) record[3];

//      System.out.println("person_name_id: " + nameRecord.person_name_id);
//      System.out.println("   given  name: " + nameRecord.given_name);
//      System.out.println("   family name: " + nameRecord.family_name);

      NameRecord codeRecord = getCodesFromDB(nameRecord);

      assertNotNull(codeRecord);
      checkEncoder(new SoundexEncoder(), nameRecord, codeRecord);
      counter++;

    }

    System.out.println("verified " + counter + " patient codes");
  }

  private static String buildSqlQueryString(String code) {
    return "SELECT p.patient_id " +
            "FROM patient p, person_name pn, person_name_code pnc " +
            "WHERE (" +
            "   pnc.given_name_code   LIKE '" + code + "%' " +
            "OR pnc.middle_name_code  LIKE '" + code + "%' " +
            "OR pnc.family_name_code  LIKE '" + code + "%' " +
            "OR pnc.family_name2_code  LIKE '" + code + "%' " +
            ")" +
            "AND pnc.person_name_id = pn.person_name_id " +
            "AND pn.person_id = p.patient_id " +
            "AND pn.voided = '0' " +
            "AND p.voided = '0';";
  }


  @Test
  public void testSoundexCodeLookup() throws Exception {

    // authenticate wertet die runtime properties junit.username und junit.password aus falls vorhanden
    // falls nicht, wird ein Dialog zum Eingeben der Credentials angezeigt
    authenticate();

    SoundexEncoder encoder = new SoundexEncoder();

    String code = encoder.encode("Misheck");
    System.out.println("code: " + code);

    final String queryString = buildSqlQueryString(code);

    System.out.println("querystring: " + queryString);

    final SQLQuery query = getCurrentSession().createSQLQuery(queryString);

    final Iterator iterator = query.list().iterator();

    int counter = 0;
    while (iterator.hasNext()) {

      counter++;
      int personId = (Integer) iterator.next();

      System.out.println("personId=" + personId);

      final Person person = Context.getPersonService().getPerson(personId);

      Assert.assertNotNull(person);
      System.out.println("   " + person.getGivenName() + " " + person.getMiddleName() + " " + person.getFamilyName());

      String given_name_code = encoder.encode(person.getGivenName());
      String middle_name_code = encoder.encode(person.getMiddleName());
      String family_name_code = encoder.encode(person.getFamilyName());
      String family_name2_code = encoder.encode(person.getPersonName().getFamilyName2());

      assertTrue((given_name_code != null && given_name_code.startsWith(code))
              || (middle_name_code != null && middle_name_code.startsWith(code))
              || (family_name_code != null && family_name_code.startsWith(code))
              || (family_name2_code != null && family_name2_code.startsWith(code)));
    }

    System.out.println("found " + counter + " matches!");
  }

  @Test
  public void testPatientServiceAroundAdvisor() throws Exception {

    // authenticate wertet die runtime properties junit.username und junit.password aus falls vorhanden
    // falls nicht, wird ein Dialog zum Eingeben der Credentials angezeigt
    authenticate();

    executeSoundexSearch("Mishek", Arrays.asList(28103, 51031, 48368, 55111));
    executeSoundexSearch("Waters", Arrays.asList(16999, 57165, 44961, 36281));
    executeSoundexSearch("Thiery", Arrays.asList(20524, 53584, 30550, 50339));
    executeSoundexSearch("Wanda", Arrays.asList(29080, 53425, 41884, 38594));
    executeSoundexSearch("Alina", Arrays.asList(16041, 50045, 56650, 16105));
  }

  private void executeSoundexSearch(String queryString, List<Integer> ids) {
    List<Integer> patientIds = new Vector<Integer>(ids);
    System.out.println("soundex search string: " + queryString);
    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    final PatientServiceAroundAdvisor.SoundexSearchAdvice advice = (PatientServiceAroundAdvisor.SoundexSearchAdvice) advisor.getAdvice();

    final List<Patient> patients = advice.executeSoundexSearch(queryString);
    assertTrue(patients.size() > 0);


    int counter = 0;
    for (Patient patient : patients) {
      counter++;
      printPatient(patient, counter);
      if (patientIds.contains(patient.getId())) {
        patientIds.remove(patient.getId());
      }
    }

    assertEquals(0, patientIds.size());

  }

  private void checkEncoder(SoundexEncoder encoder, NameRecord nameRecord, NameRecord codeRecord) throws EncoderException {
//    if (!nameRecord.given_name.toUpperCase().startsWith("Q")) {
    assertEquals("error for " + nameRecord.given_name, codeRecord.given_name, encoder.encode(nameRecord.given_name));
//    } else {
//      System.out.println("FAILED WITH " + nameRecord.given_name);
//    }
    assertEquals("error for " + nameRecord.middle_name, codeRecord.middle_name, encoder.encode(nameRecord.middle_name));
    assertEquals("error for " + nameRecord.family_name, codeRecord.family_name, encoder.encode(nameRecord.family_name));
  }

  @Test
  public void testIndividualPatients() throws Exception {

    authenticate();

    int[] patientIds = new int[]{54114, 56076, 51658, 38386, 40161, 37965, 38388, 24315, 19738, 24327, 56607};


    for (int id: patientIds) {
      System.out.println("checking patient: " + id);
      Patient patient = Context.getPatientService().getPatient(id);
      assertNotNull(patient);
      printPatient(patient);
    }

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


  @Test
  public void testSoundexResultsIncludeConventional() throws Exception {

    authenticate();

    ensureSoundexResultsIncludeConventional("Water");
    ensureSoundexResultsIncludeConventional("Agnes");
    ensureSoundexResultsIncludeConventional("Ivon");
    ensureSoundexResultsIncludeConventional("Wan");
    ensureSoundexResultsIncludeConventional("Kel");
    ensureSoundexResultsIncludeConventional("Bridget");
    ensureSoundexResultsIncludeConventional("XXXXXXXXX");
    ensureSoundexResultsIncludeConventional("");
  }

  private void ensureSoundexResultsIncludeConventional(String name) {

    System.out.println("checking results for name " + name);
    final List<Patient> conventionalResults = Context.getPatientService().getPatients(name);

    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    final PatientServiceAroundAdvisor.SoundexSearchAdvice advice = (PatientServiceAroundAdvisor.SoundexSearchAdvice) advisor.getAdvice();

    final List<Patient> soundexResults = advice.executeSoundexSearch(name);

    assertTrue(soundexResults.size() >= conventionalResults.size());

    for (Patient patient : conventionalResults) {
      System.out.println("checking " + "patient " + patient.getGivenName() + (patient.getMiddleName() != null ? " " + patient.getMiddleName() : "") + " " + patient.getFamilyName());
      assertTrue("patient " + patient.getGivenName() + " " + patient.getFamilyName() + " not in soundex result: ", soundexResults.contains(patient));
    }

  }


  @Test
  public void testGivenNameSoundexQuery() throws Exception {

    authenticate();

    executeGivenNameSoundexQuery("Emily");
    executeGivenNameSoundexQuery("Albert");
    executeGivenNameSoundexQuery("Mary");
    executeGivenNameSoundexQuery("Alina");
    executeGivenNameSoundexQuery("Whatson");
    executeGivenNameSoundexQuery("XXXXXXXXXX");
    executeGivenNameSoundexQuery("");

  }

  private void executeGivenNameSoundexQuery(String queryString) {

    System.out.println("searching for given name " + queryString);
    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    final String sql = advisor.buildSoundexGivenNameQueryString(queryString, 5000);

    System.out.println("sql: " + sql);

    int person_id = 0;
    int counter = 0;

    try {

      final SQLQuery query = getCurrentSession().createSQLQuery(sql);

      final Iterator iterator = query.list().iterator();

      while (iterator.hasNext()) {
        counter++;
        person_id = (Integer) iterator.next();

        final Patient patient;
        patient = Context.getPatientService().getPatient(person_id);

//        printPatient(patient, counter);
      }

    } catch (PropertyAccessException e) {
      System.out.println("XXXXXXXXXX ERROR for patient " + person_id);
    } catch (HibernateException e) {
      assertTrue(e.getMessage().startsWith("A collection with cascade"));
      System.out.println("XXXXXXXXXX ERROR for patient " + person_id);
    }

    System.out.println("found " + counter + " results for name " + queryString);
  }

  @Test
  public void testFamilyNameSoundexQuery() throws Exception {

    authenticate();

    executeFamilyNameSoundexQuery("Water");
    executeFamilyNameSoundexQuery("Moses");
    executeFamilyNameSoundexQuery("Peter");
    executeFamilyNameSoundexQuery("Tampa");
    executeFamilyNameSoundexQuery("Lankis");
    executeFamilyNameSoundexQuery("XXXXXXXX");
    executeFamilyNameSoundexQuery("");
  }

  private void executeFamilyNameSoundexQuery(String queryString) {
    System.out.println("searching for family name " + queryString);

    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    final String sql = advisor.buildSoundexFamilyNameQueryString(queryString, 1000);

    System.out.println("sql: " + sql);

    final SQLQuery query = getCurrentSession().createSQLQuery(sql);
    final Iterator iterator = query.list().iterator();

    int counter = 0;
    while (iterator.hasNext()) {
      counter++;
      int person_id = (Integer) iterator.next();

      final Patient patient = Context.getPatientService().getPatient(person_id);

      printPatient(patient, counter);
    }
    System.out.println("found " + counter + " results for name " + queryString);
  }

  @Test
  public void testGivenAndFamilyNameSoundexQuery() throws Exception {

    authenticate();

    executeGivenAndFamilyNameSoundexQuery("Mary", "Banda");
    executeGivenAndFamilyNameSoundexQuery("Ganizani ", "Kagomo");
    executeGivenAndFamilyNameSoundexQuery("Fatuma", "Bakali");
    executeGivenAndFamilyNameSoundexQuery("Agnes", "Moffat");
    executeGivenAndFamilyNameSoundexQuery("Agnes", "Mo");

  }

  private void executeGivenAndFamilyNameSoundexQuery(String givenNameQueryString, String familyNameQueryString) {
    System.out.println("searching for name " + givenNameQueryString + " " + familyNameQueryString);

    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    final String sql = advisor.buildSoundexGivenAndFamilyNameQueryString(givenNameQueryString, familyNameQueryString, 5000);

    System.out.println("sql: " + sql);

    final SQLQuery query = getCurrentSession().createSQLQuery(sql);
    final Iterator iterator = query.list().iterator();

    int counter = 0;
    while (iterator.hasNext()) {
      int person_id = (Integer) iterator.next();

      final Patient patient = Context.getPatientService().getPatient(person_id);

      counter++;
      printPatient(patient, counter);
    }
  }

  private void printPatient(Patient patient) {
    printPatient(patient, 0);
  }

  private void printPatient(Patient patient, int n) {

    System.out.println( (n > 0 ? n + ": " : "") +
                        patient.getGivenName() + " "
                       + patient.getFamilyName() + " " +
                       (patient.getPersonName().getFamilyName2() != null && !patient.getPersonName().getFamilyName2().equalsIgnoreCase("Unknown") ? patient.getPersonName().getFamilyName2() + " " : "")
                       + "(" + patient.getPatientId() + ")");
  }

}

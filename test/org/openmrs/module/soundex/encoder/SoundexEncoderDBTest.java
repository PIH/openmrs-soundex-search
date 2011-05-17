/**
 * Copyright (C) 2011 innoQ Deutschland GmbH
 *
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * @author Arnd Kleinbeck, innoQ Deutschland GmbH, http://www.innoq.com
 */
package org.openmrs.module.soundex.encoder;

import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.soundex.SoundexRuntimePropertyAccess;
import org.openmrs.module.soundex.advisor.PatientServiceAroundAdvisor;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.TestUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for context-sensitive tests of the SoundexEncoder and the PatientServiceAroundAdvisor.
 * It requires a loaded patient database.
 */
public class SoundexEncoderDBTest extends BaseContextSensitiveTest {

  /**
   * Limit the SQL result set and the soundex result set to a reasonable value for testing.
   */
  @Before
  public void adjustLimits() {

    setDefaultResultLimit(100);
    setDefaultSqlLimit(100);
  }

  /**
   * Sets the soundex search result limit.
   * @param defaultResultLimit the new result set limit
   */
  public static void setDefaultResultLimit(int defaultResultLimit) {
    addRuntimeProperty(SoundexRuntimePropertyAccess.DEFAULT_RESULT_LIMIT_TAG, String.valueOf(defaultResultLimit));
  }

  /**
   * Sets the SQL query result set limit.
   * @param defaultResultLimit the new result set limit
   */
  public static void setDefaultSqlLimit(int defaultResultLimit) {
    addRuntimeProperty(SoundexRuntimePropertyAccess.DEFAULT_SQL_LIMIT_TAG, String.valueOf(defaultResultLimit));
  }

  /**
   * Helper method for adjusting the runtime properties used by this test.
   * @param property_name name of the property to set
   * @param property_value value of the property to set
   */
  public static void addRuntimeProperty(String property_name, String property_value) {
    Properties props = new Properties();
    props.putAll(Context.getRuntimeProperties());
    props.setProperty(property_name, property_value);
    Context.setRuntimeProperties(props);
  }

  /**
   * Class for holding the name information and id for a patient.
   */
  private class NameRecord {

    int person_name_id;
    String given_name;
    String middle_name;
    String family_name;
  }

  /**
   * Do not use in memory database. Use real MySQL DB instead.
   * @return false to indicate not using an in memory db.
   */
  @Override
  public Boolean useInMemoryDatabase() {
    return false;
  }

  /**
   * Defines test specific runtime properties.
   * @return the runtime properties to use for this test
   */
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

  /**
   * Checks the prepared soundex codes in the person_name_code table of the openmrs database.
   * @throws Exception in case of errors
   */
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

      NameRecord codeRecord = getCodesFromDB(nameRecord);

      assertNotNull(codeRecord);
      checkEncoder(new SoundexEncoder(), nameRecord, codeRecord);
      counter++;
    }

    System.out.println("verified " + counter + " patient codes");
  }

  /**
   * Asserts that individual patients from the given patient database are found by the soundex search.
   * @throws Exception in case of errors
   */
  @Test
  public void testPatientServiceAroundAdvisor() throws Exception {

    // authenticate evaluates the runtime properties junit.username and junit.password if available
    // if not, a dialog for entering the user credentials pops up
    authenticate();

    executeSoundexSearch("Mishek", Arrays.asList(28103, 51031, 48368, 55111));
    executeSoundexSearch("Waters", Arrays.asList(16999, 57165, 44961, 36281));
    executeSoundexSearch("Thiery", Arrays.asList(22058, 45201, 51357, 52352));
    executeSoundexSearch("Wanda",  Arrays.asList(29080, 53425, 41884, 38594));
    executeSoundexSearch("Alina",  Arrays.asList(27613, 30176, 16657, 41780));
  }

  /**
   * Executes a soundex search and verifies that the provided person ids are contained in the result set.
   * @param queryString the name query string
   * @param ids the list of ids to be checked
   */
  private void executeSoundexSearch(String queryString, Collection<Integer> ids) {
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

  /**
   * Make sure that given soundex codes can be reproduced by soundex encoder.
   * @param encoder the encoder instance
   * @param nameRecord the patient infos
   * @param codeRecord the soundex name codes
   */
  private void checkEncoder(SoundexEncoder encoder, NameRecord nameRecord, NameRecord codeRecord) {
    assertEquals("error for " + nameRecord.given_name, codeRecord.given_name, encoder.encode(nameRecord.given_name));
    assertEquals("error for " + nameRecord.middle_name, codeRecord.middle_name, encoder.encode(nameRecord.middle_name));
    assertEquals("error for " + nameRecord.family_name, codeRecord.family_name, encoder.encode(nameRecord.family_name));
  }

  /**
   * Asserts some individual patients lookup.
   * @throws Exception in case of errors
   */
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

  /**
   * Get the current Hibernate session instance.
   * @return the Hibernate session instance
   */
  private static Session getCurrentSession() {
    SessionFactory sf = Context.getRegisteredComponents(SessionFactory.class).get(0);
    return sf.getCurrentSession();
  }

  /**
   * Gets the name codes for a given person from the person_name_code table.
   * @param nameRecord the record with the person names
   * @return the record with the person name soundex codes
   */
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

  /**
   * Build the namecode SQL query for a given person name id.
   * @param person_name_id the person name id
   * @return the sql query
   */
  private String getNameCodeQueryById(int person_name_id) {
    return "SELECT given_name_code, middle_name_code, family_name_code " +
            "FROM person_name_code " +
            "WHERE person_name_id='" + person_name_id + "';";
  }


  /**
   * Asserts that soundex search results are a superset of the conventional results.
   * @throws Exception in case of errors
   */
  @Test
  public void testSoundexResultsIncludeConventional() throws Exception {

    setDefaultResultLimit(9999);
    setDefaultSqlLimit(9999);

    // authenticate evaluates the runtime properties junit.username and junit.password if available
    // if not, a dialog for entering the user credentials pops up
    authenticate();

    ensureSoundexResultsIncludeConventional("Water");
    ensureSoundexResultsIncludeConventional("Ivon");
    ensureSoundexResultsIncludeConventional("Wanda");
    ensureSoundexResultsIncludeConventional("Bridget");
    ensureSoundexResultsIncludeConventional("XXXXXXXXX");
    ensureSoundexResultsIncludeConventional("");
  }

  /**
   * This test makes sure that the soundex search results are a superset of the conventional search results for a query.
   * @param name the name to search for
   */
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

  /**
   * Runs two different kinds of soundex search. One for the given name and one for the family name.
   * @throws Exception in case of errors
   */
  @Test
  public void testDoubleSoundexQuery() throws Exception {

    // authenticate evaluates the runtime properties junit.username and junit.password if available
    // if not, a dialog for entering the user credentials pops up
    authenticate();

    final long time = GregorianCalendar.getInstance().getTime().getTime();
    executeGivenNameSoundexQuery("Mary");
    executeFamilyNameSoundexQuery("Mary");

    System.out.println("time (ms): " + (GregorianCalendar.getInstance().getTime().getTime() - time));

  }


  /**
   * Runs given name soundex searches.
   * @throws Exception in case of errors
   */
  @Test
  public void testGivenNameSoundexQuery() throws Exception {

    // authenticate evaluates the runtime properties junit.username and junit.password if available
    // if not, a dialog for entering the user credentials pops up
    authenticate();

    executeGivenNameSoundexQuery("Emily");
    executeGivenNameSoundexQuery("Albert");
    executeGivenNameSoundexQuery("Mary");
    executeGivenNameSoundexQuery("Alina");
    executeGivenNameSoundexQuery("Whatson");
    executeGivenNameSoundexQuery("XXXXXXXXXX");
    executeGivenNameSoundexQuery("");

  }

  /**
   * Executes given Name Soundex search.
   * @param queryString the given name
   */
  private void executeGivenNameSoundexQuery(String queryString) {

//    System.out.println("searching for given name " + queryString);
    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    final String sql = advisor.buildSoundexGivenNameQueryString(queryString, 100);

//    System.out.println("sql: " + sql);

    final long start = System.currentTimeMillis();

    final SQLQuery query = getCurrentSession().createSQLQuery(sql);

    List<Patient> patients;
    if (true) {
      patients = new ArrayList<Patient>();
      Iterator iterator = query.list().iterator();
      while (iterator.hasNext()) {
        int id = (Integer)iterator.next();
        final Patient patient = Context.getPatientService().getPatient(id);
        patients.add(patient);
      }
    } else {
      patients = Context.getPatientSetService().getPatients(query.list());
    }

    final long end = System.currentTimeMillis();

    System.out.println("   found " + patients.size() + " results for name " + queryString + " in " + (end - start) + " ms");

  }

  /**
   * Runs family name soundex searches.
   * @throws Exception in case of errors
   */
  @Test
  public void testFamilyNameSoundexQuery() throws Exception {

    // authenticate evaluates the runtime properties junit.username and junit.password if available
    // if not, a dialog for entering the user credentials pops up
    authenticate();

    executeFamilyNameSoundexQuery("Water");
    executeFamilyNameSoundexQuery("Moses");
    executeFamilyNameSoundexQuery("Tampa");
    executeFamilyNameSoundexQuery("Lankis");
    executeFamilyNameSoundexQuery("XXXXXXXX");
    executeFamilyNameSoundexQuery("");
  }

  /**
   * Execute family name soundex query.
   * @param queryString the family name
   */
  private void executeFamilyNameSoundexQuery(String queryString) {
    System.out.println("searching for family name " + queryString);

    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    final String sql = advisor.buildSoundexFamilyNameQueryString(queryString, 1000);

    System.out.println("sql: " + sql);

    final SQLQuery query = getCurrentSession().createSQLQuery(sql);
    final List<Patient> patients = Context.getPatientSetService().getPatients(query.list());

    System.out.println("found " + patients.size() + " results for name " + queryString);
  }

  /**
   * Runs combined given name and family name soundex searches.
   * @throws Exception in case of errors
   */
  @Test
  public void testGivenAndFamilyNameSoundexQuery() throws Exception {

    // authenticate evaluates the runtime properties junit.username and junit.password if available
    // if not, a dialog for entering the user credentials pops up
    authenticate();

    executeGivenAndFamilyNameSoundexQuery("Mary", "Banda");
    executeGivenAndFamilyNameSoundexQuery("Ganizani ", "Kagomo");
    executeGivenAndFamilyNameSoundexQuery("Fatuma", "Bakali");
    executeGivenAndFamilyNameSoundexQuery("Agnes", "Moffat");
    executeGivenAndFamilyNameSoundexQuery("Agnes", "Mo");

  }

  /**
   * Execute search with given name and family name.
   * @param givenNameQueryString the given name
   * @param familyNameQueryString the family name
   */
  private void executeGivenAndFamilyNameSoundexQuery(String givenNameQueryString, String familyNameQueryString) {
    System.out.println("searching for name " + givenNameQueryString + " " + familyNameQueryString);

    final long start = System.currentTimeMillis();

    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    final String sql = advisor.buildSoundexGivenAndFamilyNameQueryString(givenNameQueryString, familyNameQueryString, 5000);

//    System.out.println("sql: " + sql);

    List<Patient> individuallyConvertedPatients = new ArrayList<Patient>();
    final SQLQuery query = getCurrentSession().createSQLQuery(sql);
    final Iterator iterator = query.list().iterator();

    int counter = 0;
    while (iterator.hasNext()) {
      int person_id = (Integer) iterator.next();

      final Patient patient = Context.getPatientService().getPatient(person_id);
      individuallyConvertedPatients.add(patient);
      counter++;
//      printPatient(patient, counter);
    }

    final long individual = System.currentTimeMillis();

    List<Patient> batchConvertedPatients = Context.getPatientSetService().getPatients(query.list());

    final long end = System.currentTimeMillis();

    System.out.println("   indiviual retrieval " + "(" + counter + "): " + (individual - start) + " ms");
    System.out.println("   batch     retrieval " + "(" + counter + "): " + (end - individual  ) + " ms");

    assertEquals(batchConvertedPatients.size(), individuallyConvertedPatients.size());

    for (int i = 0; i < batchConvertedPatients.size(); i++) {
//      assertEquals(batchConvertedPatients.get(i), individuallyConvertedPatients.get(i));
    }

  }

  /**
   * Check hibernate SQL entity query.
   */
  //@Test
  public void testSqlQueryWithDirectHibernateMapping() {

    final String sql = "SELECT * FROM person WHERE person_id >= '16000' AND person_id < '16010';";

//    final SQLQuery query = getCurrentSession().createSQLQuery(sql);
    final SQLQuery query = getCurrentSession().createSQLQuery(sql).addEntity(Person.class);

    List persons = query.list();

    assertTrue(persons.size() > 0);
  }

  /**
   * Print patient information without counter.
   * @param patient the patient object
   */
  private void printPatient(Patient patient) {
    printPatient(patient, 0);
  }

  /**
   * Print patient information to standard out.
   * @param patient the patient object
   * @param n the counter
   */
  private void printPatient(Patient patient, int n) {

    System.out.println((n > 0 ? n + ": " : "") +
            patient.getGivenName() + " "
            + patient.getFamilyName() + " " +
            (patient.getPersonName().getFamilyName2() != null && !patient.getPersonName().getFamilyName2().equalsIgnoreCase("Unknown") ? patient.getPersonName().getFamilyName2() + " " : "")
            + "(" + patient.getPatientId() + ")");
  }

  /**
   * Checks the global properties of the administration service.
   * @throws Exception in case of errors
   */
  @Test
  public void testAdminService() throws Exception {

    // authenticate evaluates the runtime properties junit.username and junit.password if available
    // if not, a dialog for entering the user credentials pops up
    authenticate();

    final AdministrationService adminService = Context.getAdministrationService();

    final String property_name  = "dies.ist.ein.test.property";
    final String property_value = "dies ist ein property value";
    adminService.saveGlobalProperty(new GlobalProperty(property_name, property_value));

    final List<GlobalProperty> globalProperties = adminService.getAllGlobalProperties();

    for (GlobalProperty prop: globalProperties) {
      System.out.println("prop: " + prop.getProperty() + "\t" + prop.getPropertyValue());
    }

    assertNotNull(adminService.getGlobalProperty(property_name));
    assertEquals(adminService.getGlobalProperty(property_name), property_value);
  }

  /**
   * Check handling of runtime properties.
   */
  @Test
  public void testRuntimeProperties() {

    final String property_name  = "dies.ist.ein.test.property";
    final String property_value = "dies ist ein property value";

    addRuntimeProperty(property_name, property_value);

    assertNotNull(Context.getRuntimeProperties().getProperty(property_name));

    Enumeration propertyNames = Context.getRuntimeProperties().propertyNames();
    while (propertyNames.hasMoreElements()) {
      String propertyName = (String) propertyNames.nextElement();
      final String propertyValue = Context.getRuntimeProperties().getProperty(propertyName);

      System.out.println(propertyName + ": " + propertyValue);
    }

  }

  @Test
  public void testSoundexActivatorCodes() {
    PatientServiceAroundAdvisor advisor = new PatientServiceAroundAdvisor();
    PatientServiceAroundAdvisor.SoundexSearchAdvice advice = (PatientServiceAroundAdvisor.SoundexSearchAdvice) advisor.getAdvice();

    assertTrue(advice.isSoundexSearch("s: Mary Banda"));
    assertTrue(advice.isSoundexSearch("s:"));
    assertTrue(advice.isSoundexSearch("soundex:"));
    assertTrue(advice.isSoundexSearch("SOUNDEX:"));
    assertTrue(advice.isSoundexSearch("soundex: John"));
    assertTrue(advice.isSoundexSearch("SOUNDEX: Alina"));

    assertFalse(advice.isSoundexSearch("soundex Alina"));
    assertFalse(advice.isSoundexSearch("s Alina"));
    assertFalse(advice.isSoundexSearch(""));
    assertFalse(advice.isSoundexSearch("Mary"));
    assertFalse(advice.isSoundexSearch(":"));
    assertFalse(advice.isSoundexSearch("sound: Alina"));

    addRuntimeProperty(SoundexRuntimePropertyAccess.SOUNDEX_ACTIVATOR_CODE_ALIAS_TAG, "sound:");

    assertTrue(advice.isSoundexSearch("sound: Alina"));

  }
}

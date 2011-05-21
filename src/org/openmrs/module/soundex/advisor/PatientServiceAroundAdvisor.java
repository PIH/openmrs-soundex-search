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
package org.openmrs.module.soundex.advisor;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.soundex.SoundexRuntimePropertyAccess;
import org.openmrs.module.soundex.encoder.SoundexEncoder;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Advisor implementation for wrapping calls to PatientService.getPatients(String).
 * Subject to some condition a soundex search is executed.
 */
public class PatientServiceAroundAdvisor extends StaticMethodMatcherPointcutAdvisor implements Advisor {

  /** default soundex search activator codes. */
  private static final Collection<String> DEFAULT_SOUNDEX_ACTIVATION_CODES = Arrays.asList("soundex:", "s:");

  /** logger */
  private Log log = LogFactory.getLog(this.getClass());

  /** soundex encoder instance. */
  private static SoundexEncoder soundexEncoder = new SoundexEncoder();

  /**
   * Perform static checking whether the given method matches. If this
   * returns <code>false</code> or if the {@link #isRuntime()} method
   * returns <code>false</code>, no runtime check (i.e. no.
   * {@link #matches(java.lang.reflect.Method, Class, Object[])} call) will be made.
   * @param method the candidate method
   * @param targetClass the target class (may be <code>null</code>, in which case
   * the candidate class must be taken to be the method's declaring class)
   * @return whether or not this method matches statically
   */
  public boolean matches(Method method, Class targetClass) {

    if (method.getName().equals("getPatients") &&
            method.getParameterTypes().length == 1 &&
            method.getParameterTypes()[0] == String.class) {
      return true;
    }
    return false;
  }


  /**
   * Return the soundex search advice implementation.
   * @return advice
   */
  @Override
  public Advice getAdvice() {
    return new SoundexSearchAdvice();
  }

  /**
   * Construct a SQL query for searching on given names with soundex matches.
   * @param name the search string
   * @return SQL query
   */
  public String buildSoundexGivenNameQueryString(String name) {
    return   buildSoundexGivenNameQueryString(name, SoundexRuntimePropertyAccess.getDefaultSqlLimit());
  }

  /**
   * Construct a SQL query for searching on given names with soundex matches including the required ordering.
   * @param name the search string
   * @param limit the limit parameter for the sql query
   * @return SQL query
   */
  public String buildSoundexGivenNameQueryString(String name, int limit) {

    String soundex_code = soundexEncoder.encode(name);
    name = name.replaceAll("'", "\'"); // mask for SQL

    return "SELECT distinct patient.patient_id " +
                    "FROM person_name_code " +
                    "INNER JOIN person_name ON person_name_code.person_name_id = person_name.person_name_id " +
                    "INNER JOIN patient ON patient.patient_id = person_name.person_id " +
                    "WHERE patient.voided = 0 AND person_name.voided = 0 AND given_name_code LIKE '" + soundex_code + "%' " +
              "ORDER BY " +
                 "CASE INSTR(given_name,'" + name + "') WHEN 0 THEN 9999 ELSE INSTR(given_name,'" + name + "') END ASC, " +
                 "CASE INSTR(given_name_code,'" + soundex_code + "') WHEN 0 THEN 9999 ELSE INSTR(given_name_code,'" + soundex_code + "') END ASC, " +
                 "ABS(LENGTH(given_name_code) - LENGTH('" + soundex_code + "')) ASC, " +
                 "given_name ASC, " +
                 "family_name ASC " +
              "LIMIT " + limit + ";";
  }

  /**
   * Construct a SQL query for searching on family names with soundex matches.
   * @param name the search string
   * @return SQL query string
   */
  public String buildSoundexFamilyNameQueryString(String name) {
    return buildSoundexFamilyNameQueryString(name, SoundexRuntimePropertyAccess.getDefaultSqlLimit());
  }

  /**
   * Construct a SQL query for searching on family names with soundex matches including the required ordering.
   * @param name the search string
   * @param limit the limit parameter for the sql query
   * @return SQL query string
   */
  public String buildSoundexFamilyNameQueryString(String name, int limit) {

    String soundex_code = soundexEncoder.encode(name);
    name = name.replaceAll("'", "\'"); // mask for SQL

    return "SELECT distinct patient.patient_id " +
                    "FROM person_name_code " +
                    "INNER JOIN person_name ON person_name_code.person_name_id = person_name.person_name_id " +
                    "INNER JOIN patient ON patient.patient_id = person_name.person_id " +
                    "WHERE patient.voided = 0 AND person_name.voided = 0 AND (family_name_code LIKE '" + soundex_code + "%' OR family_name2_code LIKE '" + soundex_code + "%') " +
              "ORDER BY " +
                 "CASE INSTR(family_name,'" + name + "') WHEN 0 THEN 9999 ELSE INSTR(family_name,'" + name + "') END ASC, " +
                 "CASE INSTR(family_name_code,'" + soundex_code + "') WHEN 0 THEN 9999 ELSE INSTR(family_name_code,'" + soundex_code + "') END ASC, " +
                 "ABS(LENGTH(family_name_code) - LENGTH('" + soundex_code + "')) ASC, " +
                 "family_name ASC, " +
                 "given_name ASC " + 
              "LIMIT " + limit + ";";

  }

  /**
   * Construct the SQL query that retrieves Soundex Matches for the provided given_name and family_name combination.
   * @param given_name the given name of the patient to search
   * @param family_name the family name of the patient to search
   * @return SQL query string
   */
  public String buildSoundexGivenAndFamilyNameQueryString(String given_name, String family_name) {
    return buildSoundexGivenAndFamilyNameQueryString(given_name, family_name, SoundexRuntimePropertyAccess.getDefaultSqlLimit());
  }

  /**
   * Construct the SQL query that retrieves Soundex Matches for the provided given_name and family_name combination.
   * @param given_name the given name of the patient to search
   * @param family_name the family name of the patient to search
   * @return SQL query string
   */
  public String buildSoundexGivenAndFamilyNameQueryString(String given_name, String family_name, int limit) {

    String soundex_code_given_name  = soundexEncoder.encode(given_name);
    String soundex_code_family_name = soundexEncoder.encode(family_name);
    given_name = given_name.replaceAll("'", "\'"); // mask for SQL
    family_name = family_name.replaceAll("'", "\'"); // mask for SQL

    return
      "SELECT distinct patient.patient_id " +
        "FROM person_name_code " +
        "INNER JOIN person_name ON person_name_code.person_name_id = person_name.person_name_id " +
        "INNER JOIN patient ON patient.patient_id = person_name.person_id " +

        "WHERE patient.voided = 0 AND person_name.voided = 0 " +
          "AND (given_name_code LIKE '" + soundex_code_given_name + "%' " +
                "AND (family_name_code LIKE '" + soundex_code_family_name + "%' " +
                      "OR family_name2_code LIKE '" + soundex_code_family_name + "%') " +
          ") " +

        "ORDER BY " +
          "CASE WHEN (INSTR(family_name,'" + family_name + "') > 0 OR INSTR(family_name2,'" + family_name + "') > 0 ) AND INSTR(given_name,'" + given_name + "') > 0 THEN 1 ELSE 2 END ASC, " +
          "CASE WHEN (INSTR(family_name,'" + family_name + "') > 0 OR INSTR(family_name2,'" + family_name + "') > 0 ) AND INSTR(given_name,'" + given_name + "') = 0 THEN 1 ELSE 2 END ASC, " +
          "CASE WHEN (INSTR(family_name,'" + family_name + "') = 0 AND INSTR(family_name2,'" + family_name + "') = 0 ) AND INSTR(given_name,'" + given_name + "') > 0 THEN 1 ELSE 2 END ASC, " +
          "ABS(LENGTH(family_name) - LENGTH('" + soundex_code_family_name + "')) ASC, " +
          "ABS(LENGTH(given_name) - LENGTH('" + soundex_code_given_name + "')) ASC, " +

          "CASE WHEN (INSTR(family_name_code,'" + soundex_code_family_name + "') > 0 OR INSTR(family_name2_code,'" + soundex_code_family_name + "') > 0 ) AND INSTR(given_name_code,'" + soundex_code_given_name + "') > 0 THEN 1 ELSE 2 END ASC, " +
          "CASE WHEN (INSTR(family_name_code,'" + soundex_code_family_name + "') > 0 OR INSTR(family_name2_code,'" + soundex_code_family_name + "') > 0 ) AND INSTR(given_name_code,'" + soundex_code_given_name + "') = 0 THEN 1 ELSE 2 END ASC, " +
          "CASE WHEN (INSTR(family_name_code,'" + soundex_code_family_name + "') = 0 AND INSTR(family_name2_code,'" + soundex_code_family_name + "') = 0 ) AND INSTR(given_name_code,'" + soundex_code_given_name + "') > 0 THEN 1 ELSE 2 END ASC, " +

          "family_name ASC, " +
          "given_name ASC, " +
          "family_name2 ASC " +

        "LIMIT " + limit + ";";

  }

  /**
   * Get the current Hibernate session.
   * @return the current hibernate session
   */
  private static Session getCurrentSession() {
    SessionFactory sf = Context.getRegisteredComponents(SessionFactory.class).get(0);
    return sf.getCurrentSession();
  }

  /**
   * Advice Implementation for PatientService Wrapper (see Spring AOP).
   */
  public class SoundexSearchAdvice implements MethodInterceptor {


    public Object invoke(MethodInvocation invocation) throws Throwable {

      final Object[] arguments = invocation.getArguments();
      final String query = (String) arguments[0];

      List<Patient> results;

      String searchType = "";
      final long start = System.currentTimeMillis();

      if (isSoundexSearch(query)) {
        searchType = "soundex";
        results = executeSoundexSearch(getEffectiveSoundexQuery(query));
      } else {
        searchType = "conventional";
        Object o = invocation.proceed();
        results = (List<Patient>) o;
      }

      final long end = System.currentTimeMillis();
      String time = NumberFormat.getIntegerInstance().format((end-start));
      log.info(searchType + " search found " + results.size() + " records for query '" + query + "' in " + time + " ms.");

      return results;
    }

    /**
     * This method checks, if the query contains a soundex descriminator.
     * @param query the query
     * @return the effective soundex query without the soundex descriminator if a descriminator is found,
     * null otherwise
     */
    private String getEffectiveSoundexQuery(String query) {
      Collection<String> soundexActivationCodes = new ArrayList<String>(DEFAULT_SOUNDEX_ACTIVATION_CODES);
      soundexActivationCodes.add(SoundexRuntimePropertyAccess.getActivatorCodeAlias());
      for (String soundexActivator: soundexActivationCodes) {
        if (query.toUpperCase().startsWith(soundexActivator.toUpperCase())) {
          return query.substring(soundexActivator.length());
        }
      }

      return null;
    }

    /**
     * Determines if the given query is a soundex query or not.
     * @param query the query
     * @return true, if a soundex descriminator is found, false otherwise
     */
    public boolean isSoundexSearch(String query) {
      return getEffectiveSoundexQuery(query) != null;
    }

    /**
     * This method implements the soundex search. If an empty query string is provided, an empty list is returned.
     * If the query is made up of a single string, method executeSingleStringQuery is called.
     * If the query is made up of more than one string, method executeDoubleStringQuery is called.
     * @param query
     * @return
     */
    public List<Patient> executeSoundexSearch(String query) {

        String queryString = query.trim();
        queryString = queryString.replaceAll("  ", " ");
        queryString = queryString.replace(", ", " ");
        String[] names = queryString.split(" ");

        if (names.length == 0) {
          return new ArrayList<Patient>();
        } else if (names.length == 1) {
          return executeSingleStringQuery(names[0]);
        } else {
          return executeDoubleStringQuery(names[0], names[1]);
        }
    }

    /**
     * This method executes two independent searches. The first for matches in the family name soundex codes,
     * the second for matches in the given name soundex codes. The results are mixed up in alternating order.
     * @param name the search string
     * @return list of patient objects that match the criteria
     */
    private List<Patient> executeSingleStringQuery(String name) {

      final String familyNameSql = buildSoundexFamilyNameQueryString(name);
      final SQLQuery familyNameQuery = getCurrentSession().createSQLQuery(familyNameSql);
      final Iterator<Integer> familyNameIterator = familyNameQuery.list().iterator();

      final String givenNameSql = buildSoundexGivenNameQueryString(name);
      final SQLQuery givenNameQuery = getCurrentSession().createSQLQuery(givenNameSql);
      final Iterator<Integer> givenNameIterator = givenNameQuery.list().iterator();

      // mix up results in alternating order
      final int GROUP_SIZE = 5;
      List<Patient> patients = new ArrayList<Patient>();
      for (int i = 0; i < Math.max(familyNameQuery.list().size(), givenNameQuery.list().size()); i++) {

        addNextPatientGroup(familyNameIterator, patients, GROUP_SIZE);

        if (patients.size() == SoundexRuntimePropertyAccess.getDefaultResultLimit()) {
            break;
        }

        addNextPatientGroup(givenNameIterator, patients, GROUP_SIZE);

        if (patients.size() == SoundexRuntimePropertyAccess.getDefaultResultLimit()) {
          break;
        }
      }

      return patients;
    }

    /**
     * Iterate next groupSize results
     * @param iterator the patient id iterator
     * @param patients list of patients (result set)
     * @param groupSize no. of patients to iterate
     */
    private void addNextPatientGroup(Iterator<Integer> iterator, List<Patient> patients, int groupSize) {
      for (int j = 0; j < groupSize; j++) {
        if (iterator.hasNext()) {
          int patientId = iterator.next();
          final Patient patient = Context.getPatientService().getPatient(patientId);
          if (!patients.contains(patient)) {
            patients.add(patient);
          }
        }

        if (patients.size() == SoundexRuntimePropertyAccess.getDefaultResultLimit()) {
          break;
        }
      }
    }

    /**
     * This method executes two independent searches. The first for matches in the family name soundex codes,
     * the second for matches in the given name soundex codes. The results are mixed up in alternating order.
     * @param name the search string
     * @return list of patient objects that match the criteria
     */
    private List<Patient> executeSingleStringQuery_old(String name) {

      final String familyNameSql = buildSoundexFamilyNameQueryString(name);
      final SQLQuery familyNameQuery = getCurrentSession().createSQLQuery(familyNameSql);
      final List<Patient> familyNamePatients = Context.getPatientSetService().getPatients(familyNameQuery.list());

      final String givenNameSql = buildSoundexGivenNameQueryString(name);
      final SQLQuery givenNameQuery = getCurrentSession().createSQLQuery(givenNameSql);
      final List<Patient> givenNamePatients = Context.getPatientSetService().getPatients(givenNameQuery.list());

      // mix up results in alternating order
      List<Patient> patients = new ArrayList<Patient>();
      for (int i = 0; i < Math.max(familyNamePatients.size(), givenNamePatients.size()); i++) {

        if (i < familyNamePatients.size() && !patients.contains(familyNamePatients.get(i))) {
          patients.add(familyNamePatients.get(i));
        }

        if (i < givenNamePatients.size() && !patients.contains(givenNamePatients.get(i))) {
          patients.add(givenNamePatients.get(i));
        }
      }

      return patients;
    }

    /**
     * This method executes a combined query for given_name and family_name soundex codes.
     * @param given_name the given name
     * @param family_name the family name
     * @return List of patients that soundex-match the provided names.
     */
    private List<Patient> executeDoubleStringQuery(String given_name, String family_name) {

      final String sql = buildSoundexGivenAndFamilyNameQueryString(given_name, family_name);
      final SQLQuery query = getCurrentSession().createSQLQuery(sql);
      final Iterator<Integer> iterator = query.list().iterator();

      List<Patient> patients = new ArrayList<Patient>();
      while (iterator.hasNext()) {
        final Patient patient = Context.getPatientService().getPatient(iterator.next());
        patients.add(patient);

        if (patients.size() == SoundexRuntimePropertyAccess.getDefaultResultLimit()) {
          break;
        }
      }

      return patients;
    }

    /**
     * This method executes a combined query for given_name and family_name soundex codes.
     * @param given_name the given name
     * @param family_name the family name
     * @return List of patients that soundex-match the provided names.
     */
    private List<Patient> executeDoubleStringQuery_old(String given_name, String family_name) {

      final String sql = buildSoundexGivenAndFamilyNameQueryString(given_name, family_name);
      final SQLQuery query = getCurrentSession().createSQLQuery(sql);

      List<Patient> patients = Context.getPatientSetService().getPatients(query.list());

      return patients.subList(0, Math.min(patients.size(), SoundexRuntimePropertyAccess.getDefaultResultLimit()));
    }
  }
}

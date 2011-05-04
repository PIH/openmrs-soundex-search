/*
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
import org.openmrs.module.soundex.encoder.SoundexEncoder;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class PatientServiceAroundAdvisor extends StaticMethodMatcherPointcutAdvisor implements Advisor {

  private static final int DEFAUL_RESULT_LIMIT = 9999;
  private Log log = LogFactory.getLog(this.getClass());
  private static SoundexEncoder soundexEncoder = new SoundexEncoder();

  public boolean matches(Method method, Class targetClass) {

    if (method.getName().equals("getPatients") &&
            method.getParameterTypes().length == 1 &&
            method.getParameterTypes()[0] == String.class) {
      return true;
    }
    return false;
  }


  @Override
  public Advice getAdvice() {
    return new SoundexSearchAdvice();
  }

  public String buildSoundexGivenNameQueryString(String name) {
    return   buildSoundexGivenNameQueryString(name, DEFAUL_RESULT_LIMIT);
  }

  public String buildSoundexGivenNameQueryString(String name, int limit) {

    String soundex_code = soundexEncoder.encode(name);

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

  public String buildSoundexFamilyNameQueryString(String name) {
    return buildSoundexFamilyNameQueryString(name, DEFAUL_RESULT_LIMIT);
  }

  public String buildSoundexFamilyNameQueryString(String name, int limit) {

    String soundex_code = soundexEncoder.encode(name);

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

  public String buildSoundexGivenAndFamilyNameQueryString(String given_name, String family_name) {
    return buildSoundexGivenAndFamilyNameQueryString(given_name, family_name, DEFAUL_RESULT_LIMIT);
  }
  public String buildSoundexGivenAndFamilyNameQueryString(String given_name, String family_name, int limit) {

    String soundex_code_given_name  = soundexEncoder.encode(given_name);
    String soundex_code_family_name = soundexEncoder.encode(family_name);

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

  private static Session getCurrentSession() {
    SessionFactory sf = Context.getRegisteredComponents(SessionFactory.class).get(0);
    return sf.getCurrentSession();
  }

  public class SoundexSearchAdvice implements MethodInterceptor {

    public Object invoke(MethodInvocation invocation) throws Throwable {

      final Object[] arguments = invocation.getArguments();
      final String query = (String) arguments[0];

//      Object o = invocation.proceed();
//
//      final List<Patient> patients = (List<Patient>) o;
//      log.info("conventional search found " + patients.size() + " records for query '" + query + "'.");

      final List<Patient> soundexResults = executeSoundexSearch(query);
      log.info("soundex search found " + soundexResults.size() + " records for query '" + query + "'.");

      return soundexResults;
    }

    public List<Patient> executeSoundexSearch(String query) {

      String queryString = query;
      queryString.replaceAll("  ", " ");
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

    private List<Patient> executeDoubleStringQuery(String given_name, String family_name) {

      final String sql = buildSoundexGivenAndFamilyNameQueryString(given_name, family_name);
      final SQLQuery query = getCurrentSession().createSQLQuery(sql);
      final Iterator iterator = query.list().iterator();

      List<Patient> patients = new ArrayList<Patient>();
      while (iterator.hasNext()) {
        int patientId= (Integer) iterator.next();
        final Patient patient = Context.getPatientService().getPatient(patientId);
        patients.add(patient);
      }

      return patients;
    }

    private List<Patient> executeSingleStringQuery(String name) {

      final String familyNameSql = buildSoundexFamilyNameQueryString(name);
      final SQLQuery familyNameQuery = getCurrentSession().createSQLQuery(familyNameSql);
      final Iterator familyNameIterator = familyNameQuery.list().iterator();

      final String givenNameSql = buildSoundexGivenNameQueryString(name);
      final SQLQuery givenNameQuery = getCurrentSession().createSQLQuery(givenNameSql);
      final Iterator givenNameIterator = givenNameQuery.list().iterator();

      // mix up results in alternating order
      List<Patient> patients = new ArrayList<Patient>();
      for (int i = 0; i < Math.max(familyNameQuery.list().size(), givenNameQuery.list().size()); i++) {

        if (familyNameIterator.hasNext()) {
          int patientId= (Integer) familyNameIterator.next();
          final Patient patient = Context.getPatientService().getPatient(patientId);
          if (!patients.contains(patient)) {
            patients.add(patient);
          }
        }

        if (givenNameIterator.hasNext()) {
          int patientId = (Integer)givenNameIterator.next();
          final Patient patient = Context.getPatientService().getPatient(patientId);
          if (!patients.contains(patient)) {
            patients.add(patient);
          }
        }
      }

      return patients;
    }

  }

}

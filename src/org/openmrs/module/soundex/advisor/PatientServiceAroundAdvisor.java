/*
  */
package org.openmrs.module.soundex.advisor;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.HibernatePatientDAO;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PatientServiceAroundAdvisor extends StaticMethodMatcherPointcutAdvisor implements Advisor {

  private Log log = LogFactory.getLog(this.getClass());

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
    log.debug("Getting new around advice");
    return new PrintingAroundAdvice();
  }

  private class PrintingAroundAdvice implements MethodInterceptor {

    public Object invoke(MethodInvocation invocation) throws Throwable {

      final Object[] arguments = invocation.getArguments();
      final String query = (String) arguments[0];

      executeSoundexSearch(query);

      log.debug("Before " + invocation.getMethod().getName() + ".");

      // the proceed() method does not have to be called
      Object o = invocation.proceed();

      log.debug("After " + invocation.getMethod().getName() + ".");

      return o;
    }

    private List<Patient> executeSoundexSearch(String query) {
/*
      String sql = "select pn2.person_id from person_name_code p1, person_name_code p2, person_name pn1, person_name pn2 "
          + " where pn1.person_id <> pn2.person_id and " + nameMatch
          + " and pn1.person_id not in (select user_id from users) and pn2.person_id not in (select user_id from users) "
          + " and pn1.person_name_id=p1.person_name_id and pn2.person_name_id=p2.person_name_id and pn1.person_id= "
          + referencePatient.getId() + ";";
*/

      SessionFactory sf = Context.getRegisteredComponents(SessionFactory.class).get(0);
      return new ArrayList<Patient>();
    }
  }

}

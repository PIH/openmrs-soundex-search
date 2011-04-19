/**
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
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.soundex.web.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.LocationEditor;
import org.openmrs.propertyeditor.PatientEditor;
import org.openmrs.propertyeditor.UserEditor;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * This controller backs the /web/module/basicmoduleForm.jsp page. This controller is tied to that
 * jsp page in the /metadata/moduleApplicationContext.xml file
 */

public class SoundexFormController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());

  User user;
	
	/**
	 * This class returns the form backing object. This can be a string, a boolean, or a normal java
	 * pojo. The type can be set in the /config/moduleApplicationContext.xml file or it can be just
	 * defined by the return type of this method
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
    user = Context.getAuthenticatedUser();
    return user;
	}
	
	/**
	 * Returns any extra data in a key-->value pair kind of way
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.Errors)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request, Object obj, Errors err) throws Exception {
		
		Map<String, Object> model = new HashMap<String, Object>();

    User user = (User)obj;
    final Set<PersonName> names = user.getNames();
    final Integer age = user.getAge();
    final PersonName name = names.iterator().next();

    model.put("firstname", name.getGivenName());
    model.put("lastname", name.getFamilyName());
    model.put("age", age);
    
		return model;
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);
		
		binder.registerCustomEditor(User.class, new UserEditor());
		binder.registerCustomEditor(Location.class, new LocationEditor());
		binder.registerCustomEditor(Patient.class, new PatientEditor());
		binder.registerCustomEditor(java.util.Date.class, new CustomDateEditor(OpenmrsUtil.getDateFormat(), true));
	}
	
	@Override
	protected void onBind(HttpServletRequest request, Object obj, BindException errors) {
    String s = (String)obj;

	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object object,
	                                BindException exceptions) throws Exception {


    final ModelAndView modelAndView = new ModelAndView(getSuccessView());
    modelAndView.addObject("user", object);
    return modelAndView;
  }


//  @Override
//  public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
//    final ModelAndView modelAndView = new ModelAndView(getSuccessView());
//
//    modelAndView.addObject("user", Context.getAuthenticatedUser());
//    return modelAndView;
//
//  }
}

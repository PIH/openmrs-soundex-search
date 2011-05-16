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

package org.openmrs.module.soundex;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.Activator;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class SoundexActivator implements Activator {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * @see org.openmrs.module.Activator#startup()
	 */
	public void startup() {
		log.info("Starting Soundex Module...");
  }
	
	/**
	 * @see org.openmrs.module.Activator#shutdown()
	 */
	public void shutdown() {
		log.info("Shutting down Soundex Module...");
	}

}

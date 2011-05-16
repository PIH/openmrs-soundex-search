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

import org.openmrs.api.context.Context;

/**
 * Helper class with access methods for soundex search specific runtime properties.
 */
public class SoundexRuntimePropertyAccess {

  /** property name for default sql limit. */
  public static final String DEFAULT_SQL_LIMIT_TAG = "soundex.search.sql.limit";
  /** default value for sql limit. */
  public static final String DEFAULT_SQL_LIMIT_VALUE = "100";
  
  /** property name for default result limit */
  public static final String DEFAULT_RESULT_LIMIT_TAG = "soundex.search.result.limit";
  /** default value for result limit */
  public static final String DEFAULT_RESULT_LIMIT_VALUE = "100";

  /** property name for soundex activator alias */
  public static final String SOUNDEX_ACTIVATOR_CODE_ALIAS_TAG = "soundex.search.activatorcode.alias";
  /** default value for soundex activator alias */
  public static final String SOUNDEX_ACTIVATOR_CODE_ALIAS_VALUE = ":s";

  /**
   * Get the default result set limit.
   * @return result set limit
   */
  static public int getDefaultResultLimit() {

    return Integer.valueOf(Context.getRuntimeProperties().getProperty(DEFAULT_RESULT_LIMIT_TAG, DEFAULT_RESULT_LIMIT_VALUE));
  }

  /**
   * Get the default Sql limit.
   * @return sql limit
   */
  static public int getDefaultSqlLimit() {
    return Integer.valueOf(Context.getRuntimeProperties().getProperty(DEFAULT_SQL_LIMIT_TAG, DEFAULT_SQL_LIMIT_VALUE));
  }

  /**
   * Get the soundex activator code alias.
   * @return the soundex activator code alias
   */
  static public String getActivatorCodeAlias() {
    return Context.getRuntimeProperties().getProperty(SOUNDEX_ACTIVATOR_CODE_ALIAS_TAG, SOUNDEX_ACTIVATOR_CODE_ALIAS_VALUE);
  }

}

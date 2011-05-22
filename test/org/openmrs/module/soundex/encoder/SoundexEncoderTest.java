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

import org.apache.commons.codec.EncoderException;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for transient (non-context-sensitive) tests of the SoundexEncoder.
 * Adopted from a ruby implementation in the bart2 project. see
 * https://github.com/BaobabHealthTrust/bart2/blob/bart2/test/unit/bantu_soundex_test.rb
 */
public class SoundexEncoderTest {

  @Test
  public void testSamples() throws EncoderException {
    SoundexEncoder encoder = new SoundexEncoder();

    assertNotNull("be able to convert a word to code", encoder.encode("Rodney"));
    assertEquals("capatalize all of the letters", "X", encoder.encode("x"));

    assertEquals("drop all of the punctuation marks", "K2", encoder.encode("kg'g"));

    assertEquals("convert vowels, 'H', 'W', and 'Y' before it removes double letters", "K22", encoder.encode("kghg"));
    assertEquals("convert vowels, 'H', 'W', and 'Y' before it removes double letters", "K22", encoder.encode("kgwg"));
    assertEquals("convert vowels, 'H', 'W', and 'Y' before it removes double letters", "K22", encoder.encode("kgyg"));

    assertEquals("convert consonants to the correct group", "X4", encoder.encode("XL"));
    assertEquals("convert consonants to the correct group", "X4", encoder.encode("XR"));

    assertEquals("remove double letters", "X4", encoder.encode("XLL"));
    assertEquals("remove double letters", "X4", encoder.encode("XLR"));

    assertEquals("remove vowels", "K", encoder.encode("KAEI"));
    assertEquals("remove vowels", "K", encoder.encode("KOUW"));
    assertEquals("remove vowels", "K", encoder.encode("KHAY"));

    assertEquals("construct the code from the first letter and first three digits", "K84", encoder.encode("KOWALE"));

    assertEquals("maintain the first letter of the word unless it is a 'N', 'M', or 'D' followed by a consonant", "Z574", encoder.encode("DZANJALIMODZI"));
    assertEquals("maintain the first letter of the word unless it is a 'N', 'M', or 'D' followed by a consonant", "Z51", encoder.encode("MZIMBA"));
    assertEquals("maintain the first letter of the word unless it is a 'N', 'M', or 'D' followed by a consonant", "G51", encoder.encode("NGOMBE"));

    assertNull("return nil for blank strings", encoder.encode(""));
    assertNull("return nil for strings with no letters", encoder.encode("1234"));
    assertNull("return nil for strings with no letters", encoder.encode(" "));
    assertNull("return nil for strings with no letters", encoder.encode("-"));

    assertEquals("encode 'W' followed by a vowel as a separate consonant class", "998", encoder.encode("CHICHEWA"));
    assertEquals("encode 'W' followed by a vowel as a separate consonant class", "B84", encoder.encode("BWAIWLA"));

    assertEquals("encode 'CH', 'TCH', and 'THY' as a separate consonant class", "K9", encoder.encode("KCH"));
    assertEquals("encode 'CH', 'TCH', and 'THY' as a separate consonant class", "K9", encoder.encode("KTHY"));
    assertEquals("encode 'CH', 'TCH', and 'THY' as a separate consonant class", "K9", encoder.encode("KTCH"));
    assertEquals("encode 'CH', 'TCH', and 'THY' as a separate consonant class", "94", encoder.encode("THYOLO"));

    assertEquals("encode initial 'C' as a 'K'", "K5", encoder.encode("CAUMA"));

    assertEquals("encode initial 'A' and 'I' as 'E'", "E15", encoder.encode("EVAN"));
    assertEquals("encode initial 'A' and 'I' as 'E'", "E15", encoder.encode("AVAN"));
    assertEquals("encode initial 'A' and 'I' as 'E'", "E15", encoder.encode("IVAN"));
  }
}

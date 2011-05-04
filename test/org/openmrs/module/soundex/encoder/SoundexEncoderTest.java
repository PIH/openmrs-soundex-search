/*
  */
package org.openmrs.module.soundex.encoder;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
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

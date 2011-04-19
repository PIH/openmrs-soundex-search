/*
  */
package org.openmrs.module.soundex.encoder;

/**
 *
 */
public class SoundexEncoder {

  public static String encode(String name) {

    String word = name;

    if (word.isEmpty()) {
      return null;
    }

    // Capitalize all letters in the word
    word = word.toUpperCase();

    // Drop all punctuation marks and numbers and spaces
    word = word.replaceAll("/[^A-Z]/", "");
    
    if (word.isEmpty()) {
      return null;
    }

    // Words starting with M or N or D followed by another consonant should drop the first letter
    word = word.replaceFirst("/^M([BDFGJKLMNPQRSTVXZ])/",  "\1");
    word = word.replaceFirst("/^N([BCDFGJKLMNPQRSTVXZ])/", "\1");
    word = word.replaceFirst("/^D([BCDFGJKLMNPQRSTVXZ])/", "\1");
/*
    # THY and CH as common phonemes enhancement
    word.gsub!(/(THY|CH|TCH)/, '9')
    # Retain the first letter of the word
    initial = word.slice(0..0)
    tail = word.slice(1..word.size)
    # Initial vowel enhancement
    initial.gsub!(/[AEI]/, 'E')
    # Initial C/K enhancement
    initial.gsub!(/[CK]/, 'K')
    initial.gsub!(/[JY]/, 'Y')
    initial.gsub!(/[VF]/, 'F')
    initial.gsub!(/[LR]/, 'R')
    initial.gsub!(/[MN]/, 'N')
    initial.gsub!(/[SZ]/, 'Z')
    # W followed by a vowel should be treated as a consonant enhancement
    tail.gsub!(/W[AEIOUHY]/, '8')
    # Change letters from the following sets into the digit given
    tail.gsub!(/[AEIOUHWY]/, '0')
    tail.gsub!(/[BFPV]/, '1')
    tail.gsub!(/[CGKQX]/, '2')
    tail.gsub!(/[DT]/, '3')
    tail.gsub!(/[LR]/, '4')
    tail.gsub!(/[MN]/, '5')
    tail.gsub!(/[SZ]/, '6') # Originally with CGKQX
    tail.gsub!(/[J]/, '7') # Originally with CGKQX
    # Remove all pairs of digits which occur beside each other from the string
    tail.gsub!(/1+/, '1')
    tail.gsub!(/2+/, '2')
    tail.gsub!(/3+/, '3')
    tail.gsub!(/4+/, '4')
    tail.gsub!(/5+/, '5')
    tail.gsub!(/6+/, '6')
    tail.gsub!(/7+/, '7')
    tail.gsub!(/8+/, '8')
    tail.gsub!(/9+/, '9')
    # Remove all zeros from the string
    tail.gsub!(/0/, '')
    # Return only the first four positions
    initial + tail.slice(0..2)
  end
*/
    return null;
  }
}

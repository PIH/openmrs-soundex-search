/*
  */
package org.openmrs.module.soundex.encoder;

/**
 *
 */

import org.apache.commons.codec.StringEncoder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChichewaSoundexEncoder implements StringEncoder {

    public final static String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public final static String consonants = "BCDFGJKLMNPQRSTVWXZ";
    public final static String vowells = "AEIOUHY";
    public final static Set<String> vowellsSet = new HashSet<String>();
    {
        vowellsSet.add("A");
        vowellsSet.add("E");
        vowellsSet.add("I");
        vowellsSet.add("O");
        vowellsSet.add("U");
        vowellsSet.add("H");
        vowellsSet.add("Y");
    }


    public String removeMNorD(String str){
        String charsToDrop = "MND";
        if (str.length() > 2 && charsToDrop.contains(String.valueOf(str.charAt(0))) && consonants.contains(String.valueOf(str.charAt(1))))
            return str.substring(1, str.length());
        else
            return str;
    }

    public String removeInvalidChars(String str){
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < str.length(); i++){
            if (validChars.contains(String.valueOf(str.charAt(i))))
                    sb.append(str.charAt(i));
        }
        return sb.toString();
    }

    public String replaceCharSequences(String replacement, Set<String> strings, String str){
        for (String strTmp : strings){
           str =  str.replaceAll(strTmp, replacement);
        }
        return str;
    }

    public String letterFollwedByStringSetEnhancement(String replacement, String str, String letter, Set<String> possibleSeconds){
        for (String stSecond : possibleSeconds){
            String searchString = letter+stSecond;
            str = str.replaceAll(searchString, replacement);
        }
        return str;
    }

    private Set<String> buildStringSet(String chars){
        Set<String> ret = new HashSet<String>();
        for (int i = 0; i < chars.length(); i ++){
            ret.add(String.valueOf(chars.charAt(i)));
        }
        return ret;
    }

    public String encode(String str){
//        StringUtils su = new StringUtils();

        if (str == null || str.equals(""))
            return null;
//TODO: //accented e's and i's to their un-accented versions.


//      # Handle blanks
        str = str.toUpperCase();

//      # Drop all punctuation marks and numbers and spaces
        str = removeInvalidChars(str);


//      return nil if word.blank?
        if (str == null || str.equals(""))
            return null;

//      # Words starting with M or N or D followed by another consonant should drop the first letter
//      word.gsub!(/^M([BDFGJKLMNPQRSTVXZ])/, '\1')
//      word.gsub!(/^N([BCDFGJKLMNPQRSTVXZ])/, '\1')
//      word.gsub!(/^D([BCDFGJKLMNPQRSTVXZ])/, '\1')
        str = removeMNorD(str);


//      # THY and CH as common phonemes enhancement
//      word.gsub!(/(THY|CH|TCH)/, '9')
        Set<String> stSet = new HashSet<String>();
        stSet.add("THY");
        stSet.add("CH");
        stSet.add("TCH");
        str = replaceCharSequences("9", stSet, str);



//      # Retain the first letter of the word
//      initial = word.slice(0..0)
        String firstLetter = String.valueOf(str.charAt(0));
//      tail = word.slice(1..word.size)
        str = str.substring(1,str.length());
//      # Initial vowel enhancement
//      initial.gsub!(/[AEI]/, 'E')
        stSet = new HashSet<String>();
        stSet.add("A");
        stSet.add("E");
        stSet.add("I");
        str = replaceCharSequences("E", stSet, str);


//      # Initial C/K enhancement
//      initial.gsub!(/[CK]/, 'K')
//      initial.gsub!(/[JY]/, 'Y')
//      initial.gsub!(/[VF]/, 'F')
//      initial.gsub!(/[LR]/, 'R')
//      initial.gsub!(/[MN]/, 'N')
//      initial.gsub!(/[SZ]/, 'Z')

        str = replaceCharSequences("K", Collections.singleton("C"), str);
        str = replaceCharSequences("J", Collections.singleton("Y"), str);
        str = replaceCharSequences("V", Collections.singleton("F"), str);
        str = replaceCharSequences("L", Collections.singleton("R"), str);
        str = replaceCharSequences("M", Collections.singleton("N"), str);
        str = replaceCharSequences("S", Collections.singleton("Z"), str);

//      # W followed by a vowel should be treated as a consonant enhancement
//      tail.gsub!(/W[AEIOUHY]/, '8')

        str = letterFollwedByStringSetEnhancement("8", str, "W", vowellsSet);

//      # Change letters from the following sets into the digit given
//      tail.gsub!(/[AEIOUHWY]/, '0')
        str = replaceCharSequences("0", vowellsSet, str);
//      tail.gsub!(/[BFPV]/, '1')
        str = replaceCharSequences("1", buildStringSet("BFPV"), str);
//      tail.gsub!(/[CGKQX]/, '2')
        str = replaceCharSequences("2", buildStringSet("CGKQX"), str);
//      tail.gsub!(/[DT]/, '3')
        str = replaceCharSequences("3", buildStringSet("DT"), str);
//      tail.gsub!(/[LR]/, '4')
        str = replaceCharSequences("4", buildStringSet("LR"), str);
//      tail.gsub!(/[MN]/, '5')
        str = replaceCharSequences("5", buildStringSet("MN"), str);
//      tail.gsub!(/[SZ]/, '6') # Originally with CGKQX
        str = replaceCharSequences("6", buildStringSet("SZ"), str);
//      tail.gsub!(/[J]/, '7') # Originally with CGKQX
        str = replaceCharSequences("7", buildStringSet("J"), str);
//      # Remove all pairs of digits which occur beside each other from the string

        str = str.replaceAll("11", "1");
        str = str.replaceAll("22", "2");
        str = str.replaceAll("33", "3");
        str = str.replaceAll("44", "4");
        str = str.replaceAll("55", "5");
        str = str.replaceAll("66", "6");
        str = str.replaceAll("77", "7");
        str = str.replaceAll("88", "8");
        str = str.replaceAll("99", "9");

//      # Remove all zeros from the string
        str = str.replaceAll("0", "");
//      # Return only the first four positions
//      initial + tail.slice(0..2)
     if (str.length() < 3)
         return firstLetter + str;
     else
         return firstLetter + str.substring(0,3);
    }

    public String encode(Object obj){
        try {
            String st = (String) obj;
            return encode(st);
        } catch (Exception ex){
            throw new RuntimeException("Chichewa PhoneticsAlgorithm was passed something that was not a string to encode.");
        }
    }
}
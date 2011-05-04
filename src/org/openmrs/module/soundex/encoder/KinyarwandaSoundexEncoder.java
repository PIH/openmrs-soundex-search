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

public class KinyarwandaSoundexEncoder implements StringEncoder {
    public final static String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public final static String consonants = "BCDFGHJKLMNPQRSTVWXYZ";
    public final static Set<String> vowellsSet = new HashSet<String>();
    {
        vowellsSet.add("A");
        vowellsSet.add("E");
        vowellsSet.add("I");
        vowellsSet.add("O");
        vowellsSet.add("U");
    }

    public String encode(String str){

//      # Handle blanks
        if (str == null || str.equals(""))
            return null;

//      # To upper case
        str = str.toUpperCase();


//      # Drop all punctuation marks and numbers and spaces
        str = removeInvalidChars(str);


//      return nil if word.blank?
        if (str == null || str.equals(""))
            return null;

//      # Words starting with M or N followed by another consonant (excluding Y) should drop the first letter
        str = removeMN(str);


//      # THY and CH as common phonemes enhancement
//      word.gsub!(/(THY|CH|TCH)/, '9')
        Set<String> stSet = new HashSet<String>();
        stSet.add("CY");
        stSet.add("SH");
        stSet.add("SHY");
        str = replaceCharSequences("9", stSet, str);


//      # Retain the first letter of the word
//      initial = word.slice(0..0)
        String firstLetter = String.valueOf(str.charAt(0));


//      tail = word.slice(1..word.size)
        str = str.substring(1,str.length());


//      # Initial vowel enhancement
//        DT:  i don't understand this -- All vowels go to '0' and then are removed anyway...
//        initial.gsub!(/[AEI]/, 'E')
//        stSet = new HashSet<String>();
//        stSet.add("A");
//        stSet.add("E");
//        stSet.add("I");
//        str = replaceCharSequences("E", stSet, str);


//      # Initial C/K enhancement
//      initial.gsub!(/[CK]/, 'K')
//      initial.gsub!(/[JY]/, 'Y')
//      initial.gsub!(/[VF]/, 'F')
//      initial.gsub!(/[LR]/, 'R')
//      initial.gsub!(/[SZ]/, 'Z')

        str = replaceCharSequences("K", Collections.singleton("C"), str);
        str = replaceCharSequences("G", Collections.singleton("JY"), str);
        str = replaceCharSequences("V", Collections.singleton("F"), str);
        str = replaceCharSequences("R", Collections.singleton("L"), str);
        str = replaceCharSequences("S", Collections.singleton("Z"), str);

//      # W followed by a vowel should be treated as a consonant enhancement
//      tail.gsub!(/W[AEIOUHY]/, '8')

        str = letterFollwedByStringSetEnhancement("8", str, "W", vowellsSet);
        str = letterFollwedByStringSetEnhancement("8", str, "Y", vowellsSet);

//      # Change letters from the following sets into the digit given
//      tail.gsub!(/[AEIOUHWY]/, '0')
        str = replaceCharSequences("0", vowellsSet, str);
//      tail.gsub!(/[BFPV]/, '1')
        str = replaceCharSequences("1", buildStringSet("BPV"), str);
//      tail.gsub!(/[CGKQX]/, '2')
        str = replaceCharSequences("2", buildStringSet("GKQX"), str);
//      tail.gsub!(/[DT]/, '3')
        str = replaceCharSequences("3", buildStringSet("DT"), str);
//      tail.gsub!(/[LR]/, '4')
        str = replaceCharSequences("4", buildStringSet("LR"), str);
//      tail.gsub!(/[MN]/, '5')
        str = replaceCharSequences("5", buildStringSet("MN"), str);
//      tail.gsub!(/[SZ]/, '6') # Originally with CGKQX
        str = replaceCharSequences("6", buildStringSet("SZ"), str);
//      tail.gsub!(/[J]/, '7') # Originally with CGKQX
        str = replaceCharSequences("7", buildStringSet("JHYW"), str);
//      # Remove all pairs of digits which occur beside each other from the string

        while (str.contains("11"))
            str = str.replaceAll("11", "1");
        while (str.contains("22"))
            str = str.replaceAll("22", "2");
        while (str.contains("33"))
            str = str.replaceAll("33", "3");
        while (str.contains("44"))
            str = str.replaceAll("44", "4");
        while (str.contains("55"))
            str = str.replaceAll("55", "5");
        while (str.contains("66"))
            str = str.replaceAll("66", "6");
        while (str.contains("77"))
            str = str.replaceAll("77", "7");
        while (str.contains("88"))
            str = str.replaceAll("88", "8");
        while (str.contains("99"))
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
            throw new RuntimeException("RwandaPhoneticsAlgorithm was passed something that was not a string to encode.");
        }
    }

    private String removeMN(String str){
        String charsToDrop = "MN";
        if (str.length() > 2 && charsToDrop.contains(String.valueOf(str.charAt(0))) && consonants.contains(String.valueOf(str.charAt(1)))
                && !String.valueOf(str.charAt(1)).equals("Y"))
            return str.substring(1, str.length());
        else
            return str;
    }

    private String removeInvalidChars(String str){
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < str.length(); i++){
            if (validChars.contains(String.valueOf(str.charAt(i))))
                    sb.append(str.charAt(i));
        }
        return sb.toString();
    }

    private String replaceCharSequences(String replacement, Set<String> strings, String str){
        for (String strTmp : strings){
           str =  str.replaceAll(strTmp, replacement);
        }
        return str;
    }

    private String letterFollwedByStringSetEnhancement(String replacement, String str, String letter, Set<String> possibleSeconds){
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
}
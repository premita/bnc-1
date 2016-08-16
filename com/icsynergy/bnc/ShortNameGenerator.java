package com.icsynergy.bnc;

import java.text.Normalizer;
import java.util.logging.Logger;

public class ShortNameGenerator {
    private static Logger log = Logger.getLogger("com.icsynergy");

    public static String getNext(String strFirst, String strLast, int number) throws Exception {
        log.entering("ShortNameGenerator" ,"getNext");

        if (number > 999 || number < 0)
            throw new IllegalArgumentException("number should be in 1..999");

        if (strFirst == null || strLast == null || strFirst.isEmpty() || strLast.isEmpty())
            throw new IllegalArgumentException("name should not be null or empty");

        String strNormF = Normalizer.normalize(strFirst, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]","").trim();
        String strNormL = Normalizer.normalize(strLast, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]","").trim();
        String woNumber = strNormL.substring(0, strNormL.length() < 3 ? strNormL.length() : 3) + strNormF.charAt(0);

        String strFormat = "%0" + (7 - woNumber.length()) + "d";
        String res = (woNumber + String.format(strFormat, number)).toLowerCase();

        log.exiting("ShortNameGenerator", "getNext", res);
        return res;
    }
}

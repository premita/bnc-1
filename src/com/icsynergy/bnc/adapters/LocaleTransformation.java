package com.icsynergy.bnc.adapters;

import com.icsynergy.bnc.Constants;

import java.util.HashMap;
import java.util.logging.Logger;

public class LocaleTransformation {
    final private Logger log = Logger.getLogger("com.icsynergy");

    public Object transform(HashMap hmUserDetails, HashMap hmEntitlementDetails, String sField) {
        log.entering(getClass().getName(), "transform");

        log.finer("sField=" + sField);
        log.finer("hmUserDetails=" + hmUserDetails.toString());

        String strRet;

        if (hmUserDetails.containsKey(Constants.CoucheAttributes.PrefLang)) {
            log.finest("Couche Preferred Language transformation");
            strRet = String.valueOf(hmUserDetails.get(Constants.CoucheAttributes.PrefLang));
        } else {
            log.finest("UMS User language transformation");
            strRet = String.valueOf(hmUserDetails.get(Constants.UmsAttributes.UserLang));
        }
        log.finer("value to transform: " + strRet);

        strRet = decodeLocale(strRet);

        log.exiting(getClass().getName(), "transform", strRet);
        return strRet;
    }

    private String decodeLocale(String strLang) {
        log.entering(getClass().getName(), "decodeLocale", strLang);

        String strRet;
        switch (strLang.toLowerCase()) {
            case "fr" : strRet = "fr-CA";
                break;
            default: strRet = "en-CA";
        }

        log.exiting(getClass().getName(), "decodeLocale", strRet);
        return strRet;
    }
}

package com.icsynergy.bnc.adapters;

import com.icsynergy.bnc.Constants;

import java.util.HashMap;
import java.util.logging.Logger;

public class NameTrimmer {
    final private Logger log = Logger.getLogger("com.icsynergy");

    public Object transform(HashMap hmUserDetails, HashMap hmEntitlementDetails, String sField) {
        log.entering(getClass().getName(), "transform");

        log.finer("sField=" + sField);
        log.finer("hmUserDetails=" + hmUserDetails.toString());
        switch (sField) {
            case Constants.CoucheAttributes.FirstNameUsed :
                return capitalizeString(String.valueOf(hmUserDetails.get("First Name")));

            case Constants.CoucheAttributes.LastNameUsed :
                return capitalizeString(String.valueOf(hmUserDetails.get("Last Name")));

            default:
                return "";
        }
    }

    private String capitalizeString(String string) {
        log.entering(getClass().getName(), "capitalizeString", string);

        if (string == null || string.isEmpty())
            return "";

        char[] chars = string.trim().toLowerCase().toCharArray();

        boolean found = false;

        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i]) || chars[i]=='-' || chars[i]=='\'') {
                found = false;
            }
        }

        log.exiting(getClass().getName(), "capitalizeString", String.valueOf(chars));
        return String.valueOf(chars);
    }
}


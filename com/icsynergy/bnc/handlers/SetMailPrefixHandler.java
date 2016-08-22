package com.icsynergy.bnc.handlers;

import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import javax.naming.*;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.icsynergy.bnc.Constants.UserAttributes.MailUniquePrefix;

public class SetMailPrefixHandler implements PostProcessHandler {
    final private Logger log = Logger.getLogger("com.icsynergy");
    final private UserManager um =
            Platform.getServiceForEventHandlers(
                    UserManager.class, null, null, "SetMailPrefixHandler", null);

    private DirContext ctxLbg, ctxRes;


    // mail domains used by BNC
    private final String[] domains = {
            "nbc.ca", "bnc.ca", "innocap.com", "innocapblogal.com", "fbn.ca", "nbcai.ca",
            "nbcgf.ie", "nbf.ca", "nbfg.ca", "nbf-us.com", "nboc.com", "perseuscapital.ca"
    };

    @Override
    public EventResult execute(long l, long l1, Orchestration orchestration) {
        log.entering(getClass().getName(), "execute");

        Context ictx = null;
        try {
            log.finest("getting an initial context...");
            ictx = new InitialContext();

            log.finest("looking up LBG");
            ctxLbg = (DirContext) ictx.lookup("jndiLBG");
            log.finest("looking up RES");
            ctxRes = (DirContext) ictx.lookup("jndiRES");

            processUser(
                    orchestration.getTarget().getEntityId(),
                    String.valueOf(orchestration.getParameters().get(UserManagerConstants.AttributeName.FIRSTNAME.getId())),
                    String.valueOf(orchestration.getParameters().get(UserManagerConstants.AttributeName.LASTNAME.getId()))
                    );
        } catch (NamingException e) {
            log.log(Level.SEVERE, "Exception working with contexts", e);
            throw new EventFailedException("Exception working with contexts", e);
        } finally {
            try {
                ctxLbg.close();
                ctxRes.close();

                if (ictx != null) {
                    ictx.close();
                }
            } catch (NamingException e) {
                log.log(Level.SEVERE, "Exception closing contexts", e);
            }
        }

        log.exiting(getClass().getName(), "execute");
        return new EventResult();
    }

    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        log.entering(getClass().getName(), "bulk execute");

        Context ictx = null;
        try {
            log.finest("getting an initial context...");
            ictx = new InitialContext();

            log.finest("looking up LBG");
            ctxLbg = (DirContext) ictx.lookup("jndiLBG");
            log.finest("looking up RES");
            ctxRes = (DirContext) ictx.lookup("jndiRES");

            HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
            final java.lang.String[] allEntityId = bulkOrchestration.getTarget().getAllEntityId();

            log.finest("processing every user in the orchestration");
            for (int i = 0; i < allEntityId.length; i++) {
                processUser(
                        allEntityId[i],
                        String.valueOf(bulkParameters[i].get(UserManagerConstants.AttributeName.FIRSTNAME.getId())),
                        String.valueOf(bulkParameters[i].get(UserManagerConstants.AttributeName.LASTNAME.getId()))
                        );
            }
        } catch (NamingException e) {
            log.log(Level.SEVERE, "Exception working with contexts", e);
            throw new EventFailedException("Exception working with contexts", e);
        } finally {
            try {
                ctxLbg.close();
                ctxRes.close();

                if (ictx != null) {
                    ictx.close();
                }
            } catch (NamingException e) {
                log.log(Level.SEVERE, "Exception closing contexts", e);
            }
        }
        log.exiting(getClass().getName(), "bulk execute");
        return new BulkEventResult();
    }

    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {
        return false;
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {

    }

    @Override
    public void initialize(HashMap<String, String> hashMap) {
    }

    private boolean isInLBG(String strName) throws NamingException {
        log.entering(getClass().getName(), "isInLBG", strName);

        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        ctls.setReturningAttributes(new String[]{"dn"});

        String[] matchAttributes = {"mail", "proxyAddresses"};
        for (String matchAttribute : matchAttributes) {

            log.finest(String.format("preparing %s search filter", matchAttribute));
            StringBuilder stringBuilder = new StringBuilder("(|");
            for (String domain : domains) {
                stringBuilder.append(String.format("(%s=%s@%s)", matchAttribute, strName, domain));
            }
            stringBuilder.append(")");
            log.finer("filter=" + stringBuilder.toString());

            log.finest("running ldap search");
            NamingEnumeration<SearchResult> answer = null;
            try {
                answer = ctxLbg.search("", stringBuilder.toString(), ctls);
            } catch (PartialResultException ignore) {
            } catch (NamingException e) {
                log.log(Level.SEVERE, "Exception running LDAP search", e);
                throw e;
            }

            try {
                if (answer != null && answer.hasMore()) {
                    log.fine(String.format("%s found in RES", matchAttribute));
                    log.exiting(getClass().getName(), "isInRES", true);
                    return true;
                }
            } catch (PartialResultException ignore) {
            } finally {
                if (answer != null) {
                    answer.close();
                }
            }
        }

        log.exiting(getClass().getName(), "isInLBG", false);
        return false;
    }

    private boolean isInRES(String strName) throws NamingException {
        log.entering(getClass().getName(), "isInRES", strName);

        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        ctls.setReturningAttributes(new String[]{"dn"});

        String[] matchAttributes = {"userPrincipalName", "msRTCSIP-PrimaryUserAddress"};
        for (String matchAttribute : matchAttributes) {

            log.finest(String.format("preparing %s search filter", matchAttribute));
            StringBuilder stringBuilder = new StringBuilder("(|");
            for (String domain : domains) {
                stringBuilder.append(String.format("(%s=%s@%s)", matchAttribute, strName, domain));
            }
            stringBuilder.append(")");
            log.finer("filter=" + stringBuilder.toString());

            log.finest("running ldap search");
            NamingEnumeration<SearchResult> answer = null;
            try {
                answer = ctxLbg.search("", stringBuilder.toString(), ctls);
            } catch (PartialResultException ignore) {
            } catch (NamingException e) {
                log.log(Level.SEVERE, "Exception running LDAP search", e);
                throw e;
            }

            try {
                if (answer != null && answer.hasMore()) {
                    log.fine(String.format("%s found in RES", matchAttribute));
                    log.exiting(getClass().getName(), "isInRES", true);
                    return true;
                }
            } catch (PartialResultException ignore) {
            } finally {
                if (answer != null) {
                    answer.close();
                }
            }
        }

        log.exiting(getClass().getName(), "isInRES", false);
        return false;
    }

    private void processUser(String strUserKey, String strFirstName, String strLastName) {
        log.entering(getClass().getName(), "processUser");

        final String strDedupChars = "BCDFGHJKLMNPRSTUVWZXQOYIEA";

        // normalize names and remove non-leters and "-"
        String strFName =
                Normalizer.normalize(
                        String.valueOf(strFirstName),
                        Normalizer.Form.NFD)
                        .trim()
                        .replaceAll("[^a-zA-Z-]", "");

        String strLName =
                Normalizer.normalize(
                        String.valueOf(strLastName),
                        Normalizer.Form.NFD)
                        .trim()
                        .replaceAll("[^a-zA-Z-]", "");
        log.log(Level.FINER, "names after normalization: {0} and {1}", new String[]{strFName, strLName});

        String strPrefix;
        boolean bTaken;
        int i = -1;

        do {
            strPrefix = i++ < 0
                    ? String.format("%s.%s", strFName, strLName)
                    : String.format("%s%s.%s", strFName, strDedupChars.charAt(i++), strLName);
            log.finer("strPrefix=" + strPrefix);

            // true if any of searches return true
            // size of user list > 0 or is in ADs
            log.finest("checking strPrefix against OIM and ADs");
            try {
                bTaken =
                        um.search(
                                new SearchCriteria(
                                        MailUniquePrefix, strPrefix.toLowerCase(), SearchCriteria.Operator.EQUAL),
                                Collections.singleton(UserManagerConstants.AttributeName.USER_LOGIN.getId()),
                                null)
                                .size() > 0
                                || isInLBG(strPrefix)
                                || isInRES(strPrefix);
            } catch (UserSearchException | NamingException e1) {
                log.log(Level.SEVERE, "Exception searching for checking user info in OIM and AD");
                throw new EventFailedException("Exception searching for user info in OIM and AD", e1);
            }
        } while (bTaken && i < strDedupChars.length());

        // if still taken
        if (bTaken)
            throw new EventFailedException("Deduplication character limit reached to generate a unique BNC Mail Prefix");

        // otherwise save
        User u = new User(strUserKey);
        u.setAttribute(MailUniquePrefix, strPrefix);

        log.log(Level.FINEST, "saving {0} to {1}", new String[]{strPrefix, MailUniquePrefix});
        try {
            um.modify(u);
        } catch (ValidationFailedException | NoSuchUserException | UserModifyException e1) {
            log.log(Level.SEVERE, "Exception modifying the user", e1);
            throw new EventFailedException("Exception modifying the user", e1);
        }
    }
}

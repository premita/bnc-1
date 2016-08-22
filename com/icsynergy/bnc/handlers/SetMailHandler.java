package com.icsynergy.bnc.handlers;

import com.icsynergy.bnc.Constants;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.ConditionalEventHandler;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

public class SetMailHandler implements PostProcessHandler, ConditionalEventHandler{
    private final Logger log = Logger.getLogger("com.icsynergy");

    @Override
    public EventResult execute(long l, long l1, Orchestration orchestration) {
        log.entering(getClass().getName(), "execute");

        UserManager um = Platform.getServiceForEventHandlers(UserManager.class, "SetMailHandler", null, null, null);

        final String strMailPrefix = String.valueOf(orchestration.getParameters().get(Constants.UserAttributes.MailUniquePrefix));
        log.finer("strMailPrefix=" + strMailPrefix);

        // array of mail domains for different languages
        String[] arEmails = new String[] {
            strMailPrefix + "@nbc.ca", strMailPrefix + "@bnc.ca"
        };
        log.finer("emails=" + Arrays.asList(arEmails).toString());

        try {
            log.finest("getting user locale");
            User u =
                    um.getDetails(
                            orchestration.getTarget().getEntityId(),
                            Collections.singleton(UserManagerConstants.AttributeName.LOCALE.getId()),
                            false);

            // email address based on locale
            String strEmailAddr =
                    String.valueOf(u.getAttribute(UserManagerConstants.AttributeName.LOCALE.getId()))
                            .toLowerCase().startsWith("en")
                            ? arEmails[0]
                            : arEmails[1];
            log.finer("email_addr=" + strEmailAddr);

            u = new User(orchestration.getTarget().getEntityId());
            u.setAttribute(UserManagerConstants.AttributeName.EMAIL.getId(), strEmailAddr);

            final String strEmailList = Arrays.asList(arEmails).toString().replaceAll("[\\[\\]]", "");
            u.setAttribute(Constants.UserAttributes.ManagedEmails, strEmailList);

            log.finest(String.format("setting user email=%s and managed mail list=%s", strEmailAddr, strEmailList));
            um.modify(u);
        } catch (NoSuchUserException | UserLookupException | ValidationFailedException | UserModifyException e) {
            throw new EventFailedException(e.getErrorMessage(), e);
        }

        log.exiting(getClass().getName(), "execute");
        return new EventResult();
    }

    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        return null;
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

    @Override
    public boolean isApplicable(AbstractGenericOrchestration abstractGenericOrchestration) {
        log.entering(getClass().getName(), "isApplicable");

        log.finest("checking if orch contains Mail Prefix");
        boolean bRet =
                abstractGenericOrchestration
                        .getParameters()
                        .containsKey(Constants.UserAttributes.MailUniquePrefix);
        log.exiting(getClass().getName(), "isApplicable", bRet);
        return bRet;
    }
}

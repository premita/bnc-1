package com.icsynergy.bnc.handlers;

import com.icsynergy.bnc.Constants;
import com.icsynergy.bnc.ShortNameGenerator;
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
import oracle.iam.identity.vo.Identity;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SetShortNameHandler implements PostProcessHandler{

    private Logger log = Logger.getLogger("com.icsynergy");
    private UserManager um = Platform.getService(UserManager.class);

    @Override
    public EventResult execute(long l, long l1, Orchestration orchestration) {
        log.entering(getClass().getName(),"execute");

        String firstName =
                String.valueOf(
                        orchestration.getParameters().get(UserManagerConstants.AttributeName.FIRSTNAME.getId()));
        String lastName =
                String.valueOf(
                        orchestration.getParameters().get(UserManagerConstants.AttributeName.LASTNAME.getId()));

        setShortName(orchestration.getTarget().getEntityId(), firstName, lastName);

        log.exiting(getClass().getName(), "execute");
        return new EventResult();
    }

    /**
     * Assign short name to OIM user login
     * @param strUserKey - user key
     * @param firstName - first name
     * @param lastName - last name
     */
    private void setShortName(String strUserKey, String firstName, String lastName) {
        log.entering(getClass().getName(), "setShortName");
        int iNum = 1;
        boolean bTaken;
        String strShortName;

        do {
            try {
                strShortName = ShortNameGenerator.getNext(firstName, lastName, iNum);
                log.finer("Short name: " + strShortName);

                SearchCriteria crit =
                        new SearchCriteria(
                                UserManagerConstants.AttributeName.USER_LOGIN.getId(),
                                strShortName,
                                SearchCriteria.Operator.EQUAL);

                // if list size > 0 -> found
                log.finest("checking if short name is taken...");
                bTaken =
                        um.search(
                                crit,
                                Collections.singleton(UserManagerConstants.AttributeName.USER_LOGIN.getId()),
                                null).size() > 0;

                if (iNum++ == 999)
                    throw new RuntimeException("ShortName id generation limit reached");
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception generating short name", e);
                throw new EventFailedException("Exception generating short name");
            }
        } while (bTaken);

        User usr = new User(strUserKey);

        log.finest("Setting usr_login=" + strShortName + " for usr_key=" + usr.getEntityId());
        usr.setAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId(), strShortName);
        try {
            um.modify(usr);
            log.fine("ShortName assigned");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception modifying user", e);
            throw new EventFailedException("Can't assign short name to the usr_key=" + strUserKey);
        }

        log.exiting(getClass().getName(), "setShortName");
    }

    @Override
    public BulkEventResult execute(long processId, long l1, BulkOrchestration bulkOrchestration) {
        log.entering(getClass().getName(), "bulk execute");

        HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
        String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();

        for (int i = 0; i < entityIds.length; i++) {
            setShortName(
                    entityIds[i],
                    String.valueOf(bulkParameters[i].get(UserManagerConstants.AttributeName.FIRSTNAME.getId())),
                    String.valueOf(bulkParameters[i].get(UserManagerConstants.AttributeName.LASTNAME.getId()))
                    );
        }

        log.exiting(getClass().getName(), "bulk execute");
        return new BulkEventResult();
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {

    }

    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration abstractGenericOrchestration) {
        return false;
    }

    @Override
    public void initialize(HashMap<String, String> hashMap) {

    }
}

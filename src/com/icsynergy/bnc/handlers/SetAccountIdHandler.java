package com.icsynergy.bnc.handlers;

import com.icsynergy.bnc.AccountIdGenerator;
import com.icsynergy.bnc.Constants;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.platform.kernel.Event;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SetAccountIdHandler implements PostProcessHandler{
    private Logger log = Logger.getLogger("com.icsynergy");

    @Override
    public EventResult execute(long l, long l1, Orchestration orchestration) {
        log.entering(getClass().getName(),"execute");
        setAccountId(orchestration.getTarget().getEntityId());

        log.exiting(getClass().getName(), "execute");
        return new EventResult();
    }

    private void setAccountId(String strUserKey) {
        log.entering(getClass().getName(),"setAccountId");

        boolean bTaken;
        String strAccId;

        final UserManager um =
                Platform.getServiceForEventHandlers(
                        UserManager.class,
                        "", "RECON", "", null);

        do {
            strAccId = AccountIdGenerator.get();
            log.finer("Generated account id: " + strAccId);

            SearchCriteria crit =
                    new SearchCriteria(Constants.UserAttributes.AccountId, strAccId, SearchCriteria.Operator.EQUAL);
            try {
                log.finest("Searchig for a user with the same account id");
                bTaken =
                        um.search(
                                crit,
                                Collections
                                        .singleton(UserManagerConstants.AttributeName.USER_LOGIN.getId()),
                                null).size() > 0;
                log.finer("found=" + bTaken);
            } catch (UserSearchException e) {
                log.log(Level.SEVERE, "Exception searching for a user", e);
                throw new EventFailedException("ICS-0001 Event failed", null, e);
            }
        } while (bTaken);

        User usr = new User(strUserKey);
        log.finest("Setting AccountId=" + strAccId + " for usr_key=" + usr.getEntityId());
        usr.setAttribute(Constants.UserAttributes.AccountId, strAccId);
        try {
            um.modify(usr);
            log.fine("User modified");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception modifying user", e);
            throw new EventFailedException("Exception modifying user", e);
        }
    }

    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bulkOrchestration) {
        Logger log = Logger.getLogger("com.icsynergy");
        log.entering(getClass().getName(), "bulk execute");

        log.finest("getting array of entities");
        final String[] allEntityId = bulkOrchestration.getTarget().getAllEntityId();

        log.finest("iterating over the array");
        for (String entityId : allEntityId)
            setAccountId(entityId);

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


package com.icsynergy.bnc.tasks;

import java.util.*;
import java.util.logging.Logger;

import com.icsynergy.bnc.Constants;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;

import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.scheduler.vo.TaskSupport;

public class SetUsersManagerTask extends TaskSupport {

    // get interface
    private final UserManager usrmgr = Platform.getService( UserManager.class );
    private final Logger log = Logger.getLogger("com.icsynergy");

    @Override
    public void execute(HashMap hashMap) throws Exception {
        log.entering(getClass().getName(), "execute");

        SearchCriteria searchCriteria =
                new SearchCriteria(Constants.UserAttributes.EMP_MNGR, null, SearchCriteria.Operator.NOT_EQUAL);

        // set of attributes to return
        Set<String> retAttr = new HashSet<>();
        retAttr.add(UserManagerConstants.AttributeName.USER_LOGIN.getName());
        retAttr.add(Constants.UserAttributes.EMP_MNGR);
        retAttr.add(UserManagerConstants.AttributeName.MANAGER_KEY.getId());

        // get user list who meet criteria
        log.finest("searching for users with Employee Manager not null");
        List<User> usersInOIM = usrmgr.search(searchCriteria, retAttr, null);
        log.fine("Users in OIM with Employee Manager not null are [ " + usersInOIM +"]");

        // If no users to process; exit
        if (usersInOIM.isEmpty()) {
            log.fine("No users found");
            log.exiting(getClass().getName(),"execute");
            return;
        }

        log.finest("processing users...");
        for (User usr : usersInOIM){
            String oimManagerID = null;
            log.fine("User: " + usr.getId() + " " + usr.getLogin());

            log.finest("Getting Employee Manager");
            String empManager = String.valueOf(usr.getAttribute(Constants.UserAttributes.EMP_MNGR));
            log.fine("Employee Manager in HR: " + empManager);

            log.finest("Getting manager usr_key from OIM");
            String oimManagerkey =
                    String.valueOf(usr.getAttribute(UserManagerConstants.AttributeName.MANAGER_KEY.getId()));
            log.fine("User Manager Key in OIM: " + oimManagerkey);

            if (!isNullOrEmpty(oimManagerkey)) {
                log.finest("Searching OIM for manager login");
                User user = usrmgr.getDetails(
                        oimManagerkey,
                        Collections.singleton(UserManagerConstants.AttributeName.USER_LOGIN.getId()),
                        false);

                oimManagerID = user.getLogin();
                log.fine("User Manager ID : " + oimManagerID);
            }

            if (!isNullOrEmpty(empManager)) {
                if (empManager.equalsIgnoreCase(oimManagerID)) {
                    log.fine("Manager in OIM [" + oimManagerID +
                            "] is the same as Employee Manager in HR [" + empManager + "]");
                }
                else {
                    log.fine("Manager in OIM [" + oimManagerID +
                            "] is not the same as Employee Manager in HR [" + empManager + "], updating OIM manager");
                    processUserManagerData(usr.getId(), empManager);
                }
            }
        }
    }

    /**
     * This method assign the manager to user as Employee Manager.
     * @param keyUserUD usr_key to set manager for
     * @param empManager manager login to assign as manager
     * @throws NoSuchUserException OIM Exception
     * @throws UserModifyException OIM Exception
     * @throws ValidationFailedException OIM Exception
     */
    private void processUserManagerData(String keyUserUD, String empManager)
            throws UserLookupException, NoSuchUserException, UserModifyException, ValidationFailedException {
        log.entering(getClass().getName(), "processUserManagerData");

        log.finest("getting manager's details");
        User u = null;
        try {
            u = this.usrmgr.getDetails(
                    empManager,
                    new HashSet<>(Arrays.asList(
                            UserManagerConstants.AttributeName.USER_LOGIN.getId(),
                            UserManagerConstants.AttributeName.STATUS.getId())),
                    true);

            if (u.getStatus().equals(UserManagerConstants.AttributeValues.USER_STATUS_ACTIVE.getId())) {
                modifyUser(keyUserUD, u.getEntityId());
            } else
                log.fine("User  [" + u.getLogin() + "] is Disabled in OIM");
        } catch (NoSuchUserException e) {
            log.warning("Manager with login " + empManager + " not found in OIM");
        }

        log.exiting(getClass().getName(),"processUserManagerData");
    }

    /**
     * Method to set a manager for a user
     * @param keyUserUD usr_key of a user to modify
     * @param mgr_key usr_key of a manager to set as a manager for the user
     * @throws NoSuchUserException OIM Exception
     * @throws UserModifyException OIM Exception
     * @throws ValidationFailedException OIM Exception
     */
    private void modifyUser(String keyUserUD, String mgr_key)
            throws NoSuchUserException, UserModifyException, ValidationFailedException {
        log.entering(getClass().getName(), "modifyUser");

        User userModify = new User(keyUserUD);
        userModify.setManagerKey(Long.valueOf(mgr_key));
        userModify.setAttribute(Constants.UserAttributes.EMP_MNGR, null);

        log.finest("setting manager for usr_key=" + keyUserUD + " to " + mgr_key);
        usrmgr.modify(userModify);

        log.exiting(getClass().getName(), "modifyUser");
    }


    /**
     * This method checks is given string is null or empty.
     * @param strCheck String to check
     * @return true if string is null or zero length
     */
    private boolean isNullOrEmpty(String strCheck)
    {
        return (strCheck == null) || strCheck.equals("null") || strCheck.trim().length() == 0;
    }

    @Override
    public HashMap getAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAttributes() {
        // TODO Auto-generated method stub
    }

}

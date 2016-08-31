package com.icsynergy.bnc.handlers;

import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.vo.Identity;
import oracle.iam.platform.Platform;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.*;

import javax.naming.*;
import javax.sql.DataSource;

import Thor.API.Operations.tcLookupOperationsIntf;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChangeUserTypeInUMSHandler implements PostProcessHandler {
	final private Logger log = Logger.getLogger("com.icsynergy");
	private static final String UMSJNDINAME = "UMS JNDI NAME";
	private static final String CONFIGLOOKUP = "UMS.Constant.Configuration";
	private static final String UPDATE_QUERY = "updateQuery";
	public static final String HR_PENDING = "HR Pending";
	public static final String EMP = "EMP";
	public static final String NEW_USER_STATE = "NEW_USER_STATE";
	public static final String CURRENT_USER = "CURRENT_USER";
	private final tcLookupOperationsIntf lookupOperationsIntf = Platform.getService(tcLookupOperationsIntf.class);

	@Override
	public EventResult execute(long arg0, long arg1, Orchestration orchestration) {
		log.entering(getClass().getName(), "execute");
		Connection oimConnection = null;
		if (orchestration != null) {
			try {
				String umsdbJNDIName = (lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, UMSJNDINAME));
				log.fine("umsdbJNDIName: " + umsdbJNDIName);
				oimConnection = getDatabaseConnection(umsdbJNDIName);
				Identity newUserState = (Identity) getNewUserStates(orchestration);
				Identity oldUserState = (Identity) getOldUserStates(orchestration);
				processUserData(newUserState, oldUserState, oimConnection);

			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception working with contexts", e);
				throw new EventFailedException("Exception working with contexts", null, e);
			} finally {
				if (oimConnection != null) {
					try {
						oimConnection.close();
					} catch (SQLException e) {
						oimConnection = null;
					}
				}
			}
		}
		log.exiting(getClass().getName(), "execute");
		return new EventResult();

	}

	private void setUserTypeInUMS(String empNo, Connection oimConnection) {
		log.entering(getClass().getName(), "setUserTypeInUMS()");
		PreparedStatement ps = null;
		try {
			String updateUMSQuery = (lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, UPDATE_QUERY));
			log.fine("updateUMSQuery -" + updateUMSQuery);
			ps = oimConnection.prepareStatement(updateUMSQuery);
			ps.setString(1, empNo);
			ps.executeUpdate();
			log.fine("Sucgcessfully updated UMS database");
		} catch (SQLException e) {
			log.log(Level.SEVERE, " SQLException ", e);
		} catch (Exception e) {
			log.log(Level.SEVERE, " Exception ", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					ps = null;
				}
			}
		}
		log.exiting(getClass().getName(), "getNewUserStates");
	}

	private Connection getDatabaseConnection(String dbJNDIName) {
		log.entering(getClass().getName(), "getDatabaseConnection()");
		Connection con = null;
		try {
			Context initialContext = new InitialContext();
			if (initialContext != null) {
				DataSource datasource = (DataSource) initialContext.lookup(dbJNDIName.trim());
				if (datasource != null) {
					con = datasource.getConnection();
				} else {
					log.fine("Failed to lookup datasource.");
					return con;
				}
				log.fine("Sucgcessfully got a database connection to Server");
			}
		} catch (NamingException ex) {
			log.log(Level.SEVERE, "Exception working with contexts", ex);
			throw new EventFailedException("Exception working with contexts", null, ex);
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "Exception working with contexts", ex);
			throw new EventFailedException("Exception working with contexts", null, ex);
		}
		log.exiting(getClass().getName(), "getDatabaseConnection");

		return con;
	}

	/**
	 * Gets new user state
	 *
	 * @param orchestration
	 * @return
	 */
	private Object getNewUserStates(Orchestration orchestration) {
		log.entering(getClass().getName(), "getNewUserStates");
		Object newUserStates = null;
		HashMap interEventData = orchestration.getInterEventData();
		if (interEventData != null)
			newUserStates = interEventData.get(NEW_USER_STATE);

		log.exiting(getClass().getName(), "getNewUserStates");
		return newUserStates;
	}

	/**
	 * Gets Old User state
	 *
	 * @param orchestration
	 * @return
	 */
	private Object getOldUserStates(Orchestration orchestration) {
		log.entering(getClass().getName(), "getOldUserStates");
		Object oldUserStates = null;
		try {
			HashMap interEventData = orchestration.getInterEventData();
			if (interEventData != null)
				oldUserStates = interEventData.get(CURRENT_USER);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception working with contexts", e);
		}
		log.exiting(getClass().getName(), "getOldUserStates");
		return oldUserStates;
	}

	private boolean isNullOrEmpty(String strCheck) {
		return (strCheck == null) || strCheck.equals("null") || strCheck.trim().length() == 0;
	}

	@Override
	public BulkEventResult execute(long arg0, long arg1, BulkOrchestration bulkOrchestration) {
		log.entering(getClass().getName(), "bulk execute");

		Identity[] oldUserStatesIdntArr = (Identity[]) getOldUserStates(bulkOrchestration);
		Identity[] newUserStatesIdntArr = (Identity[]) getNewUserStates(bulkOrchestration);

		Connection oimConnection = null;
		try {
			if (bulkOrchestration != null) {
				String umsdbJNDIName = (lookupOperationsIntf.getDecodedValueForEncodedValue(CONFIGLOOKUP, UMSJNDINAME));
				log.fine("umsdbJNDIName: " + umsdbJNDIName);
				HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
				oimConnection = getDatabaseConnection(umsdbJNDIName);
				for (int i = 0; i < bulkParameters.length; i++) {

					Identity newUserState = newUserStatesIdntArr != null ? newUserStatesIdntArr[i] : null;
					Identity oldUserState = oldUserStatesIdntArr != null ? oldUserStatesIdntArr[i] : null;
					log.fine("newUserState In Bulk operation " + newUserState + " oldUserState in Bulk operation "
							+ oldUserState);
					processUserData(newUserState, oldUserState, oimConnection);
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception working with contexts", e);
			throw new EventFailedException("Exception working with contexts", null, e);
		} finally {
			if (oimConnection != null) {
				try {
					oimConnection.close();
				} catch (SQLException e) {
					oimConnection = null;
				}
			}
		}
		log.exiting(getClass().getName(), "bulk execute");
		return new BulkEventResult();
	}

	private void processUserData(Identity newUserState, Identity oldUserState, Connection oimConnection) {
		
		log.entering(getClass().getName(), "processUserData");
		String userType = null;
		String empNo = null;
		String olduserType = null;
		if (oldUserState != null) {
			olduserType = (String) oldUserState.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId());
			log.fine("Old user Type: " + olduserType);
		}
		if (newUserState != null) {
			userType = (String) newUserState.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId());
			log.fine("Current user Type: " + userType);
			empNo = (String) newUserState.getAttribute(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId());
			log.fine("Current empNo: " + empNo);
		}

		if (!isNullOrEmpty(userType) && !isNullOrEmpty(olduserType)) {
			if (olduserType.equalsIgnoreCase(HR_PENDING) && userType.equalsIgnoreCase(EMP)) {
				log.fine("user Type changed from " + olduserType + " to " + userType + ", setting User type in IAMUMS");
				setUserTypeInUMS(empNo, oimConnection);
			} else {
				log.fine("user Type changed from " + olduserType + " to " + userType
						+ ", no need to set User type in IAMUMS");
			}
		}
		log.exiting(getClass().getName(), "processUserData");
	}

	/**
	 * Gets new user state
	 *
	 * @param orchestration
	 * @return
	 */
	private Object getNewUserStates(BulkOrchestration orchestration) {
		log.entering(getClass().getName(), "getNewUserStates");

		Object newUserStates = null;
		HashMap interEventData = orchestration.getInterEventData();
		if (interEventData != null)
			newUserStates = interEventData.get(NEW_USER_STATE);
		log.exiting(getClass().getName(), "getNewUserStates");
		return newUserStates;
	}

	/**
	 * Gets Old User state
	 *
	 * @param orchestration
	 * @return
	 */
	private Object getOldUserStates(BulkOrchestration orchestration) {
		log.entering(getClass().getName(), "getOldUserStates");
		Object oldUserStates = null;
		try {
			HashMap interEventData = orchestration.getInterEventData();
			if (interEventData != null)
				oldUserStates = interEventData.get(CURRENT_USER);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception working with contexts", e);
		}
		log.exiting(getClass().getName(), "getOldUserStates");
		return oldUserStates;
	}

	@Override
	public void initialize(HashMap<String, String> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compensate(long arg0, long arg1, AbstractGenericOrchestration arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean cancel(long arg0, long arg1, AbstractGenericOrchestration arg2) {
		// TODO Auto-generated method stub
		return false;
	}

}

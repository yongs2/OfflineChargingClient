package org.mobicents.diameter.simulator.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.MetaData;
import org.jdiameter.api.Mode;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.StackType;
import org.jdiameter.api.acc.ClientAccSession;
import org.jdiameter.api.acc.ClientAccSessionListener;
import org.jdiameter.api.acc.ServerAccSession;
import org.jdiameter.api.acc.events.AccountAnswer;
import org.jdiameter.api.acc.events.AccountRequest;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.StackImpl;
import org.jdiameter.client.impl.helpers.XMLConfiguration;
import org.jdiameter.common.impl.app.acc.AccSessionFactoryImpl;
import org.jdiameter.common.impl.app.acc.AccountRequestImpl;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.servers.diameter.utils.StackCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfflineChargingClientImpl implements OfflineChargingClient, 
NetworkReqListener, EventListener<Request, Answer>, StateChangeListener<AppSession>, ClientAccSessionListener {

	private static final Logger log = LoggerFactory.getLogger(OfflineChargingClientImpl.class);

	// Application Id -----------------------------------------------------------
	private static final ApplicationId ACCOUNTING_APPLICATION_ID = ApplicationId.createByAccAppId(0, 3);

	// Configuration Values -----------------------------------------------------
	private static final String SERVER_HOST = "127.0.0.1";

	private static String REALM_NAME = "mobicents.org";
	private StackCreator stackCreator;
	private SessionFactory sessionFactory;
	private AccSessionFactoryImpl accountingSessionFactory;
	private OfflineChargingClientListener listener;

	private ConcurrentHashMap<String, ClientAccSession> acctSessions = new ConcurrentHashMap<String, ClientAccSession>(); 
	private ConcurrentHashMap<String, Integer> acctRecNumberMap = new ConcurrentHashMap<String, Integer>(); 

	public OfflineChargingClientImpl() throws Exception {
		// Initalize Stack
		AvpDictionary.INSTANCE.parseDictionary(getClass().getClassLoader().getResourceAsStream("dictionary.xml"));
		try
		{
			// isServer 가 true 이면, config-client.xml 은 http://www.jdiameter.org/jdiameter-server 로 시작해야 함
			stackCreator = new StackCreator(getClass().getClassLoader().getResourceAsStream("config-client.xml"), this, this, "Client", true);
			sessionFactory = stackCreator.getSessionFactory();
			accountingSessionFactory = new AccSessionFactoryImpl(sessionFactory);
			Network network = (Network)stackCreator.unwrap(Network.class);
			network.addNetworkReqListener(this, new ApplicationId[] {
					ACCOUNTING_APPLICATION_ID
			});

			stackCreator.start(Mode.ANY_PEER, 30000L, TimeUnit.MILLISECONDS);

			((ISessionFactory)sessionFactory).registerAppFacory(ServerAccSession.class, accountingSessionFactory);
			((ISessionFactory)sessionFactory).registerAppFacory(ClientAccSession.class, accountingSessionFactory);
			accountingSessionFactory.setStateListener(this);
			accountingSessionFactory.setClientSessionListener(this);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	// Client Acc Session Listener Implementation ------------------------------
	public void doAccAnswerEvent(ClientAccSession appSession, AccountRequest request, AccountAnswer answer) 
			throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		// TODO Auto-generated method stub
		// Extract interesting AVPs
		AvpSet acaAvps = answer.getMessage().getAvps();

		String subscriptionId = null;
		String serviceId = null;
		try {
			String username = acaAvps.getAvp(Avp.USER_NAME).getUTF8String();
			// It's in form subscription.service@REALM_NAME
			String[] identifiers = username.replaceFirst("@" + REALM_NAME, "").split("\\.");
			subscriptionId = identifiers[0];
			serviceId = identifiers[1];
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// Get the session-id value
		String sessionId = appSession.getSessionId();

		// We must be able to get this, it's mandatory
		int accRecType = -1;
		try {
			accRecType = answer.getAccountingRecordType();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// We must be able to get this, it's mandatory
		long accRecNumber = -1L;
		try {
			accRecNumber = answer.getAccountingRecordNumber();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// If we can't get it we'll fallback to DIAMETER_UNABLE_TO_COMPLY (5012)
		long resultCode = 5012L;
		try {
			resultCode = answer.getResultCodeAvp().getUnsigned32();
		}
		catch (AvpDataException e) {
			e.printStackTrace();
		}

		// Here we fallback to 0, it means the same as omitting 
		long acctInterimInterval = 0;
		try {
			acctInterimInterval = acaAvps.getAvp(Avp.ACCT_INTERIM_INTERVAL).getUnsigned32();
		}
		catch (AvpDataException e) {
			e.printStackTrace();
		}

		// Invoke the callback to deliver the answer
		listener.offlineChargingAnswerCallback(subscriptionId, serviceId, sessionId, accRecType, accRecNumber, resultCode, acctInterimInterval);
	}

	public void doOtherEvent(AppSession appsession, AppRequestEvent apprequestevent, AppAnswerEvent appanswerevent)
			throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		// TODO Auto-generated method stub

	}

	public void stateChanged(Enum arg0, Enum arg1) {
		// TODO Auto-generated method stub

	}

	public void stateChanged(AppSession arg0, Enum arg1, Enum arg2) {
		// TODO Auto-generated method stub

	}

	public void receivedSuccessMessage(Request arg0, Answer arg1) {
		// TODO Auto-generated method stub

	}

	public void timeoutExpired(Request arg0) {
		// TODO Auto-generated method stub

	}

	public Answer processRequest(Request arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	// Offline Charging Client Implementation ----------------------------------
	public void startOfflineCharging(String subscriptionId, String serviceId)
			throws Exception {
		// TODO Auto-generated method stub
		// Create new session to send start record
		ClientAccSession session = (ClientAccSession) accountingSessionFactory.getNewSession(null, ClientAccSession.class, ACCOUNTING_APPLICATION_ID, new Object[]{});

		// Store it in the map
		acctSessions.put(session.getSessionId(), session);

		// Get the account record number
		int accRecNumber = getAccountingRecordNumber(session.getSessionId(), true);

		sendAccountingRequest(session, getIdentifier(subscriptionId, serviceId), ACCOUNTING_RECORD_TYPE_START, accRecNumber);
	}

	public void interimOfflineCharging(String subscriptionId, String serviceId, String sessionId)
			throws Exception {
		// TODO Auto-generated method stub
		// Fetch existing session to send interim record
		ClientAccSession session = this.acctSessions.get(sessionId);

		// Get the account record number
		int accRecNumber = getAccountingRecordNumber(session.getSessionId(), false);

		sendAccountingRequest(session, getIdentifier(subscriptionId, serviceId), ACCOUNTING_RECORD_TYPE_INTERIM, accRecNumber);
	}

	public void stopOfflineCharging(String subscriptionId, String serviceId, String sessionId)
			throws Exception {
		// TODO Auto-generated method stub
		// Fetch existing session  (and remove it from map) to send stop record
		ClientAccSession session = this.acctSessions.remove(sessionId);

		// Get the account record number (and remove)
		int accRecNumber = getAccountingRecordNumber(session.getSessionId(), false);
		this.acctRecNumberMap.remove(session.getSessionId());

		sendAccountingRequest(session, getIdentifier(subscriptionId, serviceId), ACCOUNTING_RECORD_TYPE_STOP, accRecNumber);
	}

	public void eventOfflineCharging(String subscriptionId, String serviceId)
			throws Exception {
		// TODO Auto-generated method stub
		// Create new session to send event record
		ClientAccSession session = (ClientAccSession) accountingSessionFactory.getNewSession(null, ClientAccSession.class, ACCOUNTING_APPLICATION_ID, new Object[]{});

		// No need to store Session or Accounting-Record-Number as it's a one-shot.

		sendAccountingRequest(session, getIdentifier(subscriptionId, serviceId), ACCOUNTING_RECORD_TYPE_EVENT, 0);
	}

	public void setListener(OfflineChargingClientListener listener) {
		// TODO Auto-generated method stub
		this.listener = listener;
	}

	// Aux Methods --------------------------------------------------------------

	/**
	 * Gets an accounting record number for the specified user+service id
	 * @param identifier the user+service identifier
	 * @param isStart indicates if it's an initial record, which should be set to 0
	 * @return the accounting record number to be used in the AVP
	 */
	private int getAccountingRecordNumber(String sessionId, boolean isStart) {
		// An easy way to produce unique numbers is to set the value to 0 for
		// records of type EVENT_RECORD and START_RECORD, and set the value to 1
		// for the first INTERIM_RECORD, 2 for the second, and so on until the 
		// value for STOP_RECORD is one more than for the last INTERIM_RECORD.
		int accRecNumber = 0;
		if(!isStart) {
			accRecNumber = acctRecNumberMap.get(sessionId);
			accRecNumber = accRecNumber++;
		}

		acctRecNumberMap.put(sessionId, accRecNumber);

		return accRecNumber;
	}

	/**
	 * Creates an Accounting-Request with the specified data.
	 * 
	 * @param session the session to be used for creating the request 
	 * @param accRecordType the type of Accounting Record (Event, Start, Interim, Stop)
	 * @param username the value to be used in the User-Name AVP
	 * @return an AccountRequest object with the needed AVPs filled
	 * @throws InternalException
	 */
	private AccountRequest createAccountingRequest(ClientAccSession session, 
			int accRecordType, int accRecNumber, String username) throws InternalException {
		AccountRequest acr = new AccountRequestImpl(session, accRecordType, accRecNumber, REALM_NAME, SERVER_HOST);

		// Let's 'abuse' from User-Name AVP and use it for identifying user@service
		AvpSet avps = acr.getMessage().getAvps();
		avps.addAvp(Avp.USER_NAME, username, false);

		return acr;
	}

	/**
	 * Method for creating and sending an Accounting-Request
	 * 
	 * @param identifier the user+service identifier to be used in the User-Name AVP
	 * @param accRecType the type of Accounting Record (Event, Start, Interim, Stop)
	 * @throws Exception
	 */
	private void sendAccountingRequest(ClientAccSession session, String identifier,
			int accRecType, int accRecNumber) throws Exception {
		// Create Accounting-Request
		AccountRequest acr = createAccountingRequest(session, accRecType, accRecNumber, identifier);

		// Send it
		session.sendAccountRequest(acr);
	}

	/**
	 * Aux method for generating a unique identifier from subscription and service ids
	 * @param subscriptionId the subscription id to be used
	 * @param serviceId the service id to be used
	 * @return the generated unique identifier
	 */
	private String getIdentifier(String subscriptionId, String serviceId) {
		return subscriptionId + "." + serviceId + "@" + REALM_NAME;
	}
}

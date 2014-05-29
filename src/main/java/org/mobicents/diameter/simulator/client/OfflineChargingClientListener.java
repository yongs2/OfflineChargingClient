package org.mobicents.diameter.simulator.client;

/**
 * Listener interface to be implemented by applications 
 * wanting to have Offline Accounting
 */
public interface OfflineChargingClientListener {
	/**
	 * Callback method invoked by Offline Charging Client to deliver answers
	 * 
	 * @param subscriptionId the String value identifying the user
	 * @param serviceId the String value identifying the service
	 * @param sessionId a String value identifying the accounting session
	 * @param accountingRecordType the type of Accounting Record the answer refers to
	 * @param accountingRecordNumber the Accounting Record number the answer refers to
	 * @param resultCode the Result-Code value obtained from the answer
	 * @param acctInterimInterval the interval in seconds to send updates
	 */
	public void offlineChargingAnswerCallback(String subscriptionId, 
			String serviceId, String sessionId, int accountingRecordType, 
			long accountingRecordNumber, long resultCode, long acctInterimInterval);
}

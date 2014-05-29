package org.mobicents.diameter.simulator.client;

public interface OfflineChargingClient {
	// Accounting-Record-Type Values --------------------------------------------
	public static final int ACCOUNTING_RECORD_TYPE_EVENT    = 1;
	public static final int ACCOUNTING_RECORD_TYPE_START    = 2;
	public static final int ACCOUNTING_RECORD_TYPE_INTERIM  = 3;
	public static final int ACCOUNTING_RECORD_TYPE_STOP     = 4;
	
	/**
	 * Sends an Accounting-Request with Accounting-Record-Type set to "2" and the 
	 * corresponding Subscription-Id and Service-Id AVPs filled accordingly.
	 * 
	 * @param subscriptionId the String value to be used for Subscription-Id AVP
	 * @param serviceId the String value to be used for Service-Id AVP
	 * @throws Exception
	 */
	public abstract void startOfflineCharging(String subscriptionId, String serviceId)
			throws Exception;

	/**
	 * Sends an Accounting-Request with Accounting-Record-Type set to "3" and the
	 * corresponding Subscription-Id and Service-Id AVPs filled accordingly.
	 * 
	 * @param subscriptionId the String value to be used for Subscription-Id AVP
	 * @param serviceId the String value to be used for Service-Id AVP
	 * @throws Exception
	 */
	public void interimOfflineCharging(String subscriptionId, String serviceId, String sessionId) 
			throws Exception;

	/**
	 * Sends an Accounting-Request with Accounting-Record-Type set to "4" and the
	 * corresponding Subscription-Id and Service-Id AVPs filled accordingly.
	 * 
	 * @param subscriptionId the String value to be used for Subscription-Id AVP
	 * @param serviceId the String value to be used for Service-Id AVP
	 * @throws Exception
	 */
	public abstract void stopOfflineCharging(String subscriptionId, String serviceId, String sessionId) 
			throws Exception;

	/**
	 * Sends an Accounting-Request with Accounting-Record-Type set to "1" and the
	 * corresponding Subscription-Id and Service-Id AVPs filled accordingly.
	 * 
	 * @param subscriptionId the String value to be used for Subscription-Id AVP
	 * @param serviceId the String value to be used for Service-Id AVP
	 * @throws Exception
	 */
	public abstract void eventOfflineCharging(String subscriptionId, String serviceId)
			throws Exception;

	/**
	 * Sets the listener to receive the callbacks from this charging client. 
	 * @param listener an OfflineChargingClientListener implementor to be the listener
	 */
	public abstract void setListener(OfflineChargingClientListener listener);
}

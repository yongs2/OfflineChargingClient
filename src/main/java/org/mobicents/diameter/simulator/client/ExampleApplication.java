package org.mobicents.diameter.simulator.client;

import static org.mobicents.diameter.simulator.client.OfflineChargingClient.*;

public class ExampleApplication implements OfflineChargingClientListener {
	// Internal Client State Machine --------------------------------------------
	private static final int STATE_IDLE                  = 0;
	private static final int STATE_START_ACR_SENT        = 2;
	private static final int STATE_START_ACA_SUCCESS     = 4;
	private static final int STATE_INTERIM_ACR_SENT      = 6;
	private static final int STATE_INTERIM_ACA_SUCCESS   = 8;
	private static final int STATE_STOP_ACR_SENT         = 10;
	private static final int STATE_STOP_ACA_SUCCESS      = 12;
	private static final int STATE_EVENT_ACR_SENT        = 14;
	private static final int STATE_EVENT_ACA_SUCCESS     = 16;
	private static final int STATE_END                   = 18;
	private static final int STATE_ERROR                 = 99;

	private int currentState = STATE_IDLE;

	public static void main(String[] args) throws Exception {
		ExampleApplication app = new ExampleApplication(new OfflineChargingClientImpl());
		app.occ.startOfflineCharging("", "");
	}

	private OfflineChargingClient occ;

	public ExampleApplication(OfflineChargingClient occ) {
		this.occ = occ;
		occ.setListener(this);
	}

	public void offlineChargingAnswerCallback(String subscriptionId, String serviceId, 
			String sessionId, int accountingRecordType, long accountingRecordNumber, 
			long resultCode, long acctInterimInterval) {
		// Handle the EVENT situation
		if(accountingRecordType == ACCOUNTING_RECORD_TYPE_EVENT) {
			if(this.currentState == STATE_EVENT_ACR_SENT) {
				if(resultCode == 2001) {
					this.currentState = STATE_EVENT_ACA_SUCCESS;
					System.out.println("(((o))) Event Offline Charging for user '"+ subscriptionId + "' and service '" + serviceId + "' completed! (((o)))");
					// and now just to be correct...
					this.currentState = STATE_END;          
				}
			} else {
				this.currentState = STATE_ERROR;
				throw new RuntimeException("Unexpected message received.");
			}
		}
		// Handle START / INTERIM / STOP situation
		else {
			switch(this.currentState) {
			// Receiving an Answer at any of these states is an error
			case STATE_IDLE:
			case STATE_EVENT_ACA_SUCCESS:
			case STATE_START_ACA_SUCCESS:
			case STATE_INTERIM_ACA_SUCCESS:
			case STATE_STOP_ACA_SUCCESS:
				// At any of these states we don't expect to receive an ACA, move to error.
				this.currentState = STATE_ERROR;
				break;
				// We've sent ACR EVENT
			case STATE_START_ACR_SENT:
				if(accountingRecordType == ACCOUNTING_RECORD_TYPE_START) {
					if(resultCode >= 2000L && resultCode < 3000L ) {
						// Our event charging has completed successfully. We're done!
						System.err.println("(((o))) Offline Charging for user '" + subscriptionId + "' and service '" + serviceId + "' started... (((o)))");

						if(acctInterimInterval > 0) {
							try {
								// Let's sleep until next interim update...
								Thread.sleep(acctInterimInterval * 1000);

								// We send an update at the correct time
								occ.interimOfflineCharging(subscriptionId, serviceId, sessionId);
							} catch (Exception e) {
								this.currentState = STATE_ERROR;
								throw new RuntimeException("Unable to schedule/send interim update.", e);
							}
						}
					} else {
						// It failed
						System.err.println("(((x))) Offline Charging for user '" + subscriptionId + "' and service '" + serviceId + "' failed with Result-Code="+ resultCode + "! (((x)))");
					}
				} else {
					this.currentState = STATE_ERROR;
					throw new RuntimeException("Unexpected message received.");
				}
				break;
				// We've sent ACR START
			case STATE_INTERIM_ACR_SENT:
				if(accountingRecordType == ACCOUNTING_RECORD_TYPE_INTERIM) {
					if(resultCode >= 2000L && resultCode < 3000L) {
						// Our offline charging has started successfully...
						System.out.println("(((o))) Offline Charging for user '" + subscriptionId + "' and service '" + serviceId + "' updated... (((o)))");

						if(acctInterimInterval > 0) {
							try {
								// Let's sleep until next interim update...
								Thread.sleep(acctInterimInterval);

								// We send an update at the correct time
								occ.interimOfflineCharging(subscriptionId, serviceId, sessionId);
							} catch (Exception e) {
								this.currentState = STATE_ERROR;
								throw new RuntimeException("Unable to schedule/send interim update.", e);
							}
						}
					} else {
						// It failed, let's warn the application
						System.out.println("(((x))) Offline Charging for user '" + subscriptionId + "' and service '" + serviceId + "' failed to start with Result-Code=" + resultCode + "! (((x)))");
					}
				} else {
					this.currentState = STATE_ERROR;
					throw new RuntimeException("Unexpected message received.");
				}
				break;
			case STATE_STOP_ACR_SENT:
				if(accountingRecordType == ACCOUNTING_RECORD_TYPE_INTERIM) {
					if(resultCode >= 2000L && resultCode < 3000L) {
						// Our offline charging has started successfully...
						System.out.println("(((o))) Offline Charging for user '" + subscriptionId + "' and service '" + serviceId + "' stopped! (((o)))");
					} else {
						// It failed, let's warn the application
						System.out.println("(((x))) Offline Charging for user '" + subscriptionId + "' and service '" + serviceId + "' failed to stop with Result-Code=" + resultCode + "! (((x)))");
					}
				} else {
					this.currentState = STATE_ERROR;
					throw new RuntimeException("Unexpected message received.");
				}
				break;
			default:
				this.currentState = STATE_ERROR;
				throw new RuntimeException("Unexpected message received.");
			}
		}
	}
}

package org.mobicents.servers.diameter.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.MetaData;
import org.jdiameter.api.Mode;
import org.jdiameter.api.MutablePeerTable;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.OverloadManager;
import org.jdiameter.api.Peer;
import org.jdiameter.api.PeerTable;
import org.jdiameter.api.RealmTable;
import org.jdiameter.api.Request;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.Statistic;
import org.jdiameter.api.StatisticRecord;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;

public class StackCreator extends StackImpl implements Stack {
	private static final Logger logger = LoggerFactory.getLogger(StackCreator.class);
	private Stack stack = null;
	
	public StackCreator(Configuration config, NetworkReqListener networkReqListener, EventListener<Request, Answer> eventListener, String identifier, Boolean isServer) {
		this.stack = new StackImpl();

		try {
			this.stack.init(config);

			// Let it stabilize...
			Thread.sleep(500);

			Network network = stack.unwrap(Network.class);

			Set<ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

			for (ApplicationId appId : appIds) {
				if(logger.isInfoEnabled()) {
					logger.info("Diameter " + identifier + " :: Adding Listener for [" + appId + "].");
				}
				network.addNetworkReqListener(networkReqListener, appId);
			}

			if(logger.isInfoEnabled()) {
				logger.info("Diameter " + identifier + " :: Supporting " + appIds.size() + " applications.");
			}
		} catch (Exception e) {
			logger.error("Failure creating stack '" + identifier + "'", e);
		}
	}

	public StackCreator(InputStream streamConfig, NetworkReqListener networkReqListener, EventListener<Request, Answer> eventListener, String dooer, Boolean isServer) throws Exception {
		this(isServer ? new XMLConfiguration(streamConfig) : new org.jdiameter.client.impl.helpers.XMLConfiguration(streamConfig), networkReqListener, eventListener, dooer, isServer);
	}

	public StackCreator(String stringConfig, NetworkReqListener networkReqListener, EventListener<Request, Answer> eventListener, String dooer, Boolean isServer) throws Exception {
		this(isServer ? new XMLConfiguration(new ByteArrayInputStream(stringConfig.getBytes())) : new org.jdiameter.client.impl.helpers.XMLConfiguration(new ByteArrayInputStream(stringConfig.getBytes())), networkReqListener, eventListener, dooer, isServer);
	}

	public void destroy() {
		stack.destroy();
	}

	public java.util.logging.Logger getLogger() {
		return stack.getLogger();
	}

	public MetaData getMetaData() {
		return stack.getMetaData();
	}

	public SessionFactory getSessionFactory() throws IllegalDiameterStateException {
		return stack.getSessionFactory();
	}

	public SessionFactory init(Configuration config) throws IllegalDiameterStateException, InternalException {
		return stack.init(config);
	}

	public boolean isActive() {
		return stack.isActive();
	}

	public boolean isWrapperFor(Class<?> iface) throws InternalException {
		return stack.isWrapperFor(iface);
	}

	public void start() throws IllegalDiameterStateException, InternalException {
		stack.start();
	}

	public void start(Mode mode, long timeout, TimeUnit unit) throws IllegalDiameterStateException, InternalException {
		stack.start(mode, timeout, unit);
	}

	public void stop(long timeout, TimeUnit unit, int disconnectReason) throws IllegalDiameterStateException, InternalException {
		stack.stop(timeout, unit, disconnectReason);
	}

	public <T> T unwrap(Class<T> iface) throws InternalException {
		return stack.unwrap(iface);
	}

	public void PrintPeerTable() {
		MetaData metaData = stack.getMetaData();
		try {
			if (stack.isWrapperFor(MutablePeerTable.class)) {

				MutablePeerTable mutablePeerTable = stack.unwrap(MutablePeerTable.class);
				List<Peer> peerTable = mutablePeerTable.getPeerTable();
				int i = 0, j = 0;
				if(peerTable.size() <= 0)
					return;
				
				logger.info("===============================================================================");
				for (Peer p : peerTable) {
					Statistic stat = mutablePeerTable.getStatistic(p.getRealmName());
					if(stat == null) {
						logger.info("Peer[" + i + "]=" + p.getRealmName() + ", Stat is NULL");
						continue;
					}
					
					logger.info("Peer[" + i + "]=" + p.getRealmName() + ", Stat=" + stat.getName() + ", "+ stat.getDescription());
					j=0;
					for (StatisticRecord record : stat.getRecords()) {
						if(record == null) {
							break;
						}
						if(record.getName() == null) {
							continue;
						}
						// record 이름에 맞춰서 정확하게 get 함수를 호출하지 않으면 오류 발생.
						if(record.getName().equals("MessageProcessingTime")) {
							logger.info("[" + i + "][" + j + "].Name=" + record.getName() + ", Value=" + record.getValueAsDouble());
						} else if(record.getName().equals("QueueSize")) {
							logger.info("[" + i + "][" + j + "].Name=" + record.getName() + ", Value=" + record.getValueAsInt());
						} else {
							logger.info("[" + i + "][" + j + "].Name=" + record.getName() + ", Value=" + record.getValueAsLong());
						}
						j++;
					}
					i++;
				}
				logger.info("===============================================================================");
			}
		} catch (InternalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

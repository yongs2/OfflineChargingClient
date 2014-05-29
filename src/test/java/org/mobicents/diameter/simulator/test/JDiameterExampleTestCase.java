package org.mobicents.diameter.simulator.test;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Mode;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Peer;
import org.jdiameter.api.PeerTable;
import org.jdiameter.api.Request;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servers.diameter.utils.DiameterUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDiameterExampleTestCase {
	
	private static final Logger LOG = LoggerFactory.getLogger(JDiameterExampleTestCase.class);
	
	static{
		System.out.println("Current working directory is " + System.getProperty("user.dir") +", logfile.name=" + System.getProperty("logfile.name"));
		InputStream inStreamLog4j;
		try {
			inStreamLog4j = new FileInputStream("src/test/resources/log4j.properties");
			Properties propertiesLog4j = new Properties();
			try {
				propertiesLog4j.load(inStreamLog4j);
				PropertyConfigurator.configure(propertiesLog4j);
			} catch (Exception e) {
				e.printStackTrace();
			}

			LOG.debug("log4j configured");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private StackImpl server;
	private StackImpl client;
	 
	private Request actualRequest;
	private Answer actualAnswer;
	private SessionFactory clientSessionFactory;
	
	private long testAuthAppId = 33333;
	
	private String readFile(InputStream resourceAsStream) throws IOException {
		// TODO Auto-generated method stub
		BufferedInputStream bin = new BufferedInputStream(resourceAsStream);

		byte[] contents = new byte[1024];

		int bytesRead = 0;
		String strFileContents;
		StringBuilder sb = new StringBuilder();

		while ((bytesRead = bin.read(contents)) != -1) {
			strFileContents = new String(contents, 0, bytesRead);
			sb.append(strFileContents);
		}
		return sb.toString();
	}
	
	@Before
	public void initClientAndServer() throws Exception {
		server = new StackImpl();
		System.out.println("Current working directory is " + System.getProperty("user.dir"));
		
		server.init(new XMLConfiguration("src/test/resources/server-jdiameter-config.xml"));
		server.unwrap(Network.class).addNetworkReqListener(new NetworkReqListener() {
			public Answer processRequest(Request request) {
				actualRequest = request;
				return request.createAnswer();
			}
		}, ApplicationId.createByAuthAppId(testAuthAppId));
		
		server.start();  
		Thread.sleep(5000);
		LOG.info("Server should start."); 

		client = new StackImpl();
		
		clientSessionFactory = client.init(new XMLConfiguration("src/test/resources/client-jdiameter-config.xml"));

		client.unwrap(Network.class).addNetworkReqListener(new NetworkReqListener() {
			public Answer processRequest(Request request) {
				return null;
			}
		}, ApplicationId.createByAuthAppId(testAuthAppId));
		
		client.start(Mode.ALL_PEERS, 5000, TimeUnit.MILLISECONDS);  
		Thread.sleep(5000);
		LOG.info("Client should start.");
	}
	
	@After
	public void tearDown() throws Exception {
		client.destroy();
		server.destroy();
	}
	
	@Test
	public void onePeerIsConnected() throws Exception {
		List<Peer> peers = server.unwrap(PeerTable.class).getPeerTable();
		assertEquals("Initial setup didn't succeed.",1,peers.size());
	}
	
	@Test
	public void sendRequest() throws Exception {
		int testCommandCode = 7;
		
		Session session = clientSessionFactory.getNewSession();
		Request request = session.createRequest(testCommandCode, ApplicationId.createByAuthAppId(testAuthAppId), "exchange.example.org", "127.0.0.1");

		session.send(request, new EventListener<Request, Answer>(){
			public void receivedSuccessMessage(Request request, Answer arg1) {
				actualAnswer = arg1;
			}

			public void timeoutExpired(Request request) {

			}
		});
		Thread.sleep(1000);
		LOG.info("Request should rich the server.");

		assertNotNull("Request wasn't sent to server sent.",actualRequest);
		assertEquals(testCommandCode, actualRequest.getCommandCode());
		assertEquals(6, actualRequest.getAvps().size());
		DiameterUtilities.printMessage(actualRequest);
		
		assertNotNull(actualAnswer);
		assertEquals(testCommandCode, actualAnswer.getCommandCode());
		assertEquals(2, actualAnswer.getAvps().size());
		DiameterUtilities.printMessage(actualAnswer);
	}
}

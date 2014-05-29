package org.mobicents.servers.diameter.utils;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Message;
import org.jdiameter.api.validation.AvpRepresentation;
import org.jdiameter.api.validation.Dictionary;
import org.jdiameter.client.impl.parser.ElementParser;
import org.jdiameter.common.impl.validation.DictionaryImpl;

public class DiameterUtilities {
	private static final Logger logger = LoggerFactory.getLogger(DiameterUtilities.class);
	
	public static Dictionary AVP_DICTIONARY = DictionaryImpl.INSTANCE;
	
	public static void printMessage(Message message) {
		String reqFlag = message.isRequest() ? "R" : "A";
		String flags = reqFlag += message.isError() ? " | E" : "";
		
		if(logger.isInfoEnabled()) {
			logger.info("Message [" + flags + "] Command-Code: " + message.getCommandCode() + " / E2E(" 
					+ message.getEndToEndIdentifier() + ") / HbH(" + message.getHopByHopIdentifier() + ")");
			logger.info("- - - - - - - - - - - - - - - - AVPs - - - - - - - - - - - - - - - -");
			printAvps(message.getAvps());
		}
	}
	
	public static void printAvps(AvpSet avps) {
		printAvps(avps, "");
	}

	public static Date TimeToDate(Avp avp) throws AvpDataException {
		ElementParser parser = new ElementParser();
	  
		try {
			
			byte[] tmp = new byte[8];
			System.arraycopy(avp.getRaw(), 0 , tmp, 4, 4);
			return new Date((parser.bytesToLong(tmp)) * 1000L);
			/*
			byte[] bytes = avp.getRaw();
			ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
			buffer.put(bytes);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.flip();
			long timestamp = buffer.getInt();
			return new Date(timestamp*1000);
			*/
		} catch (Exception e) {
			throw new AvpDataException(e);
		}
	}
  
	public static String toOctetString(Avp avp) throws AvpDataException {
		ElementParser parser = new ElementParser();
	  
		try {
			return parser.bytesToOctetString(avp.getOctetString());
		} catch (Exception e) {
			throw new AvpDataException(e);
		}
	}

	public static void printAvps(AvpSet avps, String indentation) {
		for(Avp avp : avps) {
			AvpRepresentation avpRep = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId());
			Object avpValue = null;
			boolean isGrouped = false;

			try {
				String avpType = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId()).getType();

				if("Integer32".equals(avpType) || "AppId".equals(avpType)) {
					avpValue = avp.getInteger32();
				}
				else if("Unsigned32".equals(avpType) || "VendorId".equals(avpType)) {
					avpValue = avp.getUnsigned32();
				}
				else if("Float64".equals(avpType)) {
					avpValue = avp.getFloat64();
				}
				else if("Integer64".equals(avpType)) {
					avpValue = avp.getInteger64();
				}
				else if("Time".equals(avpType)) {
					//avpValue = avp.getTime();	// Date(((bytesToLong(tmp) - SECOND_SHIFT) * 1000L)); 으로 SECOND_SHIFT를 계산하므로, 바꿔야 함. 
					avpValue = TimeToDate(avp);
				}
				else if("Unsigned64".equals(avpType)) {
					avpValue = avp.getUnsigned64();
				}
				else if("Grouped".equals(avpType)) {
					avpValue = "<Grouped>";
					isGrouped = true;
				}
				else if("UTF8String".equals(avpType)) {
					avpValue = avp.getUTF8String();
				}
				else {
					avpValue = toOctetString(avp).replaceAll("\r", "").replaceAll("\n", "");
				}
			}
			catch (Exception ignore) {
				try {
					avpValue = avp.getOctetString().toString().replaceAll("\r", "").replaceAll("\n", "");
				}
				catch (AvpDataException e) {
					avpValue = avp.toString();
				}
			}

			String avpLine = null;
			if( (avpRep == null) || (avpRep.getName() == null) ) {
				avpLine = indentation + avp.getCode() + ": XXX";
			} else {
				avpLine = indentation + avp.getCode() + ": " + avpRep.getName();
			}
			while(avpLine.length() < 50) {
				avpLine += avpLine.length() % 2 == 0 ? "." : " ";
			}
			avpLine += avpValue;

			logger.debug(avpLine);

			if(isGrouped) {
				try {
					printAvps(avp.getGrouped(), indentation + "  ");          
				}
				catch (AvpDataException e) {
					// Failed to ungroup... ignore then...
				}
			}
		}
	}
}

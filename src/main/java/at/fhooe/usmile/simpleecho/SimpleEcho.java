package at.fhooe.usmile.simpleecho;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;

public class SimpleEcho extends Applet {

	final static byte SEND_TEST = 0x51;
	final static byte RECIEVE_TEST = 0x52;
	final static byte ECHO_TEST = 0x53;

    private static class innerClass {
        short field1 = 0;
        byte field2 = 1;
    }
    
    private innerClass[] field1;
    private short field2;
    SimpleEcho field3;
    
	public SimpleEcho() {
        field1 = new innerClass[1000];
        field2 = (short)42;
        for (short i = 0; i < (short)field1.length; ++i) {
            field1[i] = new innerClass();
            field1[i].field1 = (short)(i * field2);
            field1[i].field2 *= i;
        }
        field2 = (short)42;
        field3 = this;
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration

		new SimpleEcho().register();
	}

	public void process(APDU apdu) {
        field2 += 123;
        for (short i = 0; i < (short)field1.length; ++i) {
            if ((field2 & 0x001) == (i & 0x001)) {
                field1[i] = new innerClass();
            }
            field1[i].field1 = (short)(i * field2);
            field1[i].field2 *= (i+1);
        }
        
        
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] incomingBuf = apdu.getBuffer();
		short len;
		switch (incomingBuf[ISO7816.OFFSET_INS]) {
		case RECIEVE_TEST:
			len = apdu.setOutgoing();
			apdu.setOutgoingLength(len);
			apdu.sendBytes((short) 0, len);
			break;
		case SEND_TEST:
			apdu.setIncomingAndReceive();
			apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 0);
			break;
		case ECHO_TEST:
			len = apdu.setIncomingAndReceive();
			apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, len);
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}

	}
}
/**
 * 
 */
package at.fhooe.usmile.app.se_performance.tests;

import java.util.Arrays;

import at.fhooe.usmile.app.se_performance.TestPerformerActivity;
import at.fhooe.usmile.app.se_performance.connection.SEConnection;
import at.fhooe.usmile.app.se_performance.util.APDU;
import at.fhooe.usmile.app.se_performance.util.Converter;

/**
 * @author Local2013
 *
 */
public class RSA1024Test {
	private SEConnection seConn;
	private final static byte[] testPublicParam = new byte[] { 0x04, 0x61,
			(byte) 0xEB, (byte) 0xA7, (byte) 0xE3, 0x58, 0x1F, (byte) 0x97,
			(byte) 0xA7, (byte) 0xB3, (byte) 0x8E, (byte) 0xB1, (byte) 0x97,
			(byte) 0x94, 0x1D, 0x3F, (byte) 0xB1, (byte) 0xC6, 0x56, 0x3B,
			0x34, 0x69, 0x4B, 0x28, (byte) 0xF9, 0x58, 0x7E, 0x66, 0x01, 0x03,
			0x0E, (byte) 0xB2, 0x67, (byte) 0xAE, (byte) 0x86, (byte) 0xDA,
			0x48, (byte) 0xD6, 0x1D, (byte) 0xE0, 0x7A, 0x00, (byte) 0xFD,
			(byte) 0x9E, 0x6B, 0x5F, (byte) 0xC4, 0x43, 0x21 };

	private final static byte CLA = (byte) 0x80;

	private final static byte INS_KEYPAIR_GEN_TEST = 0x21;
	private final static byte INS_CARD_PUBLIC_TEST = 0x46;
	private final static byte INS_EXT_PUB_TEST = 0x48;
	private final static byte INS_ENCRYPT_TEST = 0x41;
	private final static byte INS_DECRYPT_TEST = 0x42;
	
	private final static byte INS_SIGN_TEST = 0x43;
	private final static byte INS_TOBEVERIFIED = 0x44;
	private final static byte INS_VERIFY_TEST = 0x45;

	
	
	private final static byte P1 = 0x00;
	private final static byte P2 = 0x00;
	private final static byte Le = 0x00;

	private boolean runTest = false;
	private Thread testThread;
	private int loop = 0;
	private int count = 0;

 	private long keypairGenAvgTime = 0L;
	private long getCardPublicAvgTime = 0L;
	private long setExtPublicAvgTime = 0L;
	private long encryptAvgTime = 0L;
	private long decryptAvgTime = 0L;
	private long signAvgTime = 0L;
	private long verifyAvgTime = 0L;

 	private long keypairGenTime = 0L;
	private long getCardPublicTime = 0L;
	private long setExtPublicTime = 0L;
	private long encryptTime = 0L;
	private long decryptTime = 0L;
	private long signTime = 0L;
	private long verifyTime = 0L;

 	private long keypairGenTimeMedian = 0L;
	private long getCardPublicTimeMedian = 0L;
	private long setExtPublicMedian = 0L;
	private long encryptTimeMedian = 0L;
	private long decryptTimeMedian = 0L;
	private long signTimeMedian = 0L;
	private long verifyTimeMedian = 0L;


 	private long[] keypairGenTimeValues;
	private long[] getCardPublicValues;
	private long[] setExtPublicValues;
	private long[] encryptTimeValues;
	private long[] decryptTimeValues;
	private long[] signTimeValues;
	private long[] verifyTimeValues;
	
 	private long keypairGenWorstTime = 0L;
	private long getCardPublicWorstTime = 0L;
	private long setExtPublicWorstTime = 0L;
	private long encryptWorstTime = 0L;
	private long decryptWorstTime = 0L;
	private long signWorstTime = 0L;
	private long verifyWorstTime = 0L;
	
	private long tempTime;
	private TestPerformerActivity testerInstance;
	/**
	 * 
	 */
	public RSA1024Test(TestPerformerActivity _testerInstance, SEConnection _seConn) {
		testerInstance = _testerInstance;
		seConn = _seConn;
	}
	public void startTest(int _loop,byte[] _data) {
		final byte[] testData = _data;
		loop = _loop;
		runTest = true;
		count = 0;

 		keypairGenTimeValues = new long[loop];
		getCardPublicValues = new long[loop];
		setExtPublicValues = new long[loop];
		encryptTimeValues = new long[loop];
		decryptTimeValues = new long[loop];
		signTimeValues = new long[loop];
		verifyTimeValues = new long[loop];
		
		 
		Arrays.fill(keypairGenTimeValues, Integer.MAX_VALUE);
		Arrays.fill(getCardPublicValues, Integer.MAX_VALUE);
		Arrays.fill(setExtPublicValues, Integer.MAX_VALUE);
		Arrays.fill(encryptTimeValues, Integer.MAX_VALUE);
		Arrays.fill(decryptTimeValues, Integer.MAX_VALUE);
		Arrays.fill(signTimeValues, Integer.MAX_VALUE);
		Arrays.fill(verifyTimeValues, Integer.MAX_VALUE);

		testThread = new Thread() {

			public void run() {
				while (runTest && loop > 0) {

					count += 1;
					keyPairGenTest(count);
					byte[] respApdu = getCardPublicTest(count);
					// check if the response contains correct length of modulus and extract the modulus
					
					if(respApdu.length == 130){
						byte[] modulus = Arrays.copyOfRange(respApdu, 0, 128);
						setExtPublicTest(count, modulus);
						
						respApdu = encryptTest(count, testData);
						byte[] cipher = Arrays.copyOf(respApdu, 128);
						decryptTest(count, cipher);	
						
						respApdu = signTest(count, testData);
						byte[] signature = Arrays.copyOf(respApdu, 128);
						
						verifyTest(count, signature, testData);
						
					}
					
					loop -= 1;

				}

				perfomanceSummary();
				resetElapsedTime();
				runTest = false;
			}
		};
		testThread.start();
	}

	private void resetElapsedTime() {
 		keypairGenTime = 0L;
		getCardPublicTime = 0L;
		setExtPublicTime = 0L;
		encryptTime = 0L;
		decryptTime = 0L;
		signTime = 0L;
		verifyTime = 0L;

 		keypairGenWorstTime = 0L;
		getCardPublicWorstTime = 0L;
		setExtPublicWorstTime = 0L;
		encryptWorstTime = 0L;
		decryptWorstTime = 0L;
		getCardPublicWorstTime = 0L;
		setExtPublicWorstTime = 0L;

 		keypairGenTimeMedian = 0L;
		getCardPublicTimeMedian = 0L;
		setExtPublicMedian = 0L;
		encryptTimeMedian = 0L;
		decryptTimeMedian = 0L;
		signTimeMedian = 0L;
		verifyTimeMedian = 0L;
 	}

	public void perfomanceSummary() {

		for (int i = 0; i < count; i++) {
 			keypairGenTime += keypairGenTimeValues[i];
			getCardPublicTime += getCardPublicValues[i];
			setExtPublicTime += setExtPublicValues[i];
			encryptTime += encryptTimeValues[i];
			decryptTime += decryptTimeValues[i];
			signTime += signTimeValues[i];
			verifyTime += verifyTimeValues[i];
		}

		// long[] sortedValue =
	 
		Arrays.sort(keypairGenTimeValues);
		Arrays.sort(getCardPublicValues);
		Arrays.sort(setExtPublicValues);
		Arrays.sort(encryptTimeValues);
		Arrays.sort(decryptTimeValues);
		Arrays.sort(signTimeValues);
		Arrays.sort(verifyTimeValues);

		// worst time
		keypairGenWorstTime = keypairGenTimeValues[count-1];
		getCardPublicWorstTime = getCardPublicValues[count-1];
		setExtPublicWorstTime = setExtPublicValues[count-1];
		encryptWorstTime = encryptTimeValues[count-1];
		decryptWorstTime = decryptTimeValues[count-1];
		signWorstTime = signTimeValues[count-1];
		verifyWorstTime = verifyTimeValues[count-1];
		
		//setExtPublicWorstTime = setExtPublicValues[count-1];
		
		// median calculation
		int mid = count / 2;

		if (count % 2 != 0) {

		 
			keypairGenTimeMedian = keypairGenTimeValues[mid];
			getCardPublicTimeMedian = getCardPublicValues[mid];
			setExtPublicMedian = setExtPublicValues[mid];
			encryptTimeMedian = encryptTimeValues[mid];
			decryptTimeMedian = decryptTimeValues[mid];
			signTimeMedian = signTimeValues[mid];
			verifyTimeMedian = verifyTimeValues[mid];

		} else {
 
			keypairGenTimeMedian = (keypairGenTimeValues[mid - 1] + keypairGenTimeValues[mid]) / 2;
			getCardPublicTimeMedian += (getCardPublicValues[mid - 1] + getCardPublicValues[mid]) / 2;
			setExtPublicMedian = (setExtPublicValues[mid - 1] + setExtPublicValues[mid]) / 2;
			encryptTimeMedian = (encryptTimeValues[mid-1] +  encryptTimeValues[mid])/2;
			decryptTimeMedian = (decryptTimeValues[mid-1] + decryptTimeValues[mid])/2;
			signTimeMedian = (signTimeValues[mid-1] +  signTimeValues[mid])/2;
			verifyTimeMedian = (verifyTimeValues[mid-1] + verifyTimeValues[mid])/2;
		}

 		keypairGenAvgTime = keypairGenTime / count;
		getCardPublicAvgTime = getCardPublicTime / count;
		setExtPublicAvgTime = setExtPublicTime / count;
		encryptAvgTime = encryptTime /count;
		decryptAvgTime = decryptTime/ count;
		signAvgTime = signTime/ count;
		verifyAvgTime = verifyTime /count;
		
		// display summary

		testerInstance.logText("\n----Performance Summary---- iterations : "
				+ count);
		testerInstance
				.logText("\n==================================================");
		testerInstance.logText("\n--- KeyPair Generation \n   Average : ->"
				+ keypairGenAvgTime + " usec    Worst: ->"
				+ keypairGenWorstTime + " usec \n   Median  : ->"
				+ keypairGenTimeMedian + " usec");
		testerInstance.logText("\n--- Get Card Public Param \n   Average: ->"
				+ getCardPublicAvgTime + " usec    Worst: ->"
				+ getCardPublicWorstTime + " usec \n   Median  : ->"
				+ getCardPublicTimeMedian + " usec");
		testerInstance.logText("\n--- Set Ext. Public Param\n   Average : ->"
				+ setExtPublicAvgTime + " usec    Worst: ->"
				+ setExtPublicWorstTime + " usec \n   Median  : ->"
				+ setExtPublicMedian + " usec");
		testerInstance.logText("\n--- Ecrypt with Exteranl Public \n   Average : ->"
				+ encryptAvgTime + " usec    Worst: ->"
				+ encryptWorstTime + " usec \n   Median  : ->"
				+ encryptTimeMedian + " usec");
		testerInstance.logText("\n--- Decrypt with Own Private \n   Average : ->"
				+ decryptAvgTime + " usec    Worst: ->"
				+ decryptWorstTime + " usec \n   Median  : ->"
				+ decryptTimeMedian + " usec");
		testerInstance.logText("\n--- RSA sign with Own Private \n   Average : ->"
				+ signAvgTime + " usec    Worst: ->"
				+ signTimeMedian + " usec \n   Median  : ->"
				+ signWorstTime + " usec");
		testerInstance.logText("\n--- RSA verify with Ext. Public with known signature \n   Average : ->"
				+ verifyAvgTime + " usec    Worst: ->"
				+ verifyTimeMedian + " usec \n   Median  : ->"
				+ verifyWorstTime + " usec");
	 
		testerInstance.logText("end");
	}

	public void stopTest() {
		runTest = false;
	}

	public boolean testRunning() {
		return runTest;
	}
	
	
	public void keyPairGenTest(int testnum) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_KEYPAIR_GEN_TEST, P1, P2,
				new byte[0], Le);
		testerInstance.logText("\nRSA 1024 Keypair Generation Test " + testnum
				+ "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		keypairGenTimeValues[testnum - 1] = tempTime;
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
	}

	public byte[] getCardPublicTest(int testnum) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_CARD_PUBLIC_TEST, P1, P2,
				new byte[0], (byte) 0x80);
		testerInstance.logText("\nRSA 1024 Get Card Public Param Test " + testnum
				+ "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		getCardPublicValues[testnum - 1] = tempTime;
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
		return respApdu;
	}

	public void setExtPublicTest(int testnum, byte[] _data) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_EXT_PUB_TEST, P1, P2,
				_data, Le);
		testerInstance.logText("\nRSA 1024 Set External Public Param Test " + testnum
				+ "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		
		setExtPublicValues[testnum - 1] = tempTime;
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
	}

	public byte[] encryptTest(int testnum, byte[] _data) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_ENCRYPT_TEST, P1,
				P2, _data, Le);
		testerInstance.logText("\nRSA Encryption with External Public Test " + testnum
				+ "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		
		encryptTimeValues[testnum - 1] = tempTime;
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
		return respApdu;
	}
	
	public byte[] decryptTest(int testnum, byte[] _data) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_DECRYPT_TEST, P1,
				P2, _data, Le);
		testerInstance.logText("\nRSA Decryption with Own Private Test " + testnum
				+ "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		
		decryptTimeValues[testnum - 1] = tempTime;
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
		return respApdu;
	}
	
	public byte[] signTest(int testnum, byte[] _data) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_SIGN_TEST, P1,
				P2, _data, Le);
		testerInstance.logText("\nRSA sign with Own Private Test " + testnum
				+ "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		
		signTimeValues[testnum - 1] = tempTime;
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
		return respApdu;
	}
	 
	public byte[] verifyTest(int testnum, byte[] _signature, byte[] _data) {
		
		/**
		 *  first update the signature to be verified ... before the actual verification test
		 */
		
		byte[] data = Converter.concatArray(_signature, _data);
	 
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_VERIFY_TEST, P1,
				P2, data, Le);
		testerInstance.logText("\nRSA verify with External public Test " + testnum 
				+ "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		
		verifyTimeValues[testnum - 1] = tempTime;
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
		return respApdu;
	}
 
}

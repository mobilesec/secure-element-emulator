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
public class ECDHTest {

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

	private final static byte INS_KEYPAIR_GEN_TEST = 0x10;
	private final static byte INS_CARD_PUBLIC_TEST = 0x11;
	private final static byte INS_EXT_PUB_TEST = 0x12;
	private final static byte INS_SHARED_SEC_GEN_TEST = 0x13;
	private final static byte INS_COMPLETE_TEST = 0x14;

	private final static byte P1 = 0x00;
	private final static byte P2 = 0x00;
	private final static byte Le = 0x00;

	private boolean runTest = false;
	private Thread testThread;
	private int loop = 0;
	private int count = 0;

	private long completeECDHAvgTime = 0L;
	private long keypairGenAvgTime = 0L;
	private long getCardPublicAvgTime = 0L;
	private long setExtPublicAvgTime = 0L;
	private long genShareSecretAvgTime = 0L;

	private long completeECDHTime = 0L;
	private long keypairGenTime = 0L;
	private long getCardPublicTime = 0L;
	private long setExtPublicTime = 0L;
	private long genShareSecretTime = 0L;

	private long completeECDHTimeMedian = 0L;
	private long keypairGenTimeMedian = 0L;
	private long getCardPublicTimeMedian = 0L;
	private long setExtPublicMedian = 0L;
	private long genShareSecretTimeMedian = 0L;

	private long[] completeECDHTimeValues;
	private long[] keypairGenTimeValues;
	private long[] getCardPublicValues;
	private long[] setExtPublicValues;
	private long[] genShareSecretValues;

	private long completeECDHWorstTime = 0L;
	private long keypairGenWorstTime = 0L;
	private long getCardPublicWorstTime = 0L;
	private long setExtPublicWorstTime = 0L;
	private long genShareSecretWorstTime = 0L;

	private long tempTime;
	private TestPerformerActivity testerInstance;

	public ECDHTest(TestPerformerActivity _testerInstance, SEConnection _seConn) {
		testerInstance = _testerInstance;
		seConn = _seConn;
	}

	public void closeConnection() {
		seConn.closeConnection();
	}

	public void startTest(int _loop) {
		loop = _loop;
		runTest = true;
		count = 0;

		completeECDHTimeValues = new long[loop];
		keypairGenTimeValues = new long[loop];
		getCardPublicValues = new long[loop];
		setExtPublicValues = new long[loop];
		genShareSecretValues = new long[loop];
		
		Arrays.fill(completeECDHTimeValues, Integer.MAX_VALUE);
		Arrays.fill(keypairGenTimeValues, Integer.MAX_VALUE);
		Arrays.fill(getCardPublicValues, Integer.MAX_VALUE);
		Arrays.fill(setExtPublicValues, Integer.MAX_VALUE);
		Arrays.fill(genShareSecretValues, Integer.MAX_VALUE);

		testThread = new Thread() {

			public void run() {
				while (runTest && loop > 0) {

					count += 1;
					keyPairGenTest(count);
					getCardPublicTest(count);
					setExtPublicTest(count);
					secretValueGenTest(count);
					completeTest(count);
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
		completeECDHTime = 0L;
		keypairGenTime = 0L;
		getCardPublicTime = 0L;
		setExtPublicTime = 0L;
		genShareSecretTime = 0L;

		completeECDHWorstTime = 0L;
		keypairGenWorstTime = 0L;
		getCardPublicWorstTime = 0L;
		setExtPublicWorstTime = 0L;
		genShareSecretWorstTime = 0L;

		completeECDHTimeMedian = 0L;
		keypairGenTimeMedian = 0L;
		getCardPublicTimeMedian = 0L;
		setExtPublicMedian = 0L;
		genShareSecretTimeMedian = 0L;
	}

	public void perfomanceSummary() {

		for (int i = 0; i < count; i++) {
			completeECDHTime += completeECDHTimeValues[i];
			keypairGenTime += keypairGenTimeValues[i];
			getCardPublicTime += getCardPublicValues[i];
			setExtPublicTime += setExtPublicValues[i];
			genShareSecretTime += genShareSecretValues[i];
		}

		// long[] sortedValue =
		Arrays.sort(completeECDHTimeValues);
		Arrays.sort(keypairGenTimeValues);
		Arrays.sort(getCardPublicValues);
		Arrays.sort(setExtPublicValues);
		Arrays.sort(genShareSecretValues);

		// worst time
		completeECDHWorstTime = completeECDHTimeValues[count-1];
		keypairGenWorstTime = keypairGenTimeValues[count-1];
		getCardPublicWorstTime = getCardPublicValues[count-1];
		setExtPublicWorstTime = setExtPublicValues[count-1];
		genShareSecretWorstTime = genShareSecretValues[count-1];
		
		// median calculation
		int mid = count / 2;

		if (count % 2 != 0) {

			completeECDHTimeMedian = completeECDHTimeValues[mid];
			keypairGenTimeMedian = keypairGenTimeValues[mid];
			getCardPublicTimeMedian = getCardPublicValues[mid];
			setExtPublicMedian = setExtPublicValues[mid];
			genShareSecretTimeMedian = genShareSecretValues[mid];

		} else {

			completeECDHTimeMedian = (completeECDHTimeValues[mid - 1] + completeECDHTimeValues[mid]) / 2;
			keypairGenTimeMedian = (keypairGenTimeValues[mid - 1] + keypairGenTimeValues[mid]) / 2;
			getCardPublicTimeMedian += (getCardPublicValues[mid - 1] + getCardPublicValues[mid]) / 2;
			setExtPublicMedian = (setExtPublicValues[mid - 1] + setExtPublicValues[mid]) / 2;
			genShareSecretTimeMedian = (genShareSecretValues[mid - 1] + genShareSecretValues[mid]) / 2;

		}

		completeECDHAvgTime = completeECDHTime / count;
		keypairGenAvgTime = keypairGenTime / count;
		getCardPublicAvgTime = getCardPublicTime / count;
		setExtPublicAvgTime = setExtPublicTime / count;
		genShareSecretAvgTime = genShareSecretTime / count;

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
		testerInstance.logText("\n--- Generate Shared Secret \n   Average : ->"
				+ genShareSecretAvgTime + " usec    Worst: ->"
				+ genShareSecretWorstTime + " usec \n   Median  : ->"
				+ genShareSecretTimeMedian + " usec");
		testerInstance.logText("\n--- Complete ECDH \n   Average : ->"
				+ completeECDHAvgTime + " usec    Worst: ->"
				+ completeECDHWorstTime + " usec \n   Median  : ->"
				+ completeECDHTimeMedian + " usec");

		testerInstance
				.logText("\n==================================================");
		testerInstance.logText("end");

	}

	public void stopTest() {
		runTest = false;
	}

	public boolean testRunning() {
		return runTest;
	}

	public void completeTest(int testnum) {
		testerInstance.logText("\nComplete ECDH Key Agreement Test " + testnum
				+ "\n--------------------------------------------------");

		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_COMPLETE_TEST, P1, P2,
				testPublicParam, Le);
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		completeECDHTimeValues[testnum - 1] = tempTime;
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
	}

	public void keyPairGenTest(int testnum) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_KEYPAIR_GEN_TEST, P1, P2,
				new byte[0], Le);
		testerInstance.logText("\nEC Keypair Generation Test " + testnum
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

	public void getCardPublicTest(int testnum) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_CARD_PUBLIC_TEST, P1, P2,
				new byte[0], (byte) 0x31);
		testerInstance.logText("\nEC Get Card Public Param Test " + testnum
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
	}

	public void setExtPublicTest(int testnum) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_EXT_PUB_TEST, P1, P2,
				testPublicParam, Le);
		testerInstance.logText("\nEC Set External Public Param Test " + testnum
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

	public void secretValueGenTest(int testnum) {
		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_SHARED_SEC_GEN_TEST, P1,
				P2, new byte[0], Le);
		testerInstance.logText("\nShared Secret Generation Test " + testnum
				+ "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		
		genShareSecretValues[testnum - 1] = tempTime;
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime>" + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
	}

}

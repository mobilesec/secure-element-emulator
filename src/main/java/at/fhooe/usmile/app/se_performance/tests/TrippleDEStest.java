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

public class TrippleDEStest {

	private final static byte CLA = (byte) 0x80;

	private final static byte INS_ENCRYPT_TEST = 0x51;
	private final static byte INS_DECRYPT_TEST = 0x52;


	private final static byte P1 = 0x00;
	private final static byte P2 = 0x00;
	private final static byte Le = 0x00;

	private boolean runTest = false;

	private Thread testThread;
	private int loop = 0;
	private int count = 0;

	private long encryptAvgTime = 0L;
	private long decryptAvgTime = 0L;

	private long encryptTime = 0L;
	private long decryptTime = 0L;

	private long encryptMedian = 0L;
	private long decryptMedian = 0L;

	private long encryptWorstTime = 0L;
	private long decryptWorstTime = 0L;

	private long tempTime;
	private SEConnection seConn;
	private TestPerformerActivity testerInstance;


	private long[] encryptTimeValues;
	private long[] decryptTimeValues;

	public TrippleDEStest(TestPerformerActivity _testerInstance, SEConnection _seConn) {
		testerInstance = _testerInstance;
		seConn = _seConn;
	}
	
	public void startTest(int _loop, byte[] _data) {
		final byte[] testData = _data;
		loop = _loop;
		runTest = true;
		count = 0;
		encryptTimeValues = new long[loop];
		decryptTimeValues = new long[loop];
		
		Arrays.fill(encryptTimeValues, Integer.MAX_VALUE);
		Arrays.fill(decryptTimeValues, Integer.MAX_VALUE);
		
		testThread = new Thread() {

			public void run() {
				byte[] cipher = new byte[0];
				byte[] resp;
				while (runTest && loop > 0) {
					count += 1;
					if (count > 1) {
						resp = encryptTest(count, cipher);
					} else {
						resp = encryptTest(count, testData);
					}
					cipher = new byte[resp.length - 2];
					System.arraycopy(resp, 0, cipher, 0, cipher.length);
					decryptTest(count, cipher);

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
		encryptAvgTime = 0L;
		decryptAvgTime = 0L;

		encryptTime = 0L;
		decryptTime = 0L;

		encryptMedian = 0L;
		decryptMedian = 0L;

		encryptWorstTime = 0L;
		decryptWorstTime = 0L;
	}

	public void perfomanceSummary() {

		for (int i = 0; i < count; i++) {
			encryptTime += encryptTimeValues[i];
			decryptTime += decryptTimeValues[i];
		}
		
		//long[] sortedValue = 
		Arrays.sort(encryptTimeValues);
		Arrays.sort(decryptTimeValues);
		
		
		// worst
		encryptWorstTime = encryptTimeValues[count -1];
		decryptWorstTime = decryptTimeValues[count -1];
		
		// median
		int mid = count / 2;
		if (count % 2 != 0) {
			encryptMedian = encryptTimeValues[mid];
			decryptMedian = decryptTimeValues[mid];
		} else {
			encryptMedian = (encryptTimeValues[mid - 1] + encryptTimeValues[mid]) / 2;
			decryptMedian = (encryptTimeValues[mid - 1] + decryptTimeValues[mid]) / 2;
		}

		encryptAvgTime = encryptTime / count;
		decryptAvgTime = decryptTime / count;

		// display summary

		testerInstance.logText("\n--3-DES--Performance Summary ---- iterations : "
				+ count);
		testerInstance
				.logText("\n==================================================");

		testerInstance.logText("\n--- Encryption \n   Average : ->"
				+ encryptAvgTime + " usec    Worst: ->" + encryptWorstTime
				+ " usec \n   Median  : ->" + encryptMedian + " usec");
		testerInstance.logText("\n--- Decryption  \n   Average : ->"
				+ decryptAvgTime + " usec    Worst: ->" + decryptWorstTime
				+ " usec \n   Median  : ->" + decryptMedian + " usec");

		 
		
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

	public byte[] encryptTest(int testnum, byte[] _data) {

		testerInstance.logText("\nEncrypt Test " + testnum
				+ "\n--------------------------------------------------");
		byte[]  cmdApdu = APDU.getCommandApdu(CLA, INS_ENCRYPT_TEST, P1, P2,
					_data, Le);
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
	  
		// encryptTime += tempTime;

		encryptTimeValues[testnum - 1] = tempTime;
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime -> " + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");

		return respApdu;

	}

	public void decryptTest(int testnum, byte[] _data) {
		 
		byte[]  cmdApdu = APDU.getCommandApdu(CLA, INS_DECRYPT_TEST, P1, P2,
				_data, Le);
		testerInstance.logText("\nDecrypt Test " + testnum
				+ "\n--------------------------------------------------");
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 

		// decryptTime += tempTime;

		decryptTimeValues[testnum - 1] = tempTime;

		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsedTime -> " + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");
	}

}

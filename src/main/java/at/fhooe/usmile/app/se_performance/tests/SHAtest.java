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
public class SHAtest {
	private final static byte CLA = (byte) 0x80;

	private final static byte INS_SHA_TEST = (byte) 0x26;

	private final static byte P1 = 0x00;
	private final static byte P2 = 0x00;
	private final static byte Le = 0x00;

	private boolean runTest = false;

	private Thread testThread;
	private int loop = 0;
	private int count = 0;

	private long shaAvgTime = 0L;

	private long shaTime = 0L;

	private long shaWorstTime = 0L;

	private long tempTime;
	private SEConnection seConn;

	private TestPerformerActivity testerInstance;

	private long shaMedian = 0L;

	private long[] shaTimeValues;

	public SHAtest(TestPerformerActivity _testerInstance, SEConnection _seConn) {
		testerInstance = _testerInstance;
		seConn = _seConn;
	}

	public void startTest(int _loop, byte[] _data) {
		final byte[] testData = _data;
		loop = _loop;
		runTest = true;
		count = 0;
		shaTimeValues = new long[loop];
		Arrays.fill(shaTimeValues, Integer.MAX_VALUE);
		testThread = new Thread() {

			public void run() {

				while (runTest && loop > 0) {
					count += 1;
					digestTest(count, testData);
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
		shaAvgTime = 0L;

		shaTime = 0L;

		shaWorstTime = 0L;

		shaMedian = 0L;
	}

	public void perfomanceSummary() {

		for (int i = 0; i < count; i++) {
			shaTime += shaTimeValues[i];

		}

		Arrays.sort(shaTimeValues);

		// worst
		shaWorstTime = shaTimeValues[count - 1];

		// median
		int mid = count / 2;
		if (count % 2 != 0) {
			shaMedian = shaTimeValues[mid];

		} else {
			shaMedian = (shaTimeValues[mid - 1] + shaTimeValues[mid]) / 2;

		}

		shaAvgTime = shaTime / count;

		// display summary

		testerInstance.logText("\n----Performance Summary ---- iterations : "
				+ count);
		testerInstance
				.logText("\n==================================================\n");

		testerInstance.logText("\n--- SHA-256 \n   Average : ->" + shaAvgTime
				+ " usec    Worst: ->" + shaWorstTime + " usec \n   Median  : ->" + shaMedian + " usec");

		testerInstance
				.logText("\n==================================================\n");
		testerInstance.logText("end");

	}

	public void stopTest() {
		runTest = false;
	}

	public boolean testRunning() {
		return runTest;
	}

	public byte[] digestTest(int testnum, byte[] _data) {

		testerInstance.logText("\nSHA-256 Digest Test " + testnum
				+ "\n--------------------------------------------------");

		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_SHA_TEST, P1, P2, _data,
				Le);
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		shaTimeValues[testnum-1] = tempTime;
		testerInstance.logText("\nResponse<- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsed Time -> " + tempTime + " micro sec.");
		testerInstance
				.logText("\n==================================================\n");

		return respApdu;

	}

}

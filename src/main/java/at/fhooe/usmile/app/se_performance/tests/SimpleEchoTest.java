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
public class SimpleEchoTest {
	private final static byte CLA = (byte) 0x80;

	private final static byte INS_ECHO_TEST = 0x53;

	private final static byte P1 = 0x00;
	private final static byte P2 = 0x00;
	private final static byte Le = 0x00;

	private boolean runTest = false;

	private Thread testThread;
	private int loop = 0;
	private int count = 0;

	private long zeroEchoAvgTime = 0L;
	
	private long zeroEchoTimeMedian = 0L;
	
	private long[] zeroEchoTimeValues;

	private long zeroEchoTime = 0L;

	private long zeroEchoWorstTime = 0L;
	private long zeroEchoBestTime = 0L;

	private long tempTime;
	private SEConnection seConn;

	private TestPerformerActivity testerInstance;

	public SimpleEchoTest(TestPerformerActivity _testerInstance, SEConnection _seConn){
		testerInstance = _testerInstance;
		seConn = _seConn;
	}

	public void startTest(int _loop, final byte[] _data) {
		loop = _loop;
		runTest = true;
		count = 0;
		
		zeroEchoTimeValues = new long[loop];
		
		Arrays.fill(zeroEchoTimeValues, Integer.MAX_VALUE);
		
		testThread = new Thread() {

			public void run() { 
				while (runTest && loop > 0) {
					count += 1;
					
					echoTest(count, _data);
					 
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
		zeroEchoAvgTime = 0L;

		zeroEchoTime = 0L;

		zeroEchoWorstTime = 0L;
		zeroEchoBestTime = 0L;
	}

	public void perfomanceSummary() {
		for (int i = 0; i < count; i++) {
			zeroEchoTime += zeroEchoTimeValues[i];
		}
		
		
		
		//long[] sortedValue = 
		Arrays.sort(zeroEchoTimeValues);
		
		// worst time
		zeroEchoBestTime = zeroEchoTimeValues[0];
		zeroEchoWorstTime = zeroEchoTimeValues[count -1];
		
		// median 
		int mid = count / 2;
		
		if (count % 2 != 0) {
			zeroEchoTimeMedian = zeroEchoTimeValues[mid];
		} else {
			zeroEchoTimeMedian = (zeroEchoTimeValues[mid -1] + zeroEchoTimeValues[mid])/2;
		}
		
		zeroEchoAvgTime = zeroEchoTime / count;

		// display summary

		testerInstance.logTextSpecial("\n==================================================" + 
                                      "\n----SimpleEchoTestSummary---- iterations : " + count +
                                      "\n==================================================" +
                                      "\nBest\tAverage\tMedian\tWorst\t(in usec)" +
                                      "\n" + zeroEchoBestTime + "\t" + zeroEchoAvgTime + "\t" + zeroEchoTimeMedian + "\t" + zeroEchoWorstTime + "" +
                                      "\n==================================================" +
                                      "end");

	}

	public void stopTest() {
		runTest = false;
	}

	public boolean testRunning() {
		return runTest;
	}

	public void echoTest(int testnum, byte[] data) {

		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_ECHO_TEST, P1, P2,
				data, Le);
		testerInstance.logText("\nSimple Echo Test " + testnum +  "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
        zeroEchoTimeValues[testnum -1] = tempTime;
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsed Time -> " + tempTime
				+ " micro sec.");
		testerInstance
		.logText("\n==================================================\n");

	}

}

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
public class RoundTripTest {
	private final static byte CLA = (byte) 0x80;

	private final static byte INS_SEND_TEST = 0x51;
	private final static byte INS_RECIEVE_TEST = 0x52;
	private final static byte INS_ECHO_TEST = 0x53;

	private final static byte P1 = 0x00;
	private final static byte P2 = 0x00;
	private final static byte Le = 0x00;

	private boolean runTest = false;

	private Thread testThread;
	private int loop = 0;
	private int count = 0;

	private long sendAvgTime = 0L;
	private long receiveAvgTime = 0L;
	private long echoAvgTime = 0L;
	private long zeroEchoAvgTime = 0L;
	
	private long sendTimeMedian = 0L;
	private long receiveTimeMedian = 0L;
	private long echoTimeMedian = 0L;
	private long zeroEchoTimeMedian = 0L;
	
	private long[] sendTimeValues ;
	private long[] receiveTimeValues ;
	private long[] echoTimeValues ;
	private long[] zeroEchoTimeValues;

	private long sendTime = 0L;
	private long receiveTime = 0L;
	private long echoTime = 0L;
	private long zeroEchoTime = 0L;

	private long sendWorstTime = 0L;
	private long receiveWorstTime = 0L;
	private long echoWorstTime = 0L;
	private long zeroEchoWorstTime = 0L;

	private long tempTime;
	private SEConnection seConn;

	private TestPerformerActivity testerInstance;

	public RoundTripTest(TestPerformerActivity _testerInstance, SEConnection _seConn){
		testerInstance = _testerInstance;
		seConn = _seConn;
	}

	public void startTest(int _loop, byte[] _data) {
		final byte[] testData = _data;
		loop = _loop;
		runTest = true;
		count = 0;
		
		sendTimeValues = new long[loop];
		receiveTimeValues = new long[loop];
		 echoTimeValues = new long[loop];
		zeroEchoTimeValues = new long[loop];
		
		Arrays.fill(sendTimeValues, Integer.MAX_VALUE);
		Arrays.fill(receiveTimeValues, Integer.MAX_VALUE);
		Arrays.fill(echoTimeValues, Integer.MAX_VALUE);
		Arrays.fill(zeroEchoTimeValues, Integer.MAX_VALUE);
		
		testThread = new Thread() {

			public void run() { 
				while (runTest && loop > 0) {
					count += 1;
					
					echoTest(count, new byte[0]);
					sendTest(count, testData);
					receiveTest(count, (byte)testData.length);
					echoTest(count, testData);
					 
					loop -= 1;

				}

				perfomanceSummary(testData.length);
				resetElapsedTime();
				runTest = false;
			}
		};
		testThread.start();
	}

	private void resetElapsedTime() {
		sendAvgTime = 0L;
		receiveAvgTime = 0L;
		echoAvgTime = 0L;
		zeroEchoAvgTime = 0L;

		sendTime = 0L;
		receiveTime = 0L;
		echoTime = 0L;
		zeroEchoTime = 0L;

		sendWorstTime = 0L;
		receiveWorstTime = 0L;
		echoWorstTime = 0L;
		zeroEchoWorstTime = 0L;
	}

	public void perfomanceSummary(int datalen) {
		for (int i = 0; i < count; i++) {
			zeroEchoTime += zeroEchoTimeValues[i];
			sendTime += sendTimeValues[i];
			receiveTime += receiveTimeValues[i];
			echoTime += echoTimeValues[i];
		}
		
		
		
		//long[] sortedValue = 
		Arrays.sort(zeroEchoTimeValues);
		Arrays.sort(sendTimeValues);
		Arrays.sort(receiveTimeValues);
		Arrays.sort(echoTimeValues);
		
		// worst time
		zeroEchoWorstTime = zeroEchoTimeValues[count -1];
		sendWorstTime= sendTimeValues[count -1];
		receiveWorstTime += receiveTimeValues[count -1];
		echoWorstTime += echoTimeValues[count -1];
		
		// median 
		int mid = count / 2;
		
		if (count % 2 != 0) {
			zeroEchoTimeMedian = zeroEchoTimeValues[mid];
			sendTimeMedian = sendTimeValues[mid];
			receiveTimeMedian += receiveTimeValues[mid];
			echoTimeMedian += echoTimeValues[mid];
		} else {
			zeroEchoTimeMedian = (zeroEchoTimeValues[mid -1] + zeroEchoTimeValues[mid])/2;
			sendTimeMedian = (sendTimeValues[mid-1] + sendTimeValues[mid])/2;
			receiveTimeMedian = (receiveTimeValues[mid-1] + receiveTimeValues[mid])/2;
			echoTimeMedian = (echoTimeValues[mid -1] + echoTimeValues[mid])/2;
			 
		}
		
		zeroEchoAvgTime = zeroEchoTime / count;
		sendAvgTime = sendTime / count;
		receiveAvgTime = receiveTime / count;
		echoAvgTime = echoTime / count;

		// display summary

		testerInstance.logText("\n----Performance Summary ---- iterations : " + count );
		testerInstance
		.logText("\n==================================================");

		testerInstance.logText("\n--- 0 Bytes Echo \n   Average : ->"
				+ zeroEchoAvgTime + " usec    Worst: ->" + zeroEchoWorstTime
				+ " usec \n   Median  : ->" + zeroEchoTimeMedian + " usec");
		testerInstance.logText("\n---" + datalen + " bytes Sending \n   Average : ->"
				+ sendAvgTime + " usec    Worst: ->" + sendWorstTime + " usec \n   Median  : ->" + sendTimeMedian + " usec");
		testerInstance.logText("\n---" + datalen + " bytes Receiving  \n   Average : ->"
				+ receiveAvgTime + " usec    Worst: ->" + receiveWorstTime
				+ " usec \n   Median  : ->" + receiveTimeMedian + " usec");

		testerInstance.logText("\n---" + datalen + " bytes echo  \n   Average : ->"
				+ echoAvgTime + " usec    Worst: ->" + echoWorstTime + " usec \n   Median  : ->" + echoTimeMedian + " usec");

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

	public void sendTest(int testnum, byte[] _data) {

		testerInstance.logText("\n" + _data.length + " bytes Send Test " + testnum +  "\n--------------------------------------------------");

		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_SEND_TEST, P1, P2, _data,
				Le);
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		sendTimeValues[testnum -1] = tempTime; 
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsed Time -> " + tempTime
				+ " micro sec.");
		testerInstance
		.logText("\n==================================================\n");

	}

	public void receiveTest(int testnum, byte length) {

		testerInstance.logText("\n" + length + " bytes Receive Test " + testnum +  "\n--------------------------------------------------");

		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_RECIEVE_TEST, P1, P2,
				new byte[0], length);
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		 
		receiveTimeValues[testnum -1] = tempTime;
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsed Time -> " + tempTime
				+ " micro sec.");
		testerInstance
		.logText("\n==================================================\n");
	}

	public void echoTest(int testnum, byte[] data) {

		byte[] cmdApdu = APDU.getCommandApdu(CLA, INS_ECHO_TEST, P1, P2,
				data, Le);
		testerInstance.logText("\n" + data.length + " Bytes Echo Test " + testnum +  "\n--------------------------------------------------");
		testerInstance.logText("\nCommand ->  "
				+ Converter.byteArrayToHexString(cmdApdu));
		byte[] respApdu = seConn.sendCommand(cmdApdu);
		tempTime = seConn.getElapsedTime() / 1000;
		if (data.length > 0) {
		 
			echoTimeValues[testnum -1] = tempTime;
		} else {
			 
			zeroEchoTimeValues[testnum -1] = tempTime;		}
		
		testerInstance.logText("\nResponse <- "
				+ Converter.byteArrayToHexString(respApdu));
		testerInstance.logText("\nElapsed Time -> " + tempTime
				+ " micro sec.");
		testerInstance
		.logText("\n==================================================\n");

	}

}

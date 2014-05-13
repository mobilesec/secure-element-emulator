package at.fhooe.usmile.app.se_performance.connection;

import at.mroland.logging.Logging;
import at.mroland.utils.ArrayUtils;
import com.licel.jcardsim.base.SimulatorConfig;
import com.licel.jcardsim.base.SimulatorSystem;



public class SEConnection {

	byte[] AID;
	 
 
	
	long elapsedTime = 0L;
	
	
	public SEConnection(byte[] aid  ){		
			AID = aid;
            if (AID != null) {
	 	selectApplet(AID, 0); 
            }
	}
	
	public void closeConnection(){
	}
    
	public final boolean selectApplet(byte[] _aid, int _readerIndex){
		
        SimulatorSystem.transceiveAPDU(SimulatorConfig.INTERFACE_INTERNAL_NAME, ArrayUtils.concatenate(new byte[]{ (byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)(_aid.length & 0x0ff) }, _aid ));
		return true;
	}
	
	public byte[] sendCommand(byte[] cmdApdu){
	    	byte[] respApdu = new byte[0];
			
	    	try {
				
				long commandTime = 0L;
				long responseTime = 0L;
				commandTime = System.nanoTime();
				respApdu = SimulatorSystem.transceiveAPDU(SimulatorConfig.INTERFACE_INTERNAL_NAME, cmdApdu);
				responseTime = System.nanoTime();
				elapsedTime = responseTime - commandTime;
				
			 				 
			} catch (Exception e) {
				Logging.error("SEConnection", "Exception: " + e.toString(), e);
			}
	    	return respApdu;
	}
	
	public long getElapsedTime(){
		return elapsedTime;
	}

}

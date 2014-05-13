package at.fhooe.usmile.app.se_performance.util;



public class APDU {

	  public static byte[] getCommandApdu(byte CLA, byte INS, byte P1, byte P2, byte[] data, byte Le){
	       
	        byte[] apduByte = null;
	        if(data.length == 0){
	            apduByte = new byte[5];
	        }else{
	            apduByte = new byte[data.length + 6];
	        }
	        apduByte[0] = CLA;
	        apduByte[1] = INS;
	        apduByte[2] = P1;
	        apduByte[3] = P2;
	        if(data.length > 0){
	             apduByte[4] = (byte) data.length;
	        System.arraycopy(data, 0, apduByte, 5, data.length);
	        apduByte[5 + data.length] = Le;
	        }else{
	            apduByte[4] = Le;
	        }
	       
	        
	        return apduByte;
	    }
}

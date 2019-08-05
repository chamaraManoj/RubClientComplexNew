package com.example.rubclientcomplexnew;

import android.util.Log;

import java.nio.ByteBuffer;

public class BufferManager {

    private int NETWORK_RELATED_PARA    = 6;
    private int CHUNK_RELATED_DATA      = 9;
    
    /**Temporarly variables to store the quality level*/
    private byte QUALITY_SD              = 0;
    private byte QUALITY_HD              = 1;
    private byte QUALITY_4K              = 2;
    /**
     * Declare the network related parameters
     */
    //private String serverIp = "10.1.1.35";
    private String serverIp = "10.130.1.229";
    private String socketSend  = "5550";
    private String socketRecv1 = "5551";
    private String socketRecv2 = "5552";
    private String socketRecv3 = "5553";
    private String socketRecv4 = "5554";

    private String[] networkRelatedPara;
    private byte[] chunkDataBuffer;

    ModuleTask mModuleTask;

    /**
     * Return the network related details server ip address and the
     * Socket port numbers as String variables
     */
    public String[] getNetworkComponentData() {
        networkRelatedPara = new String[NETWORK_RELATED_PARA];

        networkRelatedPara[0] = serverIp;
        networkRelatedPara[1] = socketSend;
        networkRelatedPara[2] = socketRecv1;
        networkRelatedPara[3] = socketRecv2;
        networkRelatedPara[4] = socketRecv3;
        networkRelatedPara[5] = socketRecv4;

        return networkRelatedPara;
    }

    /**
     * Function to request the frame related data
     */
    public byte[] getChunkData() {

        int chunk;
        chunkDataBuffer = new byte[CHUNK_RELATED_DATA];
        
        /**This is s temporarly created for loop to test the implementing methods*/

        for (chunk = 0; chunk < 1; chunk++) {
            byte tile1 = 19;
            byte tile2 = (byte)(chunk%15);
            byte tile3 = 11;
            byte tile4 = (byte)(chunk%15);
            byte quality = QUALITY_SD;

            byte[] tempChunkBytes = ByteBuffer.allocate(4).putInt(chunk).array();

            chunkDataBuffer[0] = tempChunkBytes[0];
            chunkDataBuffer[1] = tempChunkBytes[1];
            chunkDataBuffer[2] = tempChunkBytes[2];
            chunkDataBuffer[3] = tempChunkBytes[3];

            chunkDataBuffer[4] = tile1;
            chunkDataBuffer[5] = tile2;
            chunkDataBuffer[6] = tile3;
            chunkDataBuffer[7] = tile4;
            chunkDataBuffer[8] = quality;
            
        }
        return chunkDataBuffer;
    }

    public void startSendRequest(){
        Log.d("Taggg", "2");
        mModuleTask = ModuleManager.sendRequest(this,true);
    }


}

package com.example.rubclientcomplexnew;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class DownloadDataRunnable implements Runnable {

    static final int DOWNLOAD_STATE_STARTED = 2;
    static final int DOWNLOAD_STATE_COMPLETED = 3;
    static final int DOWNLOAD_STATE_FAILED = -2;

    private static int BYTE_BUFFER_LENGTH_BASE = 150000;
    private static int BYTE_BUFFER_LENGTH_ENHA = 40000;


    private static int NUM_OF_TILES_BASE = 20;
    private static int NUM_OF_TILES_ENHA = 4;


    private final int threadNum;

    final TaskRunnableDownloadMethods mModuleTask;

    interface TaskRunnableDownloadMethods {

        /**
         * Sets the Thread that this instance is running on
         *
         * @param currentThread the current Thread
         */
        void setSendThread(Thread currentThread);

        /**
         * Get the current content of the
         */
        byte[] getDataBuffer(int threadNum);


        /**
         * Returns the current socket array references defined in the ModuleTask
         */
        Socket getSocket(int threadNum);


        /**
         * Defines the actions for each state of the PhotoTask instance.
         *
         * @param state The current state of the task
         */
        void handleSendState(int state);

        /**
         * Set the downloaded bytebuffer
         */
        void setByteBuffer(byte[] downloadedBuffer, int threadNum);

        /**
         * Return buffer to store the tiles data lengths
         */
        int[] getTileLengthBuffer(int threadNum);

        /**
         * Set the buffer containing the tile's lengths
         */
        void setTileLengthsBuffer(int[] tileLengthsBuffer, int threadNum);


    }

    DownloadDataRunnable(TaskRunnableDownloadMethods moduleTask, int threadNum) {
        this.threadNum = threadNum;
        this.mModuleTask = moduleTask;
    }

    @Override
    public void run() {
        Log.d("Taggg", "18");

        /**Byte reading related data*/
        int bytesRead = 0;
        int totBytesRead = 0;
        int layerLength = 0;
        int totBufferLength;
        int totTiles;

        int tempCounter;

        Log.d("Taggg", "19");

        /**Update the handlestate in module task : downloading is going to be started*/
        mModuleTask.handleSendState(DOWNLOAD_STATE_STARTED);

        Log.d("Taggg", "20");

        /**Set the current thread that the object running for any thread interruption*/
        mModuleTask.setSendThread(Thread.currentThread());

        /**Moves the current Thread into the background*/
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        byte[] byteReadData = mModuleTask.getDataBuffer(this.threadNum);

        int[] tileLengths = mModuleTask.getTileLengthBuffer(this.threadNum);

        Socket socket = mModuleTask.getSocket(this.threadNum);

        Log.d("Taggg", "21");
        Log.d("Taggg", socket.toString());

        if (socket != null) {

            InputStream inputStream = null;
            try {

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                /**inititalize the inputstream using the socket input stream*/
                inputStream = socket.getInputStream();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

                Log.d("Taggg", "22");

                /**If the thread is working with base layer transmission*/
                if (this.threadNum == ModuleManager.BASE_LAYER) {
                    totBufferLength = BYTE_BUFFER_LENGTH_BASE;
                    totTiles = NUM_OF_TILES_BASE;
                }
                /**If the thread is working with one of the enhancement layers*/
                else {
                    totBufferLength = BYTE_BUFFER_LENGTH_ENHA;
                    totTiles = NUM_OF_TILES_ENHA;
                }

                /**Initializring the new buffer for store the read data*/
                byteReadData = new byte[totBufferLength];

                /**Reading the first bytes containing the */
                bytesRead = bufferedInputStream.read(byteReadData, 0, totTiles * 2);

                totBytesRead = bytesRead;

                Log.d("Taggg", "23");

                /**Reading the lengths of each tile in the given layer*/
                /**If read the byte data*/
                if(bytesRead>0){
                    for (tempCounter = 0; tempCounter < totTiles; tempCounter++) {
                        tileLengths[tempCounter] = (byteReadData[tempCounter * 2] & 0xFF) << 8 | byteReadData[tempCounter * 2 + 1] & 0xFF;
                        layerLength += tileLengths[tempCounter];
                    }
                }

                Log.d("Taggg", "24");

                 /**Giving the total length of the byte array*/
                int endOfByte = totBytesRead +layerLength;

                /**Filling the data to the buffer*/
                do {
                    bytesRead = bufferedInputStream.read(byteReadData, totBytesRead, endOfByte - totBytesRead);
                    if (bytesRead >= 0){
                        totBytesRead += bytesRead;
                    }
                    //Log.d("Debug", "Temp Byte " + String.valueOf(tempByteRead) + "\n");
                } while (bytesRead > 0);

                Log.d("Taggg", String.valueOf(totBytesRead));
                Log.d("Taggg", "25");

                /**Closing the socket*/
                socket.close();

                /**Sets the read buffer to the buffer in the moduletask*/
                mModuleTask.setByteBuffer(byteReadData, threadNum);
                mModuleTask.handleSendState(DOWNLOAD_STATE_COMPLETED);

            } catch (InterruptedException | IOException e ) {
                e.printStackTrace();
                mModuleTask.handleSendState(DOWNLOAD_STATE_FAILED);
            }
        }
    }
}

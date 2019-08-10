package com.example.rubclientcomplexnew;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class DownloadDataRunnable implements Runnable {

    static final int DOWNLOAD_STATE_STARTED = 2;
    static final int DOWNLOAD_STATE_COMPLETED = 3;
    static final int DOWNLOAD_STATE_FAILED = -2;

    private static int BYTE_BUFFER_LENGTH_BASE = 150000;
    private static int BYTE_BUFFER_LENGTH_ENHA = 40000;
    private static int BYTE_BUFFER_LENGTH_TOT = BYTE_BUFFER_LENGTH_BASE + BYTE_BUFFER_LENGTH_ENHA;

    private static int NUM_OF_TILES_BASE = 20;
    private static int NUM_OF_TILES_ENHA = 4;
    private static int NUM_OF_TILES_TOT = 32;
    private static int NUM_OF_CHUNK_BYTE = 4;

    private final int threadNum;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;

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
        void handleSendState(int state, int threadNum);

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

    DownloadDataRunnable(TaskRunnableDownloadMethods moduleTask, int threadNum, CountDownLatch startSignal, CountDownLatch doneSignal) {
        this.threadNum = threadNum;
        this.mModuleTask = moduleTask;

        this.startSignal = startSignal;
        this.doneSignal = doneSignal;
    }

    @Override
    public void run() {
        //Log.d("Taggg", "18");
        //Log.d("Taggg", startSignal.toString()+"  18");
        try {
            startSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /**Byte reading related data*/
        int bytesRead = 0;
        int totBytesRead = 0;
        int layerLength = 0;
        int totBufferLength;
        int totTiles;
        int chunk;
        int skippedVal;

        int tempCounter;

        //Log.d("Taggg", "19");

        /**Update the handlestate in module task : downloading is going to be started*/
        if (this.threadNum == ModuleManager.BASE_LAYER)
            mModuleTask.handleSendState(DOWNLOAD_STATE_STARTED, this.threadNum);

        //Log.d("Taggg", "20");

        /**Set the current thread that the object running for any thread interruption*/
        mModuleTask.setSendThread(Thread.currentThread());

        /**Moves the current Thread into the background*/
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        byte[] byteReadData = mModuleTask.getDataBuffer(this.threadNum);
        int[] tileLengths = mModuleTask.getTileLengthBuffer(this.threadNum);
        //Log.d("Taggg", String.valueOf(this.threadNum) + " 21");

        Socket socket = mModuleTask.getSocket(this.threadNum);


        //Log.d("Taggg", String.valueOf(this.threadNum) +  socket.toString());

        if (socket != null) {

            InputStream inputStream = null;

            try {

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                /**inititalize the inputstream using the socket input stream*/
                inputStream = socket.getInputStream();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

                //Log.d("Taggg", String.valueOf(this.threadNum) +" 22");

                /**If the thread is working with base layer transmission*/
                if (this.threadNum == ModuleManager.BASE_LAYER && ModuleManager.ENA_PARALLEL_STREAM) {
                    totBufferLength = BYTE_BUFFER_LENGTH_BASE;
                    totTiles = NUM_OF_TILES_BASE;
                }
                /**if parallel thread is disabled and only one thread is working*/
                else if (this.threadNum == ModuleManager.BASE_LAYER && !ModuleManager.ENA_PARALLEL_STREAM) {
                    totBufferLength = BYTE_BUFFER_LENGTH_TOT;
                    totTiles = NUM_OF_TILES_TOT;
                }
                /**If the thread is working with one of the enhancement layers*/
                else {
                    totBufferLength = BYTE_BUFFER_LENGTH_ENHA;
                    totTiles = NUM_OF_TILES_ENHA;
                }

                /**Initializring the new buffer for store the read data*/
                byteReadData = new byte[totBufferLength];
                tileLengths = new int[totTiles];

                /**Reading the chunk number which is inthe first 4 bytes*/
                bytesRead = bufferedInputStream.read(byteReadData, 0, NUM_OF_CHUNK_BYTE);
                totBytesRead += bytesRead;
                chunk = (byteReadData[0] & 0xFF) << 24 |
                        byteReadData[1] & 0xFF<< 16|
                        byteReadData[2] & 0xFF << 8 |
                        byteReadData[3] & 0xFF;

                //Log.d("Taggg", "Layer length "+String.valueOf(totBytesRead));

                /**Reading the first bytes containing the */


                bytesRead = bufferedInputStream.read(byteReadData, 0, totTiles * 2);
                totBytesRead += bytesRead;
                //Log.d("Taggg", "Layer length "+String.valueOf(totBytesRead));
                //Log.d("Taggg", String.valueOf(this.threadNum) +" "+ String.valueOf(totBytesRead)+" 23");

                /**Reading the lengths of each tile in the given layer*/
                /**If read the byte data*/
                if (bytesRead > 0) {
                    for (tempCounter = 0; tempCounter < totTiles; tempCounter++) {
                        tileLengths[tempCounter] = (byteReadData[(tempCounter * 2)] & 0xFF) << 8 | byteReadData[tempCounter * 2 + 1] & 0xFF;
                        Log.d("Taggg","Tile Length :"+String.valueOf(tempCounter+1)+" "+ String.valueOf(tileLengths[tempCounter]));
                        layerLength += tileLengths[tempCounter];
                    }
                }

                Log.d("Taggg", "Layer length "+String.valueOf(totBytesRead));
                /**Giving the total length of the byte array*/
                int endOfByte = layerLength;
                int tempTotReadByte=0;
                /**Filling the data to the buffer*/
                do {
                    bytesRead = bufferedInputStream.read(byteReadData, 0, endOfByte - tempTotReadByte);

                    if (bytesRead >= 0) {
                        tempTotReadByte += bytesRead;
                    }
                    totBytesRead+=bytesRead;
                    Log.d("Taggg", "Temp Byte " + String.valueOf(totBytesRead) +" "+ String.valueOf(tempTotReadByte)+ " "+ String.valueOf(endOfByte - tempTotReadByte)+"\n");
                } while (bytesRead > 0);

                Log.d("Taggg", String.valueOf(this.threadNum) + " " + String.valueOf(totBytesRead));
                Log.d("Taggg", "25");

                /**Closing the socket*/
                socket.close();

                /**Sets the read buffer to the buffer in the moduletask*/

                mModuleTask.setByteBuffer(byteReadData, threadNum);

                /**Make the state change if and only if current working thread is on Baselayer transmission
                 * or prallel streaming is disabled*/
                if (this.threadNum == ModuleManager.BASE_LAYER)
                    mModuleTask.handleSendState(DOWNLOAD_STATE_COMPLETED, this.threadNum); // include the thread numb and chunk number;


            } catch (InterruptedException | IOException e) {
                e.printStackTrace();

                mModuleTask.handleSendState(DOWNLOAD_STATE_FAILED, this.threadNum);
            }
        }
        doneSignal.countDown();
    }
}

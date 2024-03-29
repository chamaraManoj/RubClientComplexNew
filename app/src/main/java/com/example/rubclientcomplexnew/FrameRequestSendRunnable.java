package com.example.rubclientcomplexnew;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class FrameRequestSendRunnable implements Runnable {

    /*Define the sending buffer length*/
    private static final int SEND_SIZE = 9;
    private static final int RECEIVED_CHANNELS = 4;
    private static final int TOT_SCOKETS = 5;

    /*Status indicating the SocketCommunication status*/
    static final int SOCKET_STATE_STARTED = 0;
    static final int SOCKET_STATE_COMPLETED = 1;
    static final int SOCKET_STATE_FAILED = -1;

    final TaskRunnableSendMethods mModuleTask;
    final CountDownLatch startSignal;
    final CountDownLatch donesignal;

    /*Interface to create methods needed to share variabled between the ModuleTask and the */
    interface TaskRunnableSendMethods {

        /**
         * Sets the Thread that this instance is running on
         *
         * @param currentThread the current Thread
         */
        void setSendThread(Thread currentThread);

        /**
         * Returns the current contents of the download buffer
         *
         * @return The byte array downloaded from the URL in the last read
         */
        byte[] getChunkByteBuffer();


        /**
         * Returns the current socket array references defined in the ModuleTask
         */
        //Socket[] getSocketBuffer();

        /**
         * Sets the current contents of the download buffer
         *
         * @param buffer The bytes that were just read
         */
        void setSocketBuffer(Socket[] buffer);

        /**
         * Defines the actions for each state of the PhotoTask instance.
         *
         * @param state The current state of the task
         */
        void handleSendState(int state);

        /**
         * Get the IP address and the socket details as a String buffer
         */
        String[] getNetworkComponentDetails();
    }

    /**
     * Adding the ModuleTask object which implements the method descirbed in uppermethod
     */
    FrameRequestSendRunnable(TaskRunnableSendMethods moduleTask, CountDownLatch startSignal, CountDownLatch donesignal) {
        this.mModuleTask = moduleTask;
        this.startSignal = startSignal;
        this.donesignal = donesignal;
    }

    @SuppressWarnings("resource")
    @Override

    public void run() {

        /**Store the thread in the mModuleTask object, so that it can interrupt this thread at
         * any time*/

        Log.d("Taggg", "6");
        try {
            startSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mModuleTask.handleSendState(SOCKET_STATE_STARTED);

        mModuleTask.setSendThread(Thread.currentThread());

        /**Moves the current Thread into the background*/
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        byte[] byteBufferSend = mModuleTask.getChunkByteBuffer();



        String[] socketBufferData = mModuleTask.getNetworkComponentDetails();

        Socket[] socketbuffer = new Socket[TOT_SCOKETS];
        Log.d("Taggg", " 7");
        try {

            /**Before continue check whether the thread has not been interrupted*/
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if(byteBufferSend!=null) {

                if(socketBufferData!=null) {


                    socketbuffer[0] = new Socket(socketBufferData[0], Integer.parseInt(socketBufferData[1]));



                    /**Before continue check whether the thread has not been interrupted*/
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }


                    /**Initializing the threads*/
                    for (int temp = 0; temp < RECEIVED_CHANNELS; temp++) {
                        socketbuffer[temp + 1] = new Socket(socketBufferData[0], Integer.parseInt(socketBufferData[temp + 2]));
                    }


                    /**Before continue check whether the thread has not been interrupted*/
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    /**Send request chunk data to the server*/
                    OutputStream os = socketbuffer[0].getOutputStream();
                    os.write(byteBufferSend, 0, byteBufferSend.length);
                    os.flush();
                    socketbuffer[0].close();

                    /**Before continue check whether the thread has not been interrupted*/
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    Log.d("Taggg", "10" );

                }else{
                    Log.d("Debug", "No Socket data received");
                }
            }else{
                Log.d("Debug","No chunk data Buffer received");
            }
            mModuleTask.setSocketBuffer(socketbuffer);
            mModuleTask.handleSendState(SOCKET_STATE_COMPLETED);

        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
            mModuleTask.handleSendState(SOCKET_STATE_FAILED);
        }
        donesignal.countDown();
    }
}

package com.example.rubclientcomplexnew;

import java.net.Socket;

public class DownloadDataRunnable implements Runnable {

    static final int DOWNLOAD_STATE_STARTED = 2;
    static final int DOWNLOAD_STATE_COMPLETED = 3;
    static final int DOWNLOAD_STATE_FAILED = -2;

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
         *
         * Set the downloaded bytebuffer*/
        void setByteBuffer(byte[] downloadedBuffer, int threadNum);

        /**
         * */
    }

    DownloadDataRunnable(TaskRunnableDownloadMethods moduleTask) {
        this.mModuleTask = moduleTask;
    }

    @Override
    public void run() {
        
    }
}

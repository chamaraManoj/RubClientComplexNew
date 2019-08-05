package com.example.rubclientcomplexnew;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ModuleManager {

    /*Status Indicators*/
    static final int REQUEST_SEND_STARTED = 5;
    static final int REQUEST_SEND_COMPLETED = 5;
    static final int REQUEST_SEND_FAILED = 5;

    static final int DOWNLOAD_FAILED = -1;
    static final int DOWNLOAD_STARTED = 1;
    static final int DOWNLOAD_COMPLETE = 2;
    static final int DECODE_STARTED = 3;
    static final int TASK_COMPLETE = 4;

    //Sets the idle time that a thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;

    //Sets the time unit variable
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    // Sets the initial threadpool size to 8
    private static final int CORE_POOL_SIZE = 8;

    // Sets the maximum threadpool size to 8
    private static final int MAXIMUM_POOL_SIZE = 8;

    //Get the number of cores available in the android
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    //A queue of Rnnable for the buffer requsts
    private final BlockingQueue<Runnable> mSendWorkQueue;

    // A queue of Runnables for the image download pool
    private final BlockingQueue<Runnable> mDownloadWorkQueue;

    // A queue of Runnables for the image download pool
    private final BlockingQueue<Runnable> mDecodeWorkQueue;

    // A queue of ModuleManger tasks. Tasks are handed to a ThreadPool.
    private final Queue<ModuleTask> mPhotoTaskWorkQueue;

    //A Managed pool of background mSendWorkQueue
    private final ThreadPoolExecutor mSendThreadPool;

    // A managed pool of background download threads
    private final ThreadPoolExecutor mDownloadThreadPool;

    // A managed pool of background decoder threads
    private final ThreadPoolExecutor mDecodeThreadPool;

    // An object that manages Messages in a Thread
    private Handler mHandler;

    // A single instance of PhotoManager, used to implement the singleton pattern
    private static ModuleManager sInstance = null;

    // A static block that sets class fields
    static {

        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        // Creates a single static instance of PhotoManager
        sInstance = new ModuleManager();
    }

    //Constructing the workqueues and thread pools for the execution
    private ModuleManager(){

        /*Creating work queue for the pool of thread objects for requestingFrame, using a linked list
         * that blocks when the queue is empty*/
        mDownloadWorkQueue = new LinkedBlockingQueue<Runnable>();

        /*Creating work queue for the pool of thread objects for downloading frames, using a linked list
        * that blocks when the queue is empty*/
        mSendWorkQueue = new LinkedBlockingQueue<Runnable>();

        /*Creating work queue for the pool of thread objects for decoding frames, using a linked list
         * that blocks when the queue is empty
         * (This will be implemented after the successful completion of
         * frameRequest and downloading)*/
        mDecodeWorkQueue = new LinkedBlockingQueue<Runnable>();

        /*
         * Creates a work queue for the set of of task objects that control downloading and
         * decoding, using a linked list queue that blocks when the queue is empty.
         */
        mPhotoTaskWorkQueue = new LinkedBlockingQueue<ModuleTask>();

        /*Creating a new pool for thread Objects for the request queue*/
        mSendThreadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mSendWorkQueue);


        /*Creating a new pool for thread Objects for the Download queue*/
        mDownloadThreadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mSendWorkQueue);

        /*Creating a new pool for thread Objects for the Decoding queue*/
        mDecodeThreadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mSendWorkQueue);

        /*handler object to sends messages to the UI object */
        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                ModuleTask  moduleTask = (ModuleTask) msg.obj;

                /*This is a weak reference to the BufferManager object given by the moduletask*/
                BufferManager bufferManager = moduleTask.getBufferManager();

                if(bufferManager!=null){

                }
          }
      };

    }

    /**Return the modulemanager object*/
    public static ModuleManager getInstance() {
        return sInstance;
    }

    /**Call relevant threads based on the status received by the */
    public void handleState(ModuleTask moduleTask, int state) {

    }

    /**
     * Starts an image download and decode
     *
     * @param bufferManager The ImageView that will get the resulting Bitmap
     * @param cacheFlag Determines if caching should be used
     * @return The task instance that will handle the work
     */
    static public ModuleTask sendRequest(
            BufferManager bufferManager,
            boolean cacheFlag) {
        Log.d("Taggg", "3");
        /*
         * Gets a task from the pool of tasks, returning null if the pool is empty
         */
        ModuleTask sendRequestTask = sInstance.mPhotoTaskWorkQueue.poll();

        // If the queue was empty, create a new task instead.
        if (null == sendRequestTask) {
            sendRequestTask = new ModuleTask();
        }

        // Initializes the task
        sendRequestTask.initializeDownloadTask(ModuleManager.sInstance, bufferManager);

        sInstance.mSendThreadPool.execute(sendRequestTask.getSendRequestRunnable());

        /*
         * Provides the download task with the cache buffer corresponding to the URL to be
         * downloaded.
         */
        /**=======================================================================================*/
        //sendRequestTask.setByteBuffer(sInstance.mPhotoCache.get(downloadTask.getImageURL()));

        // If the byte buffer was empty, the image wasn't cached
        //if (null == downloadTask.getByteBuffer()) {

            /*
             * "Executes" the tasks' download Runnable in order to download the image. If no
             * Threads are available in the thread pool, the Runnable waits in the queue.
             */
        //    sInstance.mDownloadThreadPool.execute(downloadTask.getHTTPDownloadRunnable());

            // Sets the display to show that the image is queued for downloading and decoding.
        //    imageView.setStatusResource(R.drawable.imagequeued);

            // The image was cached, so no download is required.
        //} else {

            /*
             * Signals that the download is "complete", because the byte array already contains the
             * undecoded image. The decoding starts.
             */

        //    sInstance.handleState(downloadTask, DOWNLOAD_COMPLETE);
        //}
        /**=======================================================================================*/

        // Returns a task object, either newly-created or one from the task pool
        return sendRequestTask;
    }


    void recycleTask(ModuleTask downloadTask) {

        // Frees up memory in the task
        downloadTask.recycle();

        // Puts the task object back into the queue for re-use.
        mPhotoTaskWorkQueue.offer(downloadTask);
    }
}

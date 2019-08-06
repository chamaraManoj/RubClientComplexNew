package com.example.rubclientcomplexnew;

import com.example.rubclientcomplexnew.FrameRequestSendRunnable.TaskRunnableSendMethods;
import com.example.rubclientcomplexnew.DownloadDataRunnable.TaskRunnableDownloadMethods;
import com.example.rubclientcomplexnew.ModuleManager.*;

import android.graphics.Bitmap;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.Socket;
import java.nio.Buffer;

public class ModuleTask implements TaskRunnableSendMethods,TaskRunnableDownloadMethods {

    /**
     * This weakreference is created to reference the buffermanager object.
     * If the Given BufferManager object is destroyed or unreferenced, this weak
     * reference will also be garbage collected avoding any memory leak or crashes*/
    private WeakReference<BufferManager> mBufferManWeakRef;

    /**
     * Reference to handle the Socket objects*/
    private Socket[] networkSockets;

    /**
     * Refence to handle Network related data*/
    private String[] networkComponentData;

    /**
     * Reference to the chunkRelatad data*/
    private byte[] chunkRelatedData;

    /**
     * Field containing the Thread this task is running on.
     */
    Thread mThreadThis;

    /**
     * Fields containing references to the two runnable objects that handle sending,downloading and
     * decoding of the image.
     */
    private Runnable[] mDownloadRunnable;
    private Runnable[] mDecodeRunnable;
    private Runnable mRequestSendRunnable;

    /**A buffer for containing the bytes that make up the image
    */
    byte[][] mImageBuffers;

    /**buffer containing all the socket details*/
    Socket[] socketBuffer;

    /**Array of array to store tile layer lengths in different layer*/
    int[][] tilelengths;

    /**Images to decode the threads*/
    private Bitmap mDecodedImage;

    /**Getting the current thread running*/
    private Thread mCurrentThread;

    /**
     * An object that contains the ThreadPool singleton.
     */
    private static ModuleManager sModuleManager;

    /**
     * Creates an PhotoTask containing a download object and a decoder object.
     */
    ModuleTask() {
        // Create the runnables
        //Log.d("Taggg", "3_1_1");
        mDownloadRunnable = new DownloadDataRunnable[4];

        mRequestSendRunnable = new FrameRequestSendRunnable(this);
        mDownloadRunnable[0] = new DownloadDataRunnable(this,ModuleManager.BASE_LAYER );
        mDownloadRunnable[1] = new DownloadDataRunnable(this,ModuleManager.ENHANCE_LAYER_1);
        mDownloadRunnable[2] = new DownloadDataRunnable(this,ModuleManager.ENHANCE_LAYER_2);
        mDownloadRunnable[3] = new DownloadDataRunnable(this,ModuleManager.ENHANCE_LAYER_3);
        /*mDecodeRunnable = new PhotoDecodeRunnable(this);*/
        sModuleManager = ModuleManager.getInstance();
        //Log.d("Taggg", "3_1_2");
    }

    /**
     * Initializing the task
     * This function set the required variables need for the task
     *
     * @param moduleManager A Thread pool object
     * @param bufferManager A object for bufferManagement*/

    void initializeDownloadTask(ModuleManager moduleManager, BufferManager bufferManager){
        Log.d("Taggg", "4");
        sModuleManager = moduleManager;

        /**Read the network related parameters to a string buffer*/
        networkComponentData = bufferManager.getNetworkComponentData();

        /**Read chunk related data to a byte[] buffer*/
        chunkRelatedData = bufferManager.getChunkData();

        /**Instantiates the weak reference to the incoming buffermanager*/
        mBufferManWeakRef = new WeakReference<BufferManager>(bufferManager);
    }

    /**
     * Recycles an PhotoTask object before it's put back into the pool. One reason to do
     * this is to avoid memory leaks.
     */
    void recycle() {

        // Deletes the weak reference to the imageView
        if ( null != mBufferManWeakRef ) {
            mBufferManWeakRef.clear();
            mBufferManWeakRef = null;
        }

        // Releases references to the byte buffer and the BitMaps
        mImageBuffers = null;
        mDecodedImage = null;
    }

    /*
     * Returns the Thread that this Task is running on. The method must first get a lock on a
     * static field, in this case the ThreadPool singleton. The lock is needed because the
     * Thread object reference is stored in the Thread object itself, and that object can be
     * changed by processes outside of this app.
     */
    public Thread getCurrentThread() {
        synchronized(sModuleManager) {
            return mCurrentThread;
        }
    }

    /*
     * Sets the identifier for the current Thread. This must be a synchronized operation; see the
     * notes for getCurrentThread()
     */
    public void setCurrentThread(Thread thread) {
        synchronized(sModuleManager) {
            mCurrentThread = thread;
        }
    }

    /**
     * Returns the instance that send the request to the server*/
    Runnable getSendRequestRunnable() {
        Log.d("Taggg", "5");
        return mRequestSendRunnable;
    }

    Runnable getDownloadDataRunnable(int threadNum){
        return mDownloadRunnable[threadNum];
    }

    /**
     * This function return the object of bufferManager reference by the weakReference m*/
    public BufferManager getBufferManager() {
        if ( null != mBufferManWeakRef ) {
            return mBufferManWeakRef.get();
        }
        return null;
    }

    /**Function to send the thread state data every time to the module manager*/
    void handleState(int state) {
        sModuleManager.handleState(this, state);
    }


    /**Implementation of methods in the TaskRunnableSendMethods in
     * FrameRequestSendRunnable class=============================*/
    @Override
    public void setSendThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    /**Thia funcition works of interrupting the threads*/
    @Override
    public void setSocketBuffer(Socket[] buffer) {
        Log.d("Taggg", "11" );
        networkSockets = buffer;
    }


    /***/
    @Override
    public void handleSendState(int state) {

        int outState;

        switch(state) {
            case FrameRequestSendRunnable.SOCKET_STATE_STARTED:
                outState = ModuleManager.REQUEST_SEND_STARTED;
                break;
            case FrameRequestSendRunnable.SOCKET_STATE_COMPLETED:
                outState = ModuleManager.REQUEST_SEND_COMPLETED;
                break;
            default:
                outState = ModuleManager.REQUEST_SEND_FAILED;
                break;
        }
        // Passes the state to the ThreadPool object.
        handleState(outState);
    }

    @Override
    public String[] getNetworkComponentDetails() {
        return networkComponentData;
    }

   @Override
    public byte[] getChunkByteBuffer() {
        return chunkRelatedData;
    }

    /**End of Implementation of methods in the TaskRunnableSendMethods in
     * FrameRequestSendRunnable class==================================*/


    /**Implementation of methods in the TaskRunnableDownloadMethods in
     * DataDownloadRunnable class=============================*/
    @Override
    public byte[] getDataBuffer(int threadNum) {
        return mImageBuffers[threadNum];
    }

    @Override
    public Socket getSocket(int threadNum) {
        return networkSockets[threadNum];
    }

    @Override
    public void setByteBuffer(byte[] downloadedBuffer, int threadNum) {
        mImageBuffers[threadNum] = downloadedBuffer;
    }


    public int[] getTileLengthBuffer(int threadNum){
        return tilelengths[threadNum];
    }

    /**
     * Set the buffer containing the tile's lengths */
    public void setTileLengthsBuffer(int[] tileLengthsBuffer,int threaNum){
        tilelengths[threaNum] = tileLengthsBuffer;
    }

    /**End of Implementation of methods in the TaskRunnableDownloadMethods in
     * DataDownloadRunnable class=============================*/
}

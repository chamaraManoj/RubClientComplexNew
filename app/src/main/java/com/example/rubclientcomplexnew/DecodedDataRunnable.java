package com.example.rubclientcomplexnew;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public class DecodedDataRunnable implements Runnable {

    private static final boolean VERBOSE_DECODE = true;

    private static final String TAG = "Taggg";

    private static final String MIME_TYPE = "video/hevc";

    private static final byte QUALITY_SD = 0;
    private static final byte QUALITY_HD = 1;
    private static final byte QUALITY_4K = 2;

    private MediaCodecList mediaCodecList;
    private MediaFormat mediaFormat;
    private MediaCodec decoder;

    private CountDownLatch startSignal;
    private CountDownLatch doneSignal;


    final TaskRunnableDecoderMethods moduleTask;
    final int layerNum;
    final int tileNum;



    /**
     * Variables to set the resolution
     */
    private int chunkQuality;
    private int mWidth = -1;
    private int mHeight = -1;


    public DecodedDataRunnable(TaskRunnableDecoderMethods moduleTask, int numThread,int tile, CountDownLatch startSignal, CountDownLatch doneSignal) {
        this.moduleTask = moduleTask;
        this.layerNum = numThread;
        this.startSignal = startSignal;
        this.doneSignal = doneSignal;
        this.tileNum = tile;
    }

    /**Initialize the chunkQuality qualitites*/

    /**
     * implementing the interface to getting and setting the buffers for decoding
     */
    interface TaskRunnableDecoderMethods {
        /**
         * Sets the Thread that this instance is running on
         *
         * @param currentThread the current Thread
         */
        void setSendThread(Thread currentThread);

        /**
         * Get the current content of the
         */
        byte[] getDataBuffer(int layerNum);

        /**
         * Get the quality of the chunk
         */
        int getChunkQuality();

        /**
         * Get the presentation time of the chunk
         */
        long getChunk();

        /**
         * set the surface buffer
         */
        void setSurfaceBuffer(int layerNum);

        /**
         * get the layer length
         */
        int getLayerLength(int layerNum);
    }


    @Override
    public void run() {

    }


    /**
     * Initialize the codecs for decoding
     */
    private void encodeDecodeVideoFromBuffer() {

        this.decoder = null;

        try {
            /**Get the MedicCodec info available for this MIME_TYPE*/
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);

            /**If there is no any related coded info exit the function*/
            if (codecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }

            /**if the width and hieght is not return from the function*/
            /**if(!setQuality()){
             Log.e(TAG, "Unable to Send width and hieght" + String.valueOf(mWidth) +" "+ String.valueOf(mHeight));
             return;
             }*/

            MediaCodec mediaCodec = MediaCodec.createDecoderByType("video/hevc");
            final MediaFormat[] mOutputFormat = new MediaFormat[1];

            mediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec mediaCodec, int i) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(i);

                    byte[] tempBuffer = moduleTask.getDataBuffer(layerNum);

                    /**read the buffer related data*/
                    int size = moduleTask.getLayerLength(layerNum);
                    long presentationTIme = moduleTask.getChunk();

                    /*=========================Debug Point=========================================*/
                    if (ModuleManager.VERBOSE || DecodedDataRunnable.VERBOSE_DECODE) {
                        Log.d(TAG, "InputBuffer: returned buffer of size " + size);
                        Log.d(TAG, "InputBuffer: returned buffer for time " + presentationTIme);
                    }

                    /**If the returned size is not greater than 0*/
                    if (!(size > 0)) {
                        Log.e("Taggg", "Returned buffer length is equal to zero");
                        return;
                    }
                    inputBuffer = ByteBuffer.wrap(moduleTask.getDataBuffer(layerNum));
                    if (size > 0) {
                        decoder.queueInputBuffer(i,
                                0,
                                size,
                                presentationTIme,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                @Override
                public void onOutputBufferAvailable(MediaCodec mediaCodec, int i, MediaCodec.BufferInfo bufferInfo) {

                    if (ModuleManager.VERBOSE || DecodedDataRunnable.VERBOSE_DECODE) {
                        Log.d(TAG, "video decoder: returned output buffer: " + i);
                        Log.d(TAG, "video decoder: returned buffer of size " + bufferInfo.size);
                    }

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (ModuleManager.VERBOSE || DecodedDataRunnable.VERBOSE_DECODE)
                            Log.d(TAG, "video decoder: codec config buffer");
                        decoder.releaseOutputBuffer(i, false);
                        return;
                    }
                    ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
                    MediaFormat bufferFormat = mediaCodec.getOutputFormat(i); // option A

                    decoder.releaseOutputBuffer(i,false);
                }

                @Override
                public void onError(MediaCodec mediaCodec, MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
                    mOutputFormat[0] = mediaFormat; // option B
                }
            });

            mediaCodec.configure(null,null,null,0);
            mOutputFormat[0] = mediaCodec.getOutputFormat(); // option B
            mediaCodec.start();
            // wait for processing to complete
            mediaCodec.stop();
            mediaCodec.release();


        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Set the image width and the height of the buffer
     *
     * @return if the width and hieght is set return true
     */
    private boolean setQuality() {
        chunkQuality = moduleTask.getChunkQuality();

        switch (chunkQuality) {
            case QUALITY_SD:
                mWidth = 1280;
                mHeight = 720;
                break;
            case QUALITY_HD:
                mWidth = 1920;
                mHeight = 1080;
                break;
            case QUALITY_4K:
                mWidth = 3840;
                mHeight = 2160;
        }
        if (mWidth > 0 && mHeight > 0)
            return true;
        else
            return false;
    }


}

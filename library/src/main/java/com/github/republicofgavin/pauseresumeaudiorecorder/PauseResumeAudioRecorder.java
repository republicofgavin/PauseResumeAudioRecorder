package com.github.republicofgavin.pauseresumeaudiorecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.github.republicofgavin.pauseresumeaudiorecorder.conversion.PcmWavConverter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An audio recorder that supports pause/resume functionality. All files are recorded as PCM files and then converted into WAV files upon stop being called. All recording and writing to the file is done on a separate thread with the configurations it had when recording started.
 * There is no way to return an instance of this object to any other state once it has been stopped. That way race conditions cannot occur between the recording thread and consumers over state.
 * If an error occurs during recording(Like unable to write to the specified file for example), the thread attempts to convert what is there into a WAV file and sets its state to error.
 * Error state can only occur during paused and recording states and attempts to operate the recorder(resuming recording for example) will result in an {@link IllegalStateException} being thrown.
 * @author (Gavin)republicofgavin@gmail.com
 */
public class PauseResumeAudioRecorder {

    private AtomicInteger currentAudioState;
    private AudioRecorderThread currentAudioRecordingThread;

    private int sampleRateInHertz;
    private int channelConfig;
    private int audioEncoding;
    private String audioFile;

    private static final int DEFAULT_AUDIO_SAMPLE_RATE_HERTZ=44100;
    private static final String TAG=PauseResumeAudioRecorder.class.getSimpleName();

    private OnFileSizeReachedListener onFileSizeReachedListener;
    private long maxFileSizeInBytes;

    private OnTimeCompletionListener onTimeCompletionListener;
    private TimerTask onTimeCompletionTimerTask;
    private Timer onTimeCompletedTimer;
    private long maxTimeInMillis;
    private long recordingStartTimeMillis;
    private long remainingMaxTimeInMillis;
    /**
     * The stopped state flag. At this point, the file should be made and in the right format.
     */
    public static final int STOPPED_STATE=-1;
    /**
     * The Initialized state flag. The recorder is not prepared, it still needs to be given a file path.
     */
    public static final int INITIALIZED_STATE=0;
    /**
     * The prepared state flag. At this point the recorder is configured properly and ready for recording.
     */
    public static final int PREPARED_STATE=1;
    /**
     * The recording state flag. At this point the recorder is writing the data to the PCM file. The WAV file is not made yet.
     */
    public static final int RECORDING_STATE=2;
    /**
     * The paused state flag. At this point some recording data has been written to the PCM file, but it is on stand by for the resume API to be called.
     */
    public static final int PAUSED_STATE=3;
    /**
     *The error occurred state flag. Something wrong occurred during recording on its thread. You may wish to check for this after calling stop on the recorder.
     */
    public static final int ERROR_STATE=-2;
    /**
     * Default constructor (sets values to: 44100htz, MONO, and PCM_16BIT, {@link PcmWavConverter}.MAX_TIME_WAV_FILE_MILLIS and {@link PcmWavConverter}.MAX_SIZE_WAV_FILE_BYTES,  ). These are acceptable settings, so once you set the file path. It is an prepared state(starts off initialized).
     */
    public PauseResumeAudioRecorder(){
        sampleRateInHertz=DEFAULT_AUDIO_SAMPLE_RATE_HERTZ;
        channelConfig= AudioFormat.CHANNEL_IN_MONO;
        audioEncoding=AudioFormat.ENCODING_PCM_16BIT;
        currentAudioState=new AtomicInteger(INITIALIZED_STATE);
        maxTimeInMillis=PcmWavConverter.MAX_TIME_WAV_FILE_MILLIS;
        maxFileSizeInBytes=PcmWavConverter.MAX_SIZE_WAV_FILE_BYTES;
    }

    /**
     * Sets the {@link com.github.republicofgavin.pauseresumeaudiorecorder.PauseResumeAudioRecorder.OnFileSizeReachedListener}
     * @param onFileSizeReachedListener A method you want called when the recording has reached its max file size. Null if nothing should be called.
     * @throws IllegalStateException If this is called during a recording, paused, or error state.
     */
    public void setOnFileSizeReachedListener(final OnFileSizeReachedListener onFileSizeReachedListener){
        if (currentAudioState.get()!=PREPARED_STATE && currentAudioState.get()!=INITIALIZED_STATE ){
            throw new IllegalStateException("Cannot modify onFileSizeReachedListener during a non-prepared and non-initialized state");
        }
        this.onFileSizeReachedListener = onFileSizeReachedListener;
    }

    /**
     * Sets the max file size (in bytes) default value is {@link PcmWavConverter}.MAX_SIZE_WAV_FILE_BYTES. Files will not go over that limit, but will stop slightly below it.
     * @param maxFileSizeInBytes The maximum allowed file size in bytes. Must be greater than or equal to 1kb and less than or equal to {@link PcmWavConverter}.MAX_SIZE_WAV_FILE_BYTES
     */
    public void setMaxFileSizeInBytes(final long maxFileSizeInBytes){
        if (maxFileSizeInBytes<1000 || maxFileSizeInBytes> PcmWavConverter.MAX_SIZE_WAV_FILE_BYTES){
            throw new IllegalArgumentException("Invalid max file size given");
        }
        if (currentAudioState.get()!=PREPARED_STATE && currentAudioState.get()!=INITIALIZED_STATE ){
            throw new IllegalStateException("Cannot modify max file size during a non-prepared and non-initialized state");
        }
        this.maxFileSizeInBytes=maxFileSizeInBytes;
    }
    /**
     * Sets the {@link com.github.republicofgavin.pauseresumeaudiorecorder.PauseResumeAudioRecorder.OnTimeCompletionListener}. Pausing the recorder effectively will pause the timer as well.
     * @param onTimeCompletionListener A method you want called when the recording has run out of time. Null if nothing should be called.
     * @throws IllegalStateException If this is called during a recording, paused, or error state.
     */
    public void setOnTimeCompletionListener(final OnTimeCompletionListener onTimeCompletionListener) {
        if (currentAudioState.get()!=PREPARED_STATE && currentAudioState.get()!=INITIALIZED_STATE ){
            throw new IllegalStateException("Cannot modify onTimeCompletionListener during a non-prepared and non-initialized state");
        }
        this.onTimeCompletionListener = onTimeCompletionListener;
    }

    /**
     *Sets the maxmimum time the recording should last. Default value is {@link PcmWavConverter}.MAX_TIME_WAV_FILE_MILLIS
     * @param maxTimeInMillis Sets the maximum time a recording should last. Must be greater than or equal to 1000 and less than or equal to {@link PcmWavConverter}.MAX_TIME_WAV_FILE_MILLIS
     */
    public void setMaxTimeInMillis(final long maxTimeInMillis){
        if (maxTimeInMillis<1000 || maxTimeInMillis>PcmWavConverter.MAX_TIME_WAV_FILE_MILLIS){
            throw new IllegalArgumentException("Invalid max time given");
        }
        else if (currentAudioState.get()!=PREPARED_STATE && currentAudioState.get()!=INITIALIZED_STATE ){
            throw new IllegalStateException("Cannot modify maxTimeInMillis during a non-prepared and non-initialized state");
        }
        this.maxTimeInMillis=maxTimeInMillis;
        this.remainingMaxTimeInMillis=maxTimeInMillis;
    }

    /**
     * Sets the encoding for the audio file.
     * @param audioEncoding Must be {@link AudioFormat}.ENCODING_PCM_8BIT or {@link AudioFormat}.ENCODING_PCM_16BIT.
     * @throws IllegalArgumentException If the encoding is not {@link AudioFormat}.ENCODING_PCM_8BIT or {@link AudioFormat}.ENCODING_PCM_16BIT
     * @throws IllegalStateException If it is being modified when it is not in INITIALIZED_STATE or PREPARED_STATE.
     */
    public void setAudioEncoding(final int audioEncoding){
        if (audioEncoding !=AudioFormat.ENCODING_PCM_8BIT && audioEncoding !=AudioFormat.ENCODING_PCM_16BIT){
            throw new IllegalArgumentException("Invalid encoding");
        }
        else if (currentAudioState.get()!=PREPARED_STATE && currentAudioState.get()!=INITIALIZED_STATE ){
            throw new IllegalStateException("Cannot modify audio encoding during a non-prepared and non-initialized state");
        }
        this.audioEncoding=audioEncoding;
    }
    /**
     * Setter for the audioFile. If the file does not contain a .wav suffix, it will be added. If the file has a suffix other than .wav, it will be removed. This API puts it in the prepared state.
     * NOTE: The .wav file does not exist until the stop recording (and subsequent conversion) is completed. Where the data is stored temporarily is the same path and name just with .temp instead of .wav.
     * @param audioFilePath A fully qualified file path for the audio file to be stored. The file path should exist and the file should not, errors can occur during writing.
     * @throws IllegalArgumentException if the parameter is null, empty, blank.
     * @throws IllegalStateException If the API is called while the recorder is not initialized or prepared.
     */
    public void setAudioFile(final String audioFilePath){
        if (audioFilePath==null || audioFilePath.trim().isEmpty()){
            throw new IllegalArgumentException("audioFile cannot be null, empty, blank, or directory");
        }
        else if (currentAudioState.get()!=PREPARED_STATE && currentAudioState.get()!=INITIALIZED_STATE ){
            throw new IllegalStateException("Recorder cannot have its file changed when it is not in an initialized or prepared state");
        }
        String modifiedAudioFilePath=audioFilePath;
        if (modifiedAudioFilePath.toLowerCase(Locale.getDefault()).contains(".")){
            final String subString=modifiedAudioFilePath.substring(modifiedAudioFilePath.lastIndexOf("."));
            modifiedAudioFilePath=modifiedAudioFilePath.replace(subString,".temp");
        }
        else {
            modifiedAudioFilePath=modifiedAudioFilePath+".temp";
        }
        this.audioFile=modifiedAudioFilePath;
        currentAudioState.getAndSet(PREPARED_STATE);
    }

    /**
     * Sets the sample rate for the recording.
     * @param sampleRateInHertz The sample rate to record the audio with.
     * @throws IllegalArgumentException If the sample rate is not: 44100,22050,16000, or 11025
     * @throws IllegalStateException If the API is called while the recorder is not initialized or prepared.
     */
    public void setSampleRate(final int sampleRateInHertz){
        if (sampleRateInHertz!=DEFAULT_AUDIO_SAMPLE_RATE_HERTZ && sampleRateInHertz !=22050
                && sampleRateInHertz != 16000 && sampleRateInHertz !=11025){
            throw new IllegalArgumentException("Invalid sample rate given");
        }
        else if (currentAudioState.get()!=PREPARED_STATE && currentAudioState.get()!=INITIALIZED_STATE ){
            throw new IllegalStateException("Recorder cannot have its sample rate changed when it is not in an initialized or prepared state");
        }
        this.sampleRateInHertz=sampleRateInHertz;
    }

    /**
     * Sets the channel.
     * @param channelConfig {@link AudioFormat}.CHANNEL_IN_MONO, {@link AudioFormat}.CHANNEL_IN_DEFAULT, or {@link AudioFormat}.CHANNEL_IN_STEREO
     * @throws IllegalArgumentException if it is not Mono or Stereo.
     * @throws IllegalStateException If the channel is changed when it is not in a prepared or initialized state.
     */
    public void setChannel(final int channelConfig){
        if(channelConfig != AudioFormat.CHANNEL_IN_MONO && channelConfig !=AudioFormat.CHANNEL_IN_STEREO && channelConfig != AudioFormat.CHANNEL_IN_DEFAULT){
            throw new IllegalArgumentException("Invalid channel given.");
        }
        else if (currentAudioState.get()!=PREPARED_STATE && currentAudioState.get()!=INITIALIZED_STATE ){
            throw new IllegalStateException("Recorder cannot have its file changed when it is in an initialized or prepared state");
        }
        this.channelConfig=channelConfig;
    }

    /**
     *
     * @return The current state of the recorder. These are listed as static variables on this class.
     */
    public int getCurrentState(){
        return currentAudioState.get();
    }
    /**
     * Starts the recording if the recorder is in a prepared state. At this time, the complete file path should not have .temp file(as that is where the writing is taking place) and the specified .wav file should not exist as well(as that is where the .temp file will be converted to).
     * Does nothing if it is recorder is not in a prepared state.
     * @throws IllegalArgumentException If the parameters passed into it are invalid according to {@link AudioRecord}.getMinBufferSize API.
     */
    public void startRecording(){
        if (currentAudioState.get() == PREPARED_STATE) {
            currentAudioRecordingThread = new AudioRecorderThread(audioFile.replace(".wav",".temp"), MediaRecorder.AudioSource.MIC, sampleRateInHertz,channelConfig,audioEncoding,maxFileSizeInBytes);
            currentAudioState.set(RECORDING_STATE);
            currentAudioRecordingThread.start();
            onTimeCompletedTimer=new Timer(true);
            onTimeCompletionTimerTask=new MaxTimeTimerTask();
            onTimeCompletedTimer.schedule(onTimeCompletionTimerTask,maxTimeInMillis);
            remainingMaxTimeInMillis=maxTimeInMillis;
            recordingStartTimeMillis=System.currentTimeMillis();
        }
        else{
            Log.w(TAG,"Audio recorder is not in prepared state. Ignoring call.");
        }
    }

    /**
     * Pauses the recording if the recorder is in a recording state. Does nothing if in another state. Paused media recorder halts the max time countdown.
     */
    public void pauseRecording(){
        if (currentAudioState.get()==RECORDING_STATE){
            currentAudioState.getAndSet(PAUSED_STATE);
            onTimeCompletedTimer.cancel();
            remainingMaxTimeInMillis=remainingMaxTimeInMillis-(System.currentTimeMillis()-recordingStartTimeMillis);
        }
        else{
            Log.w(TAG,"Audio recording is not recording");
        }
    }

    /**
     * Resumes the audio recording. Does nothing if the recorder is in a non-recording state.
     */
    public void resumeRecording(){
        if (currentAudioState.get()==PAUSED_STATE){
            recordingStartTimeMillis=System.currentTimeMillis();
            currentAudioState.getAndSet(RECORDING_STATE);
            onTimeCompletedTimer=new Timer(true);
            onTimeCompletionTimerTask=new MaxTimeTimerTask();
            onTimeCompletedTimer.schedule(onTimeCompletionTimerTask,remainingMaxTimeInMillis);
        }
        else {
            Log.w(TAG,"Audio recording is not paused");
        }
    }

    /**
     * Stops the audio recording if it is in a paused or recording state. Does nothing if the recorder is already stopped.
     * @throws IllegalStateException If the recorder is not in a paused, recording, or stopped state.
     */
    public void stopRecording(){
        if (currentAudioState.get()== PAUSED_STATE || currentAudioState.get()==RECORDING_STATE){
            currentAudioState.getAndSet(STOPPED_STATE);
            onTimeCompletedTimer.cancel();
            onTimeCompletedTimer=null;
            onTimeCompletionTimerTask=null;
        }
        else{
            Log.w(TAG,"Audio recording is not in a paused or recording state.");
        }
        currentAudioRecordingThread=null;//The existing thread will die out on its own, but not before attempting to convert the file into WAV format.
    }

    /**
     * This thread takes data from an {@link AudioRecord} and outputs it into the specified file. During the state of paused, it sleeps for a 100ms and rechecks to see if the state has changed.
     * If the state has changed to error or stopped, the thread tries to convert the file into a wav file and delete the PCM one before dying off.
     */
    private class AudioRecorderThread extends Thread{
        private AudioRecord currentAudioRecording;
        private int bufferSizeInBytes;
        private long threadMaxFileSizeInBytes;
        private String threadAudioFile;
        private int threadChannelConfig;
        private int threadAudioEncoding;
        private int threadSampleRateHertz;

        /**
         * Default constructor. Parameters are passed into the thread to keep the recorder(ultimately the user) from changing the values and thus altering the state of the thread.
         * @param threadAudioFile The file path where the {@link AudioRecord} writes data to. Ultimately it will deleted when the data is converted.
         * @param threadAudioSource The source of the audio data. Currently, only MIC is supported.
         * @param threadSampleRateHertz The sample rate in Hz
         * @param threadChannelConfig The channel config (MONO or STEREO).
         * @param threadAudioEncoding The audio encoding (8 bit or 16 bit).
         * @param maxFileSizeInBytes Maximum file size in bytes.
         */
        AudioRecorderThread(final String threadAudioFile,final int threadAudioSource, final int threadSampleRateHertz, final int threadChannelConfig, final int threadAudioEncoding,final long maxFileSizeInBytes){
            this.threadAudioFile=threadAudioFile;
            bufferSizeInBytes=AudioRecord.getMinBufferSize(threadSampleRateHertz,threadChannelConfig,threadAudioEncoding);
            currentAudioRecording=new AudioRecord(threadAudioSource,threadSampleRateHertz,threadChannelConfig,threadAudioEncoding,bufferSizeInBytes);

            this.threadSampleRateHertz=threadSampleRateHertz;
            this.threadChannelConfig=threadChannelConfig;
            this.threadAudioEncoding=threadAudioEncoding;
            this.threadMaxFileSizeInBytes=maxFileSizeInBytes;
        }
        @Override
        public void run(){
            currentAudioRecording.startRecording();
            final short[] readingBuffer = new short[bufferSizeInBytes];
            DataOutputStream dataOutputStream=null;
            final short waveHeaderChannelConfig=(short)((threadChannelConfig==AudioFormat.CHANNEL_IN_MONO)?1:2);
            final short waveHeaderBitrateConfig=(short)((AudioFormat.ENCODING_PCM_8BIT==threadAudioEncoding)?8:16);
            try {
                dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(threadAudioFile)));
                int currentState = currentAudioState.getAndSet(currentAudioState.get());//This, unlike the normal get, does it atomically.
                //we add a fake header to be a place holder for the real header, once the recording is complete.
                PcmWavConverter.addWavHeader(new PcmWavConverter.WaveHeader(threadSampleRateHertz, waveHeaderChannelConfig, waveHeaderBitrateConfig),threadAudioFile);
                long currentFileSizeInBytes=0;
                while (currentState == RECORDING_STATE || currentState == PAUSED_STATE) {
                    if (currentState == PAUSED_STATE) {
                        sleep(100);
                    }
                    else {
                        final int length = currentAudioRecording.read(readingBuffer, 0, bufferSizeInBytes);
                        for (int i = 0; i < length; i++) {
                            //write the data in Little Endian format;
                            dataOutputStream.writeByte(readingBuffer[i] & 0xFF);
                            dataOutputStream.writeByte((readingBuffer[i] >> 8) & 0xFF);
                        }
                        currentFileSizeInBytes=currentFileSizeInBytes+bufferSizeInBytes;
                        //If the next input clip goes over, just stop the thread now.
                        if (currentFileSizeInBytes+bufferSizeInBytes>threadMaxFileSizeInBytes){
                            Log.d(TAG,"Max file size has been reached. Stopping recording thread.");
                            currentAudioState.getAndSet(STOPPED_STATE);
                            new Thread(new MaxSizeReachedRunnable()).run();
                        }
                    }
                    currentState = currentAudioState.getAndSet(currentAudioState.get());
                }
                currentAudioRecording.stop();
            }
            catch(IOException ex){
                currentAudioState.getAndSet(ERROR_STATE);
                throw new RuntimeException("IOException has occurred while recording file: "+threadAudioFile,ex);
            }
            catch (InterruptedException ex){
                currentAudioState.getAndSet(ERROR_STATE);
                Log.d(TAG,"InterruptedException occurred for audioFile: "+ threadAudioFile);
            }
            finally{
                try {
                    if (dataOutputStream !=null) {
                        dataOutputStream.flush();
                        dataOutputStream.close();
                        PcmWavConverter.addWavHeader(new PcmWavConverter.WaveHeader(threadSampleRateHertz, waveHeaderChannelConfig, waveHeaderBitrateConfig), threadAudioFile);

                        if(!(new File(threadAudioFile).renameTo(new File(threadAudioFile.replace(".temp", ".wav"))))){
                            Log.e(TAG,"PCM file was not renamed.");
                            currentAudioState.getAndSet(ERROR_STATE);
                        }
                    }
                }
                catch (IOException ex){
                    Log.e(TAG,"IOException occurred for audioFile"+audioFile);
                    currentAudioState.getAndSet(ERROR_STATE);
                }
            }
        }
    }
    /**
     * Interface that gets called when specified time limit has been reached.
     */
    public interface OnTimeCompletionListener{
        /**
         * Called on the secondary thread when the specified time is elapsed. If you desire to run UI code, Use the looper's runonUIThread method.
         * @param pauseResumeAudioRecorder The instance of the {@link PauseResumeAudioRecorder} that had the event.
         */
        void onTimeCompleted(final PauseResumeAudioRecorder pauseResumeAudioRecorder);
    }
    /**
     * Interface that gets called when specified file size been reached or exceeded(The recorder will put the last complete set of data on the file before quitting.
     */
    public interface OnFileSizeReachedListener{
        /**
         * Called on the secondary thread when the specified file size has been reached or exceeded. If you desire to run UI code, Use the looper's runonUIThread method.
         * @param pauseResumeAudioRecorder The instance of the {@link PauseResumeAudioRecorder} that had the event.
         */
        void onFileSizeReached(final PauseResumeAudioRecorder pauseResumeAudioRecorder);
    }
    /**
     * {@link TimerTask} that sets the state to stopped and fires the interface
     */
    private class MaxTimeTimerTask extends TimerTask{
        @Override
        public void run(){
            Log.i(TAG, "Max time has been reached for recording.");
            currentAudioState.getAndSet(STOPPED_STATE);
            if (onTimeCompletionListener != null) {
                onTimeCompletionListener.onTimeCompleted(PauseResumeAudioRecorder.this);
            }
        }
    }

    /**
     * Runnable that fires when the max file size has been reached approximately.
     */
    private class MaxSizeReachedRunnable implements Runnable{
        @Override
        public void run() {
            if (onFileSizeReachedListener !=null){
                onFileSizeReachedListener.onFileSizeReached(PauseResumeAudioRecorder.this);
            }
        }
    }
}

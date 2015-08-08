package com.github.republicofgavin.pauseresumeaudiorecorder.shadows;

import android.media.AudioRecord;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * The shadow of {@link AudioRecord}
 * @author Gavin(republicofgavin@gmail.com)
 */
@Implements(AudioRecord.class)
public class ShadowAudioRecord {
    private static int minBufferSize=100;

    public boolean isRecording=false;
    public int audioSource;
    public int sampleRateInHz;
    public int channelConfig;
    public int audioFormat;
    public int bufferSizeInBytes;

    public void __constructor__(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        this.audioSource=audioSource;
        this.sampleRateInHz=sampleRateInHz;
        this.channelConfig=channelConfig;
        this.audioFormat=audioFormat;
        this.bufferSizeInBytes=bufferSizeInBytes;
    }

    @Implementation
    static public int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat){
        return minBufferSize;
    }
    @Implementation
    public void startRecording()throws IllegalStateException{
        isRecording=true;
    }
    @Implementation
    public void stop() throws IllegalStateException{
        isRecording=false;
    }
    @Implementation
    public int read(final short[] buffer,final int offsetInShorts,final int sizeInShorts){
        for (int count=offsetInShorts; count<sizeInShorts; count++){
            buffer[count]=(short)1;
        }
        return sizeInShorts;
    }
    public void setMinBufferSize(final int minBufferSize){
        this.minBufferSize=minBufferSize;
    }
}

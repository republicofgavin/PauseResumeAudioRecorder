package com.github.republicofgavin.pauseresumeaudiorecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

import com.github.republicofgavin.pauseresumeaudiorecorder.conversion.PcmWavConverter;
import com.github.republicofgavin.pauseresumeaudiorecorder.shadows.ShadowAudioRecord;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests {@link PauseResumeAudioRecorder}
 * @author Gavin(republicofgavin@gmail.com)
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE,shadows=ShadowAudioRecord.class, emulateSdk = 18)
public class PauseResumeAudioRecorderTest {
    private PauseResumeAudioRecorder pauseResumeAudioRecorder;
    private boolean failTest;

    private Field sampleRateInHertzField;
    private Field channelConfigField;
    private Field audioEncodingField;
    private Field audioFileField;
    private Field currentAudioStateField;
    private Field currentAudioRecordingThreadField;

    private Field recordingStartTimeMillisField;
    private Field remainingMaxTimeInMillisField;
    private Field onTimeCompletedTimerField;
    private Field onTimeCompletionTimerTaskField;
    private Field onTimeCompletionListenerField;
    private Field maxTimeInMillisField;

    private Field onFileSizeReachedListenerField;
    private Field maxFileSizeInBytesField;
    //thread fields
    private Field audioRecordThreadField;
    @Mock
    private Timer mockTimer;
    @Before
    public void setup()throws NoSuchFieldException,IllegalAccessException{
        MockitoAnnotations.initMocks(this);
        pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        failTest=true;

        sampleRateInHertzField=PauseResumeAudioRecorder.class.getDeclaredField("sampleRateInHertz");
        sampleRateInHertzField.setAccessible(true);

        channelConfigField=PauseResumeAudioRecorder.class.getDeclaredField("channelConfig");
        channelConfigField.setAccessible(true);

        audioEncodingField=PauseResumeAudioRecorder.class.getDeclaredField("audioEncoding");
        audioEncodingField.setAccessible(true);

        audioFileField=PauseResumeAudioRecorder.class.getDeclaredField("audioFile");
        audioFileField.setAccessible(true);

        currentAudioStateField=PauseResumeAudioRecorder.class.getDeclaredField("currentAudioState");
        currentAudioStateField.setAccessible(true);

        currentAudioRecordingThreadField=PauseResumeAudioRecorder.class.getDeclaredField("currentAudioRecordingThread");
        currentAudioRecordingThreadField.setAccessible(true);

        recordingStartTimeMillisField=PauseResumeAudioRecorder.class.getDeclaredField("recordingStartTimeMillis");
        recordingStartTimeMillisField.setAccessible(true);

        onTimeCompletedTimerField=PauseResumeAudioRecorder.class.getDeclaredField("onTimeCompletedTimer");
        onTimeCompletedTimerField.setAccessible(true);

        onTimeCompletionTimerTaskField=PauseResumeAudioRecorder.class.getDeclaredField("onTimeCompletionTimerTask");
        onTimeCompletionTimerTaskField.setAccessible(true);

        onTimeCompletionListenerField=PauseResumeAudioRecorder.class.getDeclaredField("onTimeCompletionListener");
        onTimeCompletionListenerField.setAccessible(true);

        remainingMaxTimeInMillisField=PauseResumeAudioRecorder.class.getDeclaredField("remainingMaxTimeInMillis");
        remainingMaxTimeInMillisField.setAccessible(true);

        maxTimeInMillisField=PauseResumeAudioRecorder.class.getDeclaredField("maxTimeInMillis");
        maxTimeInMillisField.setAccessible(true);

        onFileSizeReachedListenerField=PauseResumeAudioRecorder.class.getDeclaredField("onFileSizeReachedListener");
        onFileSizeReachedListenerField.setAccessible(true);

        maxFileSizeInBytesField=PauseResumeAudioRecorder.class.getDeclaredField("maxFileSizeInBytes");
        maxFileSizeInBytesField.setAccessible(true);
    }
    @Test
    public void testConstructor()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();

        Assert.assertEquals("Default sample rate is incorrect", 44100, sampleRateInHertzField.get(pauseResumeAudioRecorder));
        Assert.assertEquals("Default channel config is incorrect", AudioFormat.CHANNEL_IN_MONO, channelConfigField.get(pauseResumeAudioRecorder));
        Assert.assertEquals("Default audio encoding is incorrect", AudioFormat.ENCODING_PCM_16BIT, audioEncodingField.get(pauseResumeAudioRecorder));
        Assert.assertEquals("Default audio file is incorrect", null, audioFileField.get(pauseResumeAudioRecorder));
        Assert.assertNull("Time completion listener is not null", onTimeCompletionListenerField.get(pauseResumeAudioRecorder));
        Assert.assertNull("Max File size reached listener is not null",onFileSizeReachedListenerField.get(pauseResumeAudioRecorder));
    }
    @Test
    public void testSetAudioEncoding()throws IllegalAccessException{
        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_8BIT);

        Assert.assertEquals("audio encoding is incorrect", AudioFormat.ENCODING_PCM_8BIT, audioEncodingField.get(pauseResumeAudioRecorder));

        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_16BIT);

        Assert.assertEquals("audio encoding is incorrect", AudioFormat.ENCODING_PCM_16BIT, audioEncodingField.get(pauseResumeAudioRecorder));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSetAudioEncodingInvalidInput(){pauseResumeAudioRecorder.setAudioEncoding(0);}
    @Test(expected = IllegalStateException.class)
    public void testSetAudioEncodingInvalidState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_8BIT);
    }
    @Test
    public void testSetMaxFileSizeInBytes()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setMaxFileSizeInBytes(2000L);

        Assert.assertEquals("remainingMaxTime incorrect", 2000L, (long) maxFileSizeInBytesField.get(pauseResumeAudioRecorder));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSetMaxFileSizeInBytesMin(){new PauseResumeAudioRecorder().setMaxFileSizeInBytes(999);}
    @Test(expected=IllegalArgumentException.class)
    public void testSetMaxFileSizeInBytesMax(){new PauseResumeAudioRecorder().setMaxFileSizeInBytes(PcmWavConverter.MAX_SIZE_WAV_FILE_BYTES + 1);}
    @Test(expected=IllegalStateException.class)
    public void testSetMaxFileSizeInBytesBadState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.setMaxFileSizeInBytes(1000L);
    }
    @Test
    public void testSetOnFileSizeReachedListener()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.INITIALIZED_STATE));
        final OnMaxFileSizeReachedListener onMaxFileSizeReachedListener=new OnMaxFileSizeReachedListener();
        pauseResumeAudioRecorder.setOnFileSizeReachedListener(onMaxFileSizeReachedListener);

        Assert.assertEquals("Listener incorrect", onMaxFileSizeReachedListener, onFileSizeReachedListenerField.get(pauseResumeAudioRecorder));

        pauseResumeAudioRecorder.setOnFileSizeReachedListener(null);

        Assert.assertEquals("Listener incorrect null case", null, onFileSizeReachedListenerField.get(pauseResumeAudioRecorder));
    }
    @Test(expected = IllegalStateException.class)
    public void testSetOnFileSizeReachedListenerInvalidState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.setOnFileSizeReachedListener(null);
    }
    @Test
    public void testSetOnTimeCompletionListener()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.INITIALIZED_STATE));
        final OnMaxTimeCompletionListener onMaxTimeCompletionListener=new OnMaxTimeCompletionListener();
        pauseResumeAudioRecorder.setOnTimeCompletionListener(onMaxTimeCompletionListener);

        Assert.assertEquals("Listener incorrect", onMaxTimeCompletionListener, onTimeCompletionListenerField.get(pauseResumeAudioRecorder));

        pauseResumeAudioRecorder.setOnTimeCompletionListener(null);

        Assert.assertEquals("Listener incorrect null case", null, onTimeCompletionListenerField.get(pauseResumeAudioRecorder));
    }
    @Test(expected = IllegalStateException.class)
    public void testSetOnTimeCompletionListenerInvalidState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.setOnTimeCompletionListener(null);
    }
    @Test
    public void testSetMaxTimeInMillis()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setMaxTimeInMillis(2000);

        Assert.assertEquals("remainingMaxTime incorrect", 2000, (long) remainingMaxTimeInMillisField.get(pauseResumeAudioRecorder));
        Assert.assertEquals("maxTime incorrect", 2000, (long) maxTimeInMillisField.get(pauseResumeAudioRecorder));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSetMaxTimeInMillisMin(){new PauseResumeAudioRecorder().setMaxTimeInMillis(999);}
    @Test(expected=IllegalArgumentException.class)
    public void testSetMaxTimeInMillisMax(){new PauseResumeAudioRecorder().setMaxTimeInMillis(PcmWavConverter.MAX_TIME_WAV_FILE_MILLIS+1);}
    @Test(expected=IllegalStateException.class)
    public void testSetMaxTimeInMillisBadState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.setMaxTimeInMillis(1000);
    }
    @Test
    public void testSetAudioFile()throws IllegalAccessException{
        pauseResumeAudioRecorder.setAudioFile("/recording.wav");

        Assert.assertEquals("Wrong state was set", PauseResumeAudioRecorder.PREPARED_STATE, pauseResumeAudioRecorder.getCurrentState());
        Assert.assertEquals("Wrong audio file was set", audioFileField.get(pauseResumeAudioRecorder), "/recording.pcm");

        pauseResumeAudioRecorder.setAudioFile("/recording");
        Assert.assertEquals("Wrong audio file was set", audioFileField.get(pauseResumeAudioRecorder), "/recording.pcm");
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSetAudioFileNullCase(){pauseResumeAudioRecorder.setAudioFile(null);}
    @Test(expected=IllegalArgumentException.class)
    public void testSetAudioFileEmptyCase(){pauseResumeAudioRecorder.setAudioFile("");}
    @Test(expected=IllegalArgumentException.class)
    public void testSetAudioFileBlankCase(){pauseResumeAudioRecorder.setAudioFile(" ");}
    @Test(expected = IllegalStateException.class)
    public void testSetAudioFileBadState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.setAudioFile("/recording.wav");
    }
    @Test
    public void testSetSampleRate()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();

        pauseResumeAudioRecorder.setSampleRate(22050);
        Assert.assertEquals("Sample rate is incorrect 22050", 22050, sampleRateInHertzField.get(pauseResumeAudioRecorder));

        pauseResumeAudioRecorder.setSampleRate(16000);
        Assert.assertEquals("Sample rate is incorrect 16000", 16000, sampleRateInHertzField.get(pauseResumeAudioRecorder));

        pauseResumeAudioRecorder.setSampleRate(11025);
        Assert.assertEquals("Sample rate is incorrect 11025", 11025, sampleRateInHertzField.get(pauseResumeAudioRecorder));

        pauseResumeAudioRecorder.setSampleRate(44100);
        Assert.assertEquals("Sample rate is incorrect 44100", 44100, sampleRateInHertzField.get(pauseResumeAudioRecorder));
    }
    @Test(expected = IllegalArgumentException.class)
    public void testSetSampleRateBadInput(){pauseResumeAudioRecorder.setSampleRate(0);}
    @Test(expected = IllegalStateException.class)
    public void testSetSampleRateBadState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.setSampleRate(44100);
    }
    @Test
    public void testSetChannel()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();

        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_STEREO);
        Assert.assertEquals("Invalid channel config stereo",AudioFormat.CHANNEL_IN_STEREO,channelConfigField.get(pauseResumeAudioRecorder));

        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_MONO);
        Assert.assertEquals("Invalid channel config mono", AudioFormat.CHANNEL_IN_MONO, channelConfigField.get(pauseResumeAudioRecorder));

        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_DEFAULT);
        Assert.assertEquals("Invalid channel config default", AudioFormat.CHANNEL_IN_DEFAULT, channelConfigField.get(pauseResumeAudioRecorder));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSetChannelBadParameter(){pauseResumeAudioRecorder.setChannel(Integer.MAX_VALUE);}
    @Test(expected = IllegalStateException.class)
    public void testSetChannelBadState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_MONO);
    }
    @Test
    public void testGetCurrentState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));

        Assert.assertEquals("States are not equal", PauseResumeAudioRecorder.RECORDING_STATE, ((AtomicInteger) currentAudioStateField.get(pauseResumeAudioRecorder)).get());
    }
    @Test
    public void testStartRecordingBadState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.INITIALIZED_STATE));
        pauseResumeAudioRecorder.startRecording();

        Assert.assertEquals("State was changed", PauseResumeAudioRecorder.INITIALIZED_STATE,pauseResumeAudioRecorder.getCurrentState());
    }
    @Test
    public void testStartRecording()throws NoSuchFieldException, IllegalAccessException,InterruptedException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setAudioFile(Environment.getExternalStorageDirectory() + "/recording.wav");
        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_STEREO);
        pauseResumeAudioRecorder.setSampleRate(44100);
        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_8BIT);

        pauseResumeAudioRecorder.startRecording();

        Assert.assertNotNull("Recording thread is not created", currentAudioRecordingThreadField.get(pauseResumeAudioRecorder));
        Assert.assertEquals("Correct state not set", PauseResumeAudioRecorder.RECORDING_STATE, pauseResumeAudioRecorder.getCurrentState());
        Assert.assertNotNull("Timer is null", onTimeCompletedTimerField.get(pauseResumeAudioRecorder));
        Assert.assertNotNull("TimerTask is null",onTimeCompletionTimerTaskField.get(pauseResumeAudioRecorder));
        Assert.assertTrue("start time not set",((long)recordingStartTimeMillisField.get(pauseResumeAudioRecorder))>=0);

        Thread.sleep(100);//Give it some time to create the file.

        //Thread's fields
        audioRecordThreadField=currentAudioRecordingThreadField.get(pauseResumeAudioRecorder).getClass().getDeclaredField("currentAudioRecording");
        audioRecordThreadField.setAccessible(true);
        AudioRecord audioRecord=(AudioRecord)audioRecordThreadField.get(currentAudioRecordingThreadField.get(pauseResumeAudioRecorder));
        ShadowAudioRecord shadowAudioRecord=Robolectric.shadowOf_(audioRecord);

        Assert.assertTrue("AudioRecord is not recording", shadowAudioRecord.isRecording);
        Assert.assertEquals("AudioRecord has wrong sample rate", 44100, shadowAudioRecord.sampleRateInHz);
        Assert.assertEquals("AudioRecord has wrong audio encoding", AudioFormat.ENCODING_PCM_8BIT, shadowAudioRecord.audioFormat);
        Assert.assertEquals("AudioRecord has wrong audio source", MediaRecorder.AudioSource.MIC,shadowAudioRecord.audioSource);
        Assert.assertEquals("AudioRecord has wrong channel config", AudioFormat.CHANNEL_IN_STEREO, shadowAudioRecord.channelConfig);

        final File pcmFile=new File(Environment.getExternalStorageDirectory() + "/recording.pcm");
        Assert.assertTrue(pcmFile.exists());
        pcmFile.delete();
    }
    @Test
    public void testStartRecordingWhileRecording()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setAudioFile(Environment.getExternalStorageDirectory() + "/recording.wav");
        currentAudioStateField.set(pauseResumeAudioRecorder,new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));

        pauseResumeAudioRecorder.startRecording();
        //Thread should not have been created in this situation.
        Assert.assertNull("Thread was created", currentAudioRecordingThreadField.get(pauseResumeAudioRecorder));
    }
    @Test
    public void testStartRecordingInvalidState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();//no file set
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.INITIALIZED_STATE));

        pauseResumeAudioRecorder.startRecording();

        Assert.assertEquals("State was changed.", PauseResumeAudioRecorder.INITIALIZED_STATE, pauseResumeAudioRecorder.getCurrentState());
    }
    @Test
    public void testPauseRecording()throws InterruptedException,IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setAudioFile(Environment.getExternalStorageDirectory() + "/recording.wav");
        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_STEREO);
        pauseResumeAudioRecorder.setSampleRate(44100);
        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_8BIT);
        pauseResumeAudioRecorder.startRecording();
        onTimeCompletedTimerField.set(pauseResumeAudioRecorder, mockTimer);

        Thread.sleep(100);
        pauseResumeAudioRecorder.pauseRecording();
        Thread.sleep(200);

        long remainingTimeMillis=((long) remainingMaxTimeInMillisField.get(pauseResumeAudioRecorder));
        Assert.assertTrue((remainingTimeMillis<PcmWavConverter.MAX_TIME_WAV_FILE_MILLIS-100) &&remainingTimeMillis>PcmWavConverter.MAX_TIME_WAV_FILE_MILLIS-4000);
        Mockito.verify(mockTimer,Mockito.times(1)).cancel();
        Assert.assertEquals("Correct state not set", PauseResumeAudioRecorder.PAUSED_STATE, pauseResumeAudioRecorder.getCurrentState());
        final File pcmFile=new File(Environment.getExternalStorageDirectory() + "/recording.pcm");
        pcmFile.delete();

        Thread.sleep(100);//Give it time to recreate the file if it is running incorrectly.

        Assert.assertFalse("Thread is not paused", pcmFile.exists());
    }
    @Test
    public void testPauseRecordingWhilePaused()throws IllegalAccessException{
        //make sure calling pause while paused does not result in a crash.
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.PAUSED_STATE));
        pauseResumeAudioRecorder.pauseRecording();

        new File(Environment.getExternalStorageDirectory() + "/recording.pcm").delete();
    }
    @Test
    public void testPauseRecordingInvalidState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.INITIALIZED_STATE));
        pauseResumeAudioRecorder.pauseRecording();

        Assert.assertEquals("State was changed", PauseResumeAudioRecorder.INITIALIZED_STATE, pauseResumeAudioRecorder.getCurrentState());
    }
    @Test
    public void testStopRecording()throws IllegalAccessException,NoSuchFieldException, InterruptedException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setAudioFile(Environment.getExternalStorageDirectory() + "/recording.wav");
        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_STEREO);
        pauseResumeAudioRecorder.setSampleRate(44100);
        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_8BIT);
        pauseResumeAudioRecorder.startRecording();
        Thread.sleep(100);
        audioRecordThreadField=currentAudioRecordingThreadField.get(pauseResumeAudioRecorder).getClass().getDeclaredField("currentAudioRecording");
        audioRecordThreadField.setAccessible(true);
        AudioRecord audioRecord=(AudioRecord)audioRecordThreadField.get(currentAudioRecordingThreadField.get(pauseResumeAudioRecorder));
        ShadowAudioRecord shadowAudioRecord=Robolectric.shadowOf_(audioRecord);

        pauseResumeAudioRecorder.stopRecording();

        Thread.sleep(5000);//Give it time to convert the file to wav and delete PCM.

        Assert.assertEquals("Wrong state was set", PauseResumeAudioRecorder.STOPPED_STATE, pauseResumeAudioRecorder.getCurrentState());
        Assert.assertFalse("PCM file still exists", new File(Environment.getExternalStorageDirectory() + "/recording.pcm").exists());
        Assert.assertTrue("WAV file does not exist", new File(Environment.getExternalStorageDirectory() + "/recording.wav").exists());

        Assert.assertFalse("AudioRecord is still recording",shadowAudioRecord.isRecording);

        new File(Environment.getExternalStorageDirectory() + "/recording.wav").delete();
    }
    @Test
    public void testStopRecordingInvalidState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.INITIALIZED_STATE));
        pauseResumeAudioRecorder.stopRecording();
        Assert.assertEquals("State was changed.",PauseResumeAudioRecorder.INITIALIZED_STATE,pauseResumeAudioRecorder.getCurrentState());
    }

    @Test
    public void testResumeRecording()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.PAUSED_STATE));

        pauseResumeAudioRecorder.resumeRecording();

        Assert.assertEquals("Wrong state was set", PauseResumeAudioRecorder.RECORDING_STATE, pauseResumeAudioRecorder.getCurrentState());
    }
    @Test
    public void testResumeRecordingInvalidState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.INITIALIZED_STATE));
        pauseResumeAudioRecorder.resumeRecording();
        Assert.assertEquals("State was changed",PauseResumeAudioRecorder.INITIALIZED_STATE,pauseResumeAudioRecorder.getCurrentState());
    }
    @Test
    public void testOnMaxTimeCompletionListener()throws InterruptedException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setAudioFile(Environment.getExternalStorageDirectory() + "/recording.wav");
        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_STEREO);
        pauseResumeAudioRecorder.setSampleRate(44100);
        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_8BIT);
        pauseResumeAudioRecorder.setMaxTimeInMillis(1000);
        pauseResumeAudioRecorder.setOnTimeCompletionListener(new OnMaxTimeCompletionListener());

        pauseResumeAudioRecorder.startRecording();
        Thread.sleep(1100);

        Assert.assertFalse("completion listener was not called", failTest);
        new File(Environment.getExternalStorageDirectory() + "/recording.wav").delete();
    }
    @Test
    public void testMaxFileSizeReached()throws InterruptedException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setAudioFile(Environment.getExternalStorageDirectory() + "/recording.wav");
        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_STEREO);
        pauseResumeAudioRecorder.setSampleRate(44100);
        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_8BIT);
        pauseResumeAudioRecorder.setMaxFileSizeInBytes(1000L);
        pauseResumeAudioRecorder.setOnFileSizeReachedListener(new OnMaxFileSizeReachedListener());

        pauseResumeAudioRecorder.startRecording();
        Thread.sleep(1100);

        Assert.assertFalse("completion listener was not called", failTest);
        new File(Environment.getExternalStorageDirectory() + "/recording.wav").delete();
    }
    private class OnMaxTimeCompletionListener implements PauseResumeAudioRecorder.OnTimeCompletionListener{
        @Override
        public void onTimeCompleted(PauseResumeAudioRecorder pauseResumeAudioRecorder) {
            failTest=false;
        }
    }
    private class OnMaxFileSizeReachedListener implements PauseResumeAudioRecorder.OnFileSizeReachedListener{
        @Override
        public void onFileSizeReached(PauseResumeAudioRecorder pauseResumeAudioRecorder) {
            failTest=false;
        }
    }

}

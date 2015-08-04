package republicofgavin.pauseresumeaudiorecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import republicofgavin.pauseresumeaudiorecorder.shadows.ShadowAudioRecord;

/**
 * Tests {@link PauseResumeAudioRecorder}
 * @author Gavin(republicofgavin@gmail.com)
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE,shadows=ShadowAudioRecord.class, emulateSdk = 18)
public class PauseResumeAudioRecorderTest {
    private PauseResumeAudioRecorder pauseResumeAudioRecorder;

    private Field sampleRateInHertzField;
    private Field channelConfigField;
    private Field audioEncodingField;
    private Field audioFileField;
    private Field currentAudioStateField;
    private Field currentAudioRecordingThreadField;
    //thread fields
    private Field audioRecordThreadField;

    @Before
    public void setup()throws NoSuchFieldException,IllegalAccessException{
        pauseResumeAudioRecorder=new PauseResumeAudioRecorder();

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

    }
    @Test
    public void testConstructor()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();

        Assert.assertEquals("Default sample rate is incorrect", 44100, sampleRateInHertzField.get(pauseResumeAudioRecorder));
        Assert.assertEquals("Default channel config is incorrect", AudioFormat.CHANNEL_IN_MONO, channelConfigField.get(pauseResumeAudioRecorder));
        Assert.assertEquals("Default audio encoding is incorrect", AudioFormat.ENCODING_PCM_16BIT, audioEncodingField.get(pauseResumeAudioRecorder));
        Assert.assertEquals("Default audio file is incorrect", null, audioFileField.get(pauseResumeAudioRecorder));
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
    @Test(expected = IllegalStateException.class)
    public void testStartRecordingBadState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.PAUSED_STATE));
        pauseResumeAudioRecorder.startRecording();
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
        Assert.assertNull("Thread was created",currentAudioRecordingThreadField.get(pauseResumeAudioRecorder));
    }
    @Test(expected = IllegalStateException.class)
    public void testStartRecordingInvalidState()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();//no file set
        pauseResumeAudioRecorder.startRecording();
    }
    @Test
    public void testPauseRecording()throws InterruptedException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        pauseResumeAudioRecorder.setAudioFile(Environment.getExternalStorageDirectory() + "/recording.wav");
        pauseResumeAudioRecorder.setChannel(AudioFormat.CHANNEL_IN_STEREO);
        pauseResumeAudioRecorder.setSampleRate(44100);
        pauseResumeAudioRecorder.setAudioEncoding(AudioFormat.ENCODING_PCM_8BIT);
        pauseResumeAudioRecorder.startRecording();

        Thread.sleep(100);
        pauseResumeAudioRecorder.pauseRecording();

        Assert.assertEquals("Correct state not set", PauseResumeAudioRecorder.PAUSED_STATE, pauseResumeAudioRecorder.getCurrentState());

        final File pcmFile=new File(Environment.getExternalStorageDirectory() + "/recording.pcm");
        pcmFile.delete();

        Thread.sleep(100);//Give it time to recreate the file if it is running incorrectly.

        Assert.assertFalse("Thread is not paused",pcmFile.exists());
    }
    @Test
    public void testPauseRecordingWhilePaused()throws IllegalAccessException{
        //make sure calling pause while paused does not result in a crash.
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.PAUSED_STATE));
        pauseResumeAudioRecorder.pauseRecording();

        new File(Environment.getExternalStorageDirectory() + "/recording.pcm").delete();
    }
    @Test(expected=IllegalStateException.class)
    public void testPauseRecordingInvalidState()throws IllegalAccessException{new PauseResumeAudioRecorder().pauseRecording();}
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
    public void testStopRecordingWhileStopped()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.STOPPED_STATE));
        pauseResumeAudioRecorder.stopRecording();
    }
    @Test(expected = IllegalStateException.class)
    public void testStopRecordingInvalidState(){new PauseResumeAudioRecorder().stopRecording();}

    @Test
    public void testResumeRecording()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.PAUSED_STATE));

        pauseResumeAudioRecorder.resumeRecording();

        Assert.assertEquals("Wrong state was set", PauseResumeAudioRecorder.RECORDING_STATE, pauseResumeAudioRecorder.getCurrentState());
    }
    @Test
    public void testResumeRecordingWhileRecording()throws IllegalAccessException{
        PauseResumeAudioRecorder pauseResumeAudioRecorder=new PauseResumeAudioRecorder();
        currentAudioStateField.set(pauseResumeAudioRecorder, new AtomicInteger(PauseResumeAudioRecorder.RECORDING_STATE));
        pauseResumeAudioRecorder.resumeRecording();
    }
    @Test(expected = IllegalStateException.class)
    public void testResumeRecordingInvalidState(){new PauseResumeAudioRecorder().resumeRecording();}

}

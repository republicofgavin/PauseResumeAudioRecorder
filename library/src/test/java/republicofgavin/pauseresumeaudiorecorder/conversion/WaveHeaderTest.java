package republicofgavin.pauseresumeaudiorecorder.conversion;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests {@link PcmWavConverter}
 * @author (Gavin)republicofgavin@gmail.com
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class WaveHeaderTest {

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadByteNumber(){ new PcmWavConverter.WaveHeader(44100,(short)1,(short)8,-2); }
    @Test (expected = IllegalArgumentException.class)
    public void testConstructorBadChannelNum(){new PcmWavConverter.WaveHeader(22050,(short)3,(short)8,1000);}
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadSampleRateInHtz(){new PcmWavConverter.WaveHeader(22051,(short)2,(short)16,1000);}
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadBitRate(){new PcmWavConverter.WaveHeader(16000,(short)1,(short)32,1000);}
    @Test
    public void testConstructorAndGetters(){
        PcmWavConverter.WaveHeader waveHeader= new PcmWavConverter.WaveHeader(11025,(short)1,(short)8,1337);

        Assert.assertEquals("Incorrect sample rate",11025,waveHeader.getSampleRateInHertz());
        Assert.assertEquals("Incorrect channel number",1,waveHeader.getChannelNum());
        Assert.assertEquals("Incorrect bit rate",8,waveHeader.getBitRate());
        Assert.assertEquals("Incorrect byte number",1337,waveHeader.getByteNumber());
    }
}

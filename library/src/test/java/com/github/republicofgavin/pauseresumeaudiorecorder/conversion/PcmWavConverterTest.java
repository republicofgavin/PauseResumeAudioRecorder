package com.github.republicofgavin.pauseresumeaudiorecorder.conversion;

import android.os.Environment;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests {@link PcmWavConverter}
 * @author (Gavin)republicofgavin@gmail.com
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class PcmWavConverterTest {

    private PcmWavConverter.WaveHeader waveHeader;
    private short[] pcmDataSample=new short[]{8,7,1,2,5,6};
    private static final String TEST_DIRECTORY_PATH=Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"Test";
    @Before
    public void setup(){
        waveHeader=new PcmWavConverter.WaveHeader(11025,(short)1,(short)8,-1);
        File file=new File(TEST_DIRECTORY_PATH);
        file.mkdirs();
    }
    @After
    public void tearDown() throws IOException{
        recursiveFileDelete(new File(TEST_DIRECTORY_PATH));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullWaveHeader()throws IOException{PcmWavConverter.convertPCMToWav(null, "/x/something/x/Gavin.pcm", "/x/something/x/Gavin.wav");}
    @Test(expected=IllegalArgumentException.class)
    public void testNullPCMPath()throws IOException{PcmWavConverter.convertPCMToWav(waveHeader,null,"/x/something/x/Gavin.wav");}
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyPCMPath()throws IOException{PcmWavConverter.convertPCMToWav(waveHeader, "", "/x/something/x/Gavin.wav");}
    @Test(expected=IllegalArgumentException.class)
    public void testBlankPCMPath()throws IOException{PcmWavConverter.convertPCMToWav(waveHeader," ","/x/something/x/Gavin.wav");}

    @Test(expected=IllegalArgumentException.class)
    public void testNullWAVPath()throws IOException{PcmWavConverter.convertPCMToWav(waveHeader,"/x/something/x/Gavin.pcm",null);}
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyWAVPath()throws IOException{PcmWavConverter.convertPCMToWav(waveHeader,"/x/something/x/Gavin.pcm","");}
    @Test(expected=IllegalArgumentException.class)
    public void testBlankWAVPath()throws IOException{PcmWavConverter.convertPCMToWav(waveHeader,"/x/something/x/Gavin.pcm","  ");}

    @Test
    public void testPcmWavConversion()throws IOException{
        final String pcmFilePath=Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"Test"+ File.separator+"Test.pcm";
        createPCMFile(pcmFilePath);
        final String wavFilePath=pcmFilePath.replace(".pcm", ".wav");

        PcmWavConverter.convertPCMToWav(waveHeader, pcmFilePath, wavFilePath);

        final DataInputStream dataInputStream=new DataInputStream(new BufferedInputStream(new FileInputStream(wavFilePath)));

        //header
        byte[] expectedByteArray=new byte["RIFF".getBytes().length];
        dataInputStream.read(expectedByteArray);
        assertArrayEquals("RIFF is not there or in correct position.", "RIFF".getBytes(), expectedByteArray);

        Assert.assertEquals("File length is not there or in correct position", Integer.reverseBytes(36 + (int) new File(pcmFilePath).length()), dataInputStream.readInt());

        expectedByteArray=new byte["WAVE".getBytes().length];
        dataInputStream.read(expectedByteArray);
        assertArrayEquals("WAVE is not there or in correct position.", "WAVE".getBytes(), expectedByteArray);

        expectedByteArray=new byte["fmt ".getBytes().length];
        dataInputStream.read(expectedByteArray);
        assertArrayEquals("fmt  is not there or in correct position.", "fmt ".getBytes(), expectedByteArray);

        Assert.assertEquals("16 is not there or in correct position.", Integer.reverseBytes(16), dataInputStream.readInt());
        Assert.assertEquals("PCM format is not there or in correct position.", Short.reverseBytes((short) 1), dataInputStream.readShort());
        Assert.assertEquals("Channel num is not there or in correct position.", Short.reverseBytes(waveHeader.getChannelNum()), dataInputStream.readShort());
        Assert.assertEquals("Sample rate is not there or in correct position.",Integer.reverseBytes(waveHeader.getSampleRateInHertz()),dataInputStream.readInt());
        Assert.assertEquals("Data calculation is not there or in correct position.",Integer.reverseBytes(waveHeader.getChannelNum() * waveHeader.getSampleRateInHertz() * waveHeader.getBitRate() / 8),dataInputStream.readInt());
        Assert.assertEquals("Channel num and bit rate calculation is not there or in correct position.", Short.reverseBytes((short) (waveHeader.getChannelNum() * waveHeader.getBitRate() / 8)),dataInputStream.readShort());
        Assert.assertEquals("Bit rate is not there or in correct position",Short.reverseBytes(waveHeader.getBitRate()),dataInputStream.readShort());

        expectedByteArray=new byte["data".getBytes().length];
        dataInputStream.read(expectedByteArray);
        assertArrayEquals("data is not there or in correct position.", "data".getBytes(), expectedByteArray);

        Assert.assertEquals("File size is not there or in correct position.",Integer.reverseBytes((int) new File(pcmFilePath).length()),dataInputStream.readInt());
        //PCM data
        int i=0;
        while (dataInputStream.available()>0) {
            ByteBuffer bb = ByteBuffer.allocate(2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put(dataInputStream.readByte());
            bb.put(dataInputStream.readByte());
            Assert.assertEquals(pcmDataSample[i++],bb.getShort(0));
        }
    }
    private void recursiveFileDelete(final File currentFile) throws IOException{
        if (currentFile.isDirectory()){
            for (final File file:currentFile.listFiles()){
                recursiveFileDelete(file);
            }
        }
        if (currentFile.exists() && !currentFile.delete()){
            throw new IOException("Unable to delete file:"+currentFile.getName());
        }
    }
    private void createPCMFile(final String filePath)throws IOException{
        final DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
        try {
            for (final short data : pcmDataSample) {
                dataOutputStream.writeShort(data);
            }
        }
        finally{
            dataOutputStream.flush();
            dataOutputStream.close();
        }
    }
}

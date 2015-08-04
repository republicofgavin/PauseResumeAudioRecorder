package republicofgavin.pauseresumeaudiorecorder.conversion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Converts PCM (Big Endian format) files to WAV (Little Endian format).
 * @author (Gavin)republicofgavin@gmail.com
 */
public class PcmWavConverter {

    private static final short PCM_FORMAT=1;

    /**
     *
     * @param waveHeader A {@link PcmWavConverter.WaveHeader} composed of the format of data location at the pcmFilePath. Cannot be null.
     * @param pcmFilePath The absolute path to the PCM file. Cannot be: null, empty, blank. It is recommended that the file have a .pcm suffix.
     * @param wavFilePath The absolute path to where the WAV file will be created. Directory path should already be created. String cannot be: null, empty, blank. It is recommended that the file have a .wavs uffix.
     * @throws IOException If there is a problem reading/writing between the PCM and WAV files. Such as the WAV file already existing or the PCM file not existing. Or if one of them is a directory.
     * @throws IllegalArgumentException If the parameters are invalid.
     */
    public static void convertPCMToWav(WaveHeader waveHeader,final String pcmFilePath, final String wavFilePath)throws IOException{
        if (waveHeader ==null){
            throw new IllegalArgumentException("waveHeader cannot be null");
        }

        if (pcmFilePath==null || pcmFilePath.trim().isEmpty()){
            throw new IllegalArgumentException("pcmFilePath cannot be null, empty, blank");
        }
        final File pcmFile=new File(pcmFilePath);

        if (wavFilePath==null || wavFilePath.trim().isEmpty()){
            throw new IllegalArgumentException("wavFilePath cannot be null, empty, blank");
        }
        final DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(wavFilePath)));
        final DataInputStream dataInputStream=new DataInputStream(new BufferedInputStream(new FileInputStream(pcmFile)));
        try {
            //NOTE: The PCM data recording format data as Big Endian. However, WAV files require it in Little Endian, so, it is inverted.
            //Has to be integer (http://www.topherlee.com/software/pcm-tut-wavformat.html), so if cast fails, it is too big to be a wav file.
            final int numberOfBytes = (waveHeader.byteNumber == -1) ? ((int) pcmFile.length()) : waveHeader.byteNumber;

            dataOutputStream.writeBytes("RIFF");
            dataOutputStream.writeInt(Integer.reverseBytes(36+numberOfBytes));
            dataOutputStream.writeBytes("WAVE");

            dataOutputStream.writeBytes("fmt ");
            dataOutputStream.writeInt(Integer.reverseBytes(16));
            dataOutputStream.writeShort(Short.reverseBytes(PCM_FORMAT));
            dataOutputStream.writeShort(Short.reverseBytes(waveHeader.channelNum));
            dataOutputStream.writeInt(Integer.reverseBytes(waveHeader.sampleRateInHertz));
            dataOutputStream.writeInt(Integer.reverseBytes(waveHeader.channelNum * waveHeader.sampleRateInHertz * waveHeader.bitRate / 8));

            dataOutputStream.writeShort(Short.reverseBytes((short) (waveHeader.channelNum * waveHeader.bitRate / 8)));
            dataOutputStream.writeShort(Short.reverseBytes(waveHeader.bitRate));

            dataOutputStream.writeBytes("data");
            dataOutputStream.writeInt(Integer.reverseBytes(numberOfBytes));

            writePCMData(dataOutputStream, dataInputStream);
        }
        finally {
            dataOutputStream.flush();
            dataOutputStream.close();
            dataInputStream.close();
        }

    }
    private static void writePCMData(final DataOutputStream out, final DataInputStream in)throws IOException{
        while (in.available()>0){
            final short data=in.readShort();
            out.writeByte(data & 0xFF);
            out.writeByte((data >> 8) & 0xFF);
        }
    }

    /**
     * Object that represents the header of a .wav file.
     * @author (Gavin)republicofgavin@gmail.com
     */
    public static class WaveHeader{
        private int byteNumber;
        private int sampleRateInHertz;
        private short channelNum;
        private short bitRate;

        /**
         *
         * @param sampleRateInHertz The rate at which the recording samples audio data. Valid values are: 44100, 22050, 16000, 11025 hertz.
         * @param channelNum The type of audio channel the .PCM file uses (Mono(1) or Stereo(2))
         * @param bitRate The bit rate of the PCM file (8 or 16).
         * @throws IllegalArgumentException If any parameters are invalid.
         */
        public WaveHeader(final int sampleRateInHertz, final short channelNum, final short bitRate){
            this(sampleRateInHertz, channelNum, bitRate,-1);
        }

        /**
         *
         * @param sampleRateInHertz The rate at which the recording samples audio data. Valid values are: 44100, 22050, 16000, 11025 hertz.
         * @param channelNum The type of audio channel the .PCM file uses (Mono(1) or Stereo(2))
         * @param bitRate The bit rate of the PCM file (8 or 16).
         * @param byteNumber The number of bytes in the PCM file. -1 for the converter to find the file size for you. Anything lower than -1 not allowed.
         * @throws IllegalArgumentException If any parameters are invalid.
         */
        public WaveHeader(final int sampleRateInHertz, final short channelNum, final short bitRate,final int byteNumber){
            if (channelNum!=1 && channelNum !=2){
                throw new IllegalArgumentException("Channel number must be 1(mono) or 2(stereo)");
            }
            this.channelNum=channelNum;

            if (sampleRateInHertz!=44100 && sampleRateInHertz !=22050
                    && sampleRateInHertz != 16000 && sampleRateInHertz !=11025){
                throw new IllegalArgumentException("Invalid sample rate given");
            }
            this.sampleRateInHertz=sampleRateInHertz;

            if (bitRate !=8 && bitRate !=16){
                throw new IllegalArgumentException("Invalid bit rate (must be 8 or 16)");
            }
            this.bitRate=bitRate;

            if (byteNumber<-1){
                throw new IllegalArgumentException("Invalid number of bytes for file.");
            }
            this.byteNumber=byteNumber;
        }

        /**
         * The number of bytes the WAV header represents. -1 if it doesn't know, but will determine upon PCM conversion.
         * @return A number equal to -1 or greater.
         */
        public int getByteNumber() {
            return byteNumber;
        }

        /**
         * The rate at which the recording samples audio data.
         * @return 44100, 22050, 16000, or 11025
         */
        public int getSampleRateInHertz() {
            return sampleRateInHertz;
        }

        /**
         * The type of audio channel the .PCM file uses .
         * @return Mono(1) or Stereo(2)
         */
        public short getChannelNum() {
            return channelNum;
        }

        /**
         * bitRate The bit rate of the PCM file.
         * @return 8 or 16
         */
        public short getBitRate() {
            return bitRate;
        }
    }
}

package sproj.util;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.*;

import javax.sound.sampled.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * With code from https://github.com/bytedeco/javacv/blob/master/samples/WebcamAndMicrophoneCapture.java
 *
 *
 */

public class TestCameraRecorder {

//    private final static int WEBCAM_DEVICE_INDEX = 1;       // 0 is the laptop webcam --> on desktop it'll depend if a monitor is plugged in that has a webcam
    private final static int AUDIO_DEVICE_INDEX = 4;

    private final static int FRAME_RATE = 30;
    private final static int GOP_LENGTH_IN_FRAMES = 60;

    private final String VIDEO_FORMAT = "mp4"; // flv

    private static long startTime = 0;
    private static long videoTS = 0;

    private FrameGrabber frameGrabber;
    private FFmpegFrameRecorder recorder;


    private final int CAPTURE_WIDTH = 1280;
    private final int CAPTURE_HEIGHT = 720;


    public TestCameraRecorder(String videoPath) throws IOException {


        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("/home/alex/Videos/tadpole_test_videos/firstTestVids/IMG_4887.MOV");
        grabber.start();

        System.out.println("Got here");


    }

    private void initializeGrabber(String videoPath, int camIndx) throws FrameGrabber.Exception {
        frameGrabber = new OpenCVFrameGrabber(camIndx);       // ffmpeggrabber works better
//        frameGrabber = new FFmpegFrameGrabber(videoPath);
//         frameGrabber.setFormat("MOV");
//        frameGrabber.setImageWidth(CAPTURE_WIDTH);
//        frameGrabber.setImageHeight(CAPTURE_HEIGHT);
//        frameGrabber.getFormatContext();
        frameGrabber.start();
    }


    public static void blah(String[] args) throws Exception {

        int camIndx = 0;
        int currentNumberGroup = 1;     // group of 1 tadpole
        int numberOfTrialsToRecord = 20;
        long lengthOfEachVideo = 30 * 1000;   // 30 seconds
        String savePrefix = String.format("/home/alex/Videos/tadpole_test_videos/videoTrials/1tadpoles/tadpoles%d", currentNumberGroup);

        String videoPath = "/dev/video0";

        for (int i=0; i<numberOfTrialsToRecord; i++) {

            TestCameraRecorder videoRecorder = new TestCameraRecorder(videoPath);
            videoRecorder.initializeGrabber(videoPath, camIndx);

            String savePath = String.format("%s_%d", savePrefix, i+1);
            videoRecorder.recordVideo(lengthOfEachVideo, savePath);


            System.out.print("\r" + (i + 1) + " of " + numberOfTrialsToRecord + " videos recorded");
        }
    }


    private void setUpRecorder(String videoPath) throws FrameRecorder.Exception {
        // org.bytedeco.javacv.FFmpegFrameRecorder.FFmpegFrameRecorder(String
        // filename, int imageWidth, int imageHeight, int audioChannels)
        // For each param, we're passing in...
        // filename = either a path to a local file we wish to create, or an
        // RTMP url to an FMS / Wowza server
        // imageWidth = width we specified for the grabber
        // imageHeight = height we specified for the grabber
        // audioChannels = 2, because we like stereo
        recorder = new FFmpegFrameRecorder(videoPath, CAPTURE_WIDTH, CAPTURE_HEIGHT, 2);
        recorder.setInterleaved(true);

        // decrease "startup" latency in FFMPEG (see:
        // https://trac.ffmpeg.org/wiki/StreamingGuide)
        recorder.setVideoOption("tune", "zerolatency");

        // tradeoff between quality and encode speed
        // possible values are ultrafast,superfast, veryfast, faster, fast,
        // medium, slow, slower, veryslow
        // ultrafast offers us the least amount of compression (lower encoder
        // CPU) at the cost of a larger stream size
        // at the other end, veryslow provides the best compression (high
        // encoder CPU) while lowering the stream size
        // (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
        recorder.setVideoOption("preset", "ultrafast");

        // Constant Rate Factor (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
        recorder.setVideoOption("crf", "28");

        // 2000 kb/s, reasonable "sane" area for 720
        recorder.setVideoBitrate(2000000);

        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);

        recorder.setFormat(VIDEO_FORMAT);

        // FPS (frames per second)
        recorder.setFrameRate(FRAME_RATE);

        // Key frame interval, in our case every 2 seconds -> 30 (fps) * 2 = 60
        // (gop length)
        recorder.setGopSize(GOP_LENGTH_IN_FRAMES);

        // We don't want variable bitrate audio
        recorder.setAudioOption("crf", "0");
        // Highest quality
        recorder.setAudioQuality(0);
        // 192 Kbps
        recorder.setAudioBitrate(192000);
        recorder.setSampleRate(44100);
        recorder.setAudioChannels(2);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);

        // Jack 'n coke... do it...
        recorder.start();
    }


    public void recordVideo(long duration, String savePath) throws IOException, InterruptedException {

        setUpRecorder(savePath);

        // The available FrameGrabber classes include OpenCVFrameGrabber (opencv_videoio),
        // DC1394FrameGrabber, FlyCaptureFrameGrabber, OpenKinectFrameGrabber,
        // PS3EyeFrameGrabber, VideoInputFrameGrabber, and FFmpegFrameGrabber.

//        frameGrabber = new OpenCVFrameGrabber(1);
//        frameGrabber.setImageWidth(captureWidth);
//        frameGrabber.setImageHeight(captureHeight);
//        frameGrabber.start();

        /*
        // org.bytedeco.javacv.FFmpegFrameRecorder.FFmpegFrameRecorder(String
        // filename, int imageWidth, int imageHeight, int audioChannels)
        // For each param, we're passing in...
        // filename = either a path to a local file we wish to create, or an
        // RTMP url to an FMS / Wowza server
        // imageWidth = width we specified for the frameGrabber
        // imageHeight = height we specified for the frameGrabber
        // audioChannels = 2, because we like stereo
        recorder = new FFmpegFrameRecorder(
                savePath,
                captureWidth, captureHeight, 2);
        recorder.setInterleaved(true);

        // decrease "startup" latency in FFMPEG (see:
        // https://trac.ffmpeg.org/wiki/StreamingGuide)
        recorder.setVideoOption("tune", "zerolatency");

        // tradeoff between quality and encode speed
        // possible values are ultrafast,superfast, veryfast, faster, fast,
        // medium, slow, slower, veryslow
        // ultrafast offers us the least amount of compression (lower encoder
        // CPU) at the cost of a larger stream size
        // at the other end, veryslow provides the best compression (high
        // encoder CPU) while lowering the stream size
        // (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
        recorder.setVideoOption("preset", "ultrafast");

        // Constant Rate Factor (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
        recorder.setVideoOption("crf", "28");

        // 2000 kb/s, reasonable "sane" area for 720
        recorder.setVideoBitrate(2000000);

        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);

        recorder.setFormat("flv");

        // FPS (frames per second)
        recorder.setFrameRate(FRAME_RATE);

        // Key frame interval, in our case every 2 seconds -> 30 (fps) * 2 = 60
        // (gop length)
        recorder.setGopSize(GOP_LENGTH_IN_FRAMES);

        // We don't want variable bitrate audio
        recorder.setAudioOption("crf", "0");
        // Highest quality
        recorder.setAudioQuality(0);
        // 192 Kbps
        recorder.setAudioBitrate(192000);
        recorder.setSampleRate(44100);
        recorder.setAudioChannels(2);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);

        // Jack 'n coke... do it...
        recorder.start();

        */


        // Thread for audio capture, this could be in a nested private class if you prefer...
        new Thread(() -> {
            // Pick a format...
            // NOTE: It is better to enumerate the formats that the system supports,
            // because getLine() can error out with any particular format...
            // For us: 44.1 sample rate, 16 bits, stereo, signed, little endian
            AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);

            // Get TargetDataLine with that format
            Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
            Mixer mixer = AudioSystem.getMixer(minfoSet[AUDIO_DEVICE_INDEX]);
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

            try
            {
                // Open and start capturing audio
                // It's possible to have more control over the chosen audio device with this line:
                // TargetDataLine line = (TargetDataLine)mixer.getLine(dataLineInfo);
                TargetDataLine line = (TargetDataLine)AudioSystem.getLine(dataLineInfo);
                line.open(audioFormat);
                line.start();

                int sampleRate = (int) audioFormat.getSampleRate();
                int numChannels = audioFormat.getChannels();

                // Let's initialize our audio buffer...
                int audioBufferSize = sampleRate * numChannels;
                byte[] audioBytes = new byte[audioBufferSize];

                // Using a ScheduledThreadPoolExecutor vs a while loop with
                // a Thread.sleep will allow
                // us to get around some OS specific timing issues, and keep
                // to a more precise
                // clock as the fixed rate accounts for garbage collection
                // time, etc
                // a similar approach could be used for the webcam capture
                // as well, if you wish
                ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                exec.scheduleAtFixedRate(() -> {
                    try
                    {
                        // Read from the line... non-blocking
                        int nBytesRead = 0;
                        while (nBytesRead == 0) {
                            nBytesRead = line.read(audioBytes, 0, line.available());
                        }

                        // Since we specified 16 bits in the AudioFormat,
                        // we need to convert our read byte[] to short[]
                        // (see source from FFmpegFrameRecorder.recordSamples for AV_SAMPLE_FMT_S16)
                        // Let's initialize our short[] array
                        int nSamplesRead = nBytesRead / 2;
                        short[] samples = new short[nSamplesRead];

                        // Let's wrap our short[] into a ShortBuffer and
                        // pass it to recordSamples
                        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                        ShortBuffer sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);

                        // recorder is instance of
                        // org.bytedeco.javacv.FFmpegFrameRecorder
                        recorder.recordSamples(sampleRate, numChannels, sBuff);
                    }
                    catch (FrameRecorder.Exception e)
                    {
                        e.printStackTrace();
                    }
                }, 0, (long) 1000 / FRAME_RATE, TimeUnit.MILLISECONDS);
            }
            catch (LineUnavailableException e1)
            {
                e1.printStackTrace();
            }
        }).start();

        // A really nice hardware accelerated component for our preview...
        CanvasFrame cFrame = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / frameGrabber.getGamma());

        Frame capturedFrame;

        // While we are capturing...
        while ((capturedFrame = frameGrabber.grab()) != null) {

            if (videoTS >= duration) {
                System.out.println("video time stamp: " + videoTS + " stopping.");
                break;
            }

            if (cFrame.isVisible())
            {
                // Show our frame in the preview
                cFrame.showImage(capturedFrame);
            }

            // Let's define our start time...
            // This needs to be initialized as close to when we'll use it as
            // possible,
            // as the delta from assignment to computed time could be too high
            if (startTime == 0)
                startTime = System.currentTimeMillis();

            // Create timestamp for this frame
            videoTS = 1000 * (System.currentTimeMillis() - startTime);

            // Check for AV drift
            if (videoTS > recorder.getTimestamp())
            {
                System.out.println(
                        "Lip-flap correction: "
                                + videoTS + " : "
                                + recorder.getTimestamp() + " -> "
                                + (videoTS - recorder.getTimestamp()));

                // We tell the recorder to write this frame at this timestamp
                recorder.setTimestamp(videoTS);
            }

            // Send the frame to the org.bytedeco.javacv.FFmpegFrameRecorder
            recorder.record(capturedFrame);

            KeyEvent keyPressed = cFrame.waitKey(10);
            if (keyPressed != null) {

                int keyChar = keyPressed.getKeyCode();

                System.out.println(keyChar);

                if (keyChar == KeyEvent.VK_ESCAPE) {
                    break;
                }

                switch(keyChar) {

//                    case KeyEvent.VK_ESCAPE: break;      // hold escape key or 'q' to quit
                    case KeyEvent.VK_Q: break;

                    case KeyEvent.VK_P: ;// pause? ;
                }

            }
        }

        cFrame.dispose();
        recorder.stop();
        frameGrabber.stop();
    }
}
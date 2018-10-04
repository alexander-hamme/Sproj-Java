package sproj.util;

import org.bytedeco.javacv.*;

class VideoTest {
    public static void main(String[] args) throws Exception  {
        try {
//            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(1);
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("/dev/video1");
//            FlyCapture2FrameGrabber grabber = new FlyCapture2FrameGrabber(1);
            grabber.start();
            Frame frame = grabber.grab();
            CanvasFrame canvasFrame = new CanvasFrame("Video with JavaCV");
            canvasFrame.setCanvasSize(frame.imageWidth, frame.imageHeight);
            grabber.setFrameRate(grabber.getFrameRate());

            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("/home/alex/Videos/tadpole_test_videos/videoTrials/1tadpoles/1.mp4", grabber.getImageWidth(), grabber.getImageHeight());  // specify your path
            recorder.setFormat("mp4");
            recorder.setFrameRate(30);
            recorder.setVideoBitrate(10 * 1024 * 1024);

            recorder.start();
            while (canvasFrame.isVisible() && (frame = grabber.grab()) != null) {
                canvasFrame.showImage(frame);
                recorder.record(frame);
            }
            recorder.stop();
            grabber.stop();
            canvasFrame.dispose();

        } catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }
}
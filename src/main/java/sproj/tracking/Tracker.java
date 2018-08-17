package sproj.tracking;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;

import org.deeplearning4j.nn.layers.objdetect.DetectedObject;

import sproj.util.BoundingBox;
import sproj.util.DetectionsParser;
import sproj.yolo_porting_attempts.YOLOModelContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.bytedeco.javacpp.opencv_highgui.destroyAllWindows;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.opencv.imgproc.Imgproc.LINE_AA;


/**
 * This class iterates through the input video feed (from a file or a camera device),
 * and implements tracking functions to record the movement data of the subject animals.
 *
 * The recorded data is intermittently passed to the IOUtils class to be written (or appended) to file.
 */
public class Tracker {


    private static final Logger logger = LogManager.getLogger("Tracker");

    final double DISPL_THRESH_FRACT = 1.5;      // used for distance thresholding
    final int DISPL_THRESH = 80;
    final int ARRAY_MAX_SIZE = 60;              // buffer size of array to accumulate data
    final int frame_resize_width = 720;
    boolean DRAW_SHAPES = true;
    boolean DRAW_RECTANGLES = false;
    int circleRadius = 5;

    private String CANVAS_NAME = "Tadpole Tracker";

    private final int IMG_WIDTH = YOLOModelContainer.IMG_WIDTH;
    private final int IMG_HEIGHT = YOLOModelContainer.IMG_HEIGHT;
    private int INPUT_FRAME_WIDTH;
    private int INPUT_FRAME_HEIGHT;

    private int WINDOW_WIDTH = 720;     // ask user for size
    private int WINDOW_HEIGHT = 720;     // ask user for size

    private YOLOModelContainer yoloModelContainer = new YOLOModelContainer();
    private ArrayList<Animal> animals = new ArrayList<>();

    public FFmpegFrameGrabber grabber;
    private DetectionsParser detectionsParser = new DetectionsParser();
    private OpenCVFrameConverter frameConverter = new OpenCVFrameConverter.ToMat();

    private int number_of_objs;


    public Tracker(int n_objs, boolean display) throws IOException {
        this.number_of_objs = n_objs;
        this.DRAW_SHAPES = display;

    }

    private void setup(int width, int height) {
        int[][] colors = {{100, 100, 100}, {90, 90, 90}, {255, 0, 255}, {0, 255, 255}, {0, 0, 255}, {47, 107, 85},
                {113, 179, 60}, {255, 0, 0}, {255, 255, 255}, {0, 180, 0}, {255, 255, 0}, {160, 160, 160},
                {160, 160, 0}, {0, 0, 0}, {202, 204, 249}, {0, 255, 127}, {40, 46, 78}};

        int x, y;

        for (int i = 0; i < number_of_objs; i++) {
            x = (int) ((i + 1) / ((double) number_of_objs * width));            // distribute animal objects diagonally across screen
            y = (int) ((i + 1) / ((double) number_of_objs * height));
            this.animals.add(new Animal(x, y, colors[i]));
        }
    }


    public void trackVideo(String videoPath, int[] cropDimensions, CanvasFrame canvasFrame) throws InterruptedException, IOException {

        int msDelay = 10;

        // todo should this be in the constructor, or in a different class?
        setup(cropDimensions[2], cropDimensions[3]);

        List<BoundingBox> boundingBoxes;
        List<DetectedObject> detectedObjects;


        //TODO      use Range instead of Rect?
        Rect cropRect = new Rect(cropDimensions[0], cropDimensions[1], cropDimensions[2], cropDimensions[3]);
        //  new Range(300,600), new Range(200,400)); //

        grabber = new FFmpegFrameGrabber(videoPath);        // OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(videoPath);
        grabber.start();    // open video file

        INPUT_FRAME_WIDTH = grabber.getImageWidth();        // todo are these necessary?
        INPUT_FRAME_HEIGHT = grabber.getImageHeight();

        BytePointer windowPointer = new BytePointer(CANVAS_NAME);

        // TODO    bytepointer??
        opencv_highgui.namedWindow(windowPointer);

//        opencv_highgui.namedWindow(CANVAS_NAME);   //, int i?     todo   difference with cvNamedWindow?
        opencv_highgui.resizeWindow(windowPointer, WINDOW_WIDTH, WINDOW_HEIGHT);




        /**         TODO    SWITCH BACK TO CANVASFRAME???        look at all its methods!!!



        canvasFrame.setContentPane(new Container());
        canvasFrame.setIconImage(new Image());                      // convert frame to Graphics Object??
        canvasFrame.setLayeredPane(new JLayeredPane().paint(new Graphics()););
        canvasFrame.setGlassPane();


        Component frameContainer = new Component() {
            @Override
            public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
                return super.imageUpdate(img, infoflags, x, y, w, h);
            }
        };
        canvasFrame.update(new Graphics().create());
        canvasFrame.createImage(new ImageProducer());
        canvasFrame.add("frame", frameContainer);
         */

//        opencv_highgui.ButtonCallback testButton = new opencv_highgui.ButtonCallback();

//        opencv_highgui.ButtonCallback buttonCallback = new opencv_highgui.ButtonCallback(windowPointer);
//        opencv_highgui.createButton(windowPointer, buttonCallback);


        Frame frame;
        while ((frame = grabber.grabImage()) != null) {

            // TODO   this needs to be done using opencv.high_gui   instead

//            Mat frameImg = frameConverter.convertToMat(frame);
            Mat frameImg = new Mat(frameConverter.convertToMat(frame), cropRect);   // crop the frame

            // clone this, so you can show the original scaled up image in the display window???
            resize(frameImg, frameImg, new Size(IMG_WIDTH, IMG_HEIGHT));

            detectedObjects = yoloModelContainer.detectImg(frameImg);    // TODO   pass the numbers of animals, and if the numbers don't match  (or didn't match in the previous frame?), try with lower confidence?

            boundingBoxes = detectionsParser.parseDetections(detectedObjects);

            updateObjectTracking(boundingBoxes, frameImg, grabber.getFrameNumber(), grabber.getTimestamp());

            // TODO: 8/13/18 change everything to opencv_highui
            opencv_highgui.imshow(CANVAS_NAME, frameImg);
//            opencv_highgui.resizeWindow(CANVAS_NAME,WINDOW_WIDTH, WINDOW_HEIGHT);

            int key = waitKey(msDelay);

            if (key == 27) { // Escape key to exit      todo check char number for 'q' and other letters
                break;
            } else if (key == (int) 'q') {
                break;
            }

            /*
            canvasFrame.showImage(frameConverter.convert(frameImg));
            */
                        /*logSimpleMessage(

            String.format("%n---------------Time Profiles (s)-------------" +
                            "%nFrame to Mat Conversion:\t%.7f %nResize Mat Object:\t\t\t%.7f %nYolo Detection:\t\t\t\t%.7f" +
                            "%nParse Detections:\t\t\t%.7f %nUpdate Obj Tracking:\t\t%.7f %nDraw Graphics:\t\t\t\t%.7f%n" +
                            "----------------------------------------------%n",
                    (time2 - time1) / 1.0e9, (time3 - time2) / 1.0e9, (time4 - time3) / 1.0e9,
                    (time5 - time4) / 1.0e9, (time6 - time5) / 1.0e9, (time7 - time6) / 1.0e9
            )
            );*/

//            Thread.sleep(10L);
        }
        grabber.release();
        destroyAllWindows();
    }


    private void updateObjectTracking(List<BoundingBox> boundingBoxes, Mat frameImage, int frameNumber, long timePos) {

        double min_prox, prox;

        // the length of the diagonal across the frame--> the largest possible displacement distance for an object in the image
        int prox_start_val = (int) Math.round(Math.sqrt(Math.pow(frameImage.rows(), 2) + Math.pow(frameImage.cols(), 2)));

        double displThresh = (frameNumber < 10) ? prox_start_val : DISPL_THRESH;   // start out with large proximity threshold to quickly snap to objects

        ArrayList<BoundingBox> assignedBoxes = new ArrayList<>(boundingBoxes.size());
        BoundingBox closestBox;


        // TODO: 8/13/18 opencv_highgui.startWindowThread() ???    what is this used for

        for (Animal animal : animals) {

            min_prox = displThresh;     // start at max allowed value and then favor smaller values
            closestBox = null;

            for (BoundingBox box : boundingBoxes) {

                if (!assignedBoxes.contains(box)) {  // skip already assigned boxes
                    // circleRadius = Math.round(box[2] + box[3] / 2);  // approximate circle from rectangle dimensions

                    prox = Math.pow(Math.abs(animal.x - box.centerX) ^ 2 + Math.abs(animal.y - box.centerY) ^ 2, 0.5);

                    if (prox < min_prox) {
                        min_prox = prox;
                        closestBox = box;
                    }
                }

                if (DRAW_RECTANGLES) {
                    // this rectangle drawing will be removed later  (?)
                    rectangle(frameImage, new Point(box.topleftX, box.topleftY),
                            new Point(box.botRightX, box.botRightY), Scalar.RED, 1, CV_AA, 0);
                }
            }
            if (boundingBoxes.size() == animals.size() && closestBox != null) {   // This means min_prox < displacement_thresh?
                // todo: instead of min_prox --> use (Decision tree? / Markov? / SVM? / ???) to determine if the next point is reasonable
                animal.updateLocation(closestBox.centerX, closestBox.centerY, timePos);
                assignedBoxes.add(closestBox);

            } else if (closestBox != null) {
                System.out.println("First if-statement");

                animal.updateLocation(closestBox.centerX, closestBox.centerY, timePos);
                assignedBoxes.add(closestBox);

            } else {
                System.out.println("Predicting trajectory goes here?");
                animal.updateLocation(animal.x, animal.y, timePos);
            }

            if (DRAW_SHAPES) {
                drawShapesOnImageFrame(frameImage, animal);             // call this here so that this.animals doesn't have to be iterated through again
            }
        }
    }


    private void drawShapesOnImageFrame(Mat videoFrameMat, Animal animal) {

        // TODO: 8/12/18    need to rework this to use    org.bytedeco.javacpp.opencv_highgui

        // info : http://bytedeco.org/javacpp-presets/opencv/apidocs/org/bytedeco/javacpp/opencv_imgproc.html#method.detail

        circle(videoFrameMat, new Point(animal.x, animal.y), animal.CIRCLE_RADIUS, new Scalar(0, 255, 0, 1));

        int thickness;

        Iterator<int[]> linePointsIterator = animal.getLinePointsIterator();

        if (!linePointsIterator.hasNext()) {
            logger.warn("Line points iterator is empty");
            return;
        }


        // draw trailing trajectory line behind current animal
        int[] pt1 = linePointsIterator.next();
        int[] pt2;

        while (linePointsIterator.hasNext()) {

            pt2 = linePointsIterator.next();

            thickness = (int) animal.LINE_THICKNESS;

            line(videoFrameMat,
                    new Point(pt1[0], pt1[1]),
                    new Point(pt2[0], pt2[1]),
                    animal.color, thickness, LINE_AA, 0); // thickness, line type, shift  -->   //line type is LINE_4, LINE_8, or LINE_AA

            pt1 = pt2;
        }


        // TODO   check if iterator is faster (for such a small array) than converting to int[][] array  and iterating through



        if (1==1) return;





        int[][] linePointsArr = animal.getLinePointsAsArray();

        for (int i = 1; i < linePointsArr.length; i++) {

            if (linePointsArr[i - 1] == null || linePointsArr[i] == null) {
                break;
            }   // check for null values

            // todo just use one thickness value??
//            thickness = (int) Math.round(Math.sqrt(animal.LINE_THICKNESS / (linePointsArr.length - i)) * 2);  // todo what does this do?
            thickness = (int) animal.LINE_THICKNESS;

            line(videoFrameMat,
                    new Point(linePointsArr[i - 1][0], linePointsArr[i - 1][1]),
                    new Point(linePointsArr[i][0], linePointsArr[i][1]),
                    animal.color, thickness, LINE_AA, 0); // thickness, line type, shift  -->   //line type is LINE_4, LINE_8, or LINE_AA
        }



    }
}
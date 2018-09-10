package sproj.tracking;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.*;
import sproj.util.DetectionsParser;
import sproj.util.Logger;
import sproj.yolo_porting_attempts.YOLOModelContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static org.bytedeco.javacpp.opencv_imgproc.line;
import static org.opencv.imgproc.Imgproc.LINE_AA;

public abstract class Tracker {          //  TODO make this an interface?

    static final Logger logger = new Logger();   // LogManager.getLogger("SinglePlateTracker");

    final double DISPL_THRESH_FRACT = 1.5;      // used for distance thresholding
    final int DISPL_THRESH = 15;
    final int ARRAY_MAX_SIZE = 60;              // buffer size of array to accumulate data
    final int frame_resize_width = 720;
    boolean DRAW_SHAPES = true;
    boolean DRAW_RECTANGLES = false;
    int circleRadius = 5;

    String CANVAS_NAME;

    final int IMG_WIDTH = YOLOModelContainer.IMG_WIDTH;
    final int IMG_HEIGHT = YOLOModelContainer.IMG_HEIGHT;
    int INPUT_FRAME_WIDTH;
    int INPUT_FRAME_HEIGHT;

    int WINDOW_WIDTH = 720;     // ask user for size
    int WINDOW_HEIGHT = 720;     // ask user for size

//    FFmpegFrameGrabber grabber;


//    protected YOLOModelContainer yoloModelContainer = new YOLOModelContainer();
//    protected ArrayList<Animal> animals = new ArrayList<>();
//
//    private DetectionsParser detectionsParser = new DetectionsParser();
//    private OpenCVFrameConverter frameConverter = new OpenCVFrameConverter.ToMat();

    int numb_of_anmls;     // TODO remove this, since it would number of 'dishes' in MultiPlate??


//    abstract void initializeFrameGrabber(String videoPath) throws FrameGrabber.Exception;


    public abstract Frame timeStep() throws IOException;


    abstract void createAnimalObjects();

    abstract void initializeFrameGrabber(String videoPath) throws FrameGrabber.Exception;

    public abstract void tearDown();


    /**
     * Note that these drawing functions change the Mat object by changing color values to draw the shapes.
     * @param videoFrameMat Mat object
     * @param animal Animal object
     */
    void traceAnimalOnFrame(opencv_core.Mat videoFrameMat, Animal animal) {
        // info : http://bytedeco.org/javacpp-presets/opencv/apidocs/org/bytedeco/javacpp/opencv_imgproc.html#method.detail

        opencv_core.Scalar circleColor = animal.color; //new Scalar(0,255,0,1);
        circle(videoFrameMat, new opencv_core.Point(animal.x, animal.y), animal.CIRCLE_RADIUS, circleColor);

        // draw trailing trajectory line behind current animal
        int lineThickness = animal.LINE_THICKNESS;
        Iterator<int[]> linePointsIterator = animal.getLinePointsIterator();

        if (linePointsIterator.hasNext()) {

            int[] pt1 = linePointsIterator.next();
            int[] pt2;

            while (linePointsIterator.hasNext()) {

                pt2 = linePointsIterator.next();
                // lineThickness = Math.round(Math.sqrt(animal.LINE_THICKNESS / (animal.linePointsSize - i)) * 2);

                line(videoFrameMat,
                        new opencv_core.Point(pt1[0], pt1[1]),
                        new opencv_core.Point(pt2[0], pt2[1]),
                        animal.color, lineThickness, LINE_AA, 0); // lineThickness, line type, shift
                pt1 = pt2;                                           // -->  line type is LINE_4, LINE_8, or LINE_AA
            }
        } else {
            logger.warn("Line points iterator is empty, failed to draw trajectory paths.");
        }
    }
}

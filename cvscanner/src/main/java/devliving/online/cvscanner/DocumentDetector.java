package devliving.online.cvscanner;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.util.List;

import devliving.online.cvscanner.util.CVProcessor;

/**
 * Created by user on 10/15/16.
 */
public class DocumentDetector extends Detector<Document> {

    Context mContext;

    public DocumentDetector(Context context){
        super();
        mContext = context;
    }

    @Override
    public SparseArray<Document> detect(Frame frame) {
        SparseArray<Document> detections = new SparseArray<>();
        if(frame.getBitmap() != null) {
            Document doc = detectDocument(frame);

            if (doc != null) detections.append(frame.getMetadata().getId(), doc);
        }

        return detections;
    }

    Document detectDocument(Frame frame){
        Size imageSize = new Size(frame.getMetadata().getWidth(), frame.getMetadata().getHeight());
        Mat src = new Mat();
        Utils.bitmapToMat(frame.getBitmap(), src);
        List<MatOfPoint> contours = CVProcessor.findContours(src);
        src.release();

        if(!contours.isEmpty()){
            CVProcessor.Quadrilateral quad = CVProcessor.getQuadrilateral(contours, imageSize);

            if(quad != null){
                quad.points = CVProcessor.getUpscaledPoints(quad.points, CVProcessor.getScaleRatio(imageSize));

                Point tl = quad.points[0];
                Point tr = quad.points[1];
                Point br = quad.points[2];
                Point bl = quad.points[3];

                double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
                double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));
                double dw = Math.max(widthA, widthB);

                double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
                double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));
                double dh = Math.max(heightA, heightB);

                double ratio = dh < dw ? dh / dw : dw / dh;
                Log.i("RATIO", Double.toString(ratio));

                //double test = 0.582; // Card
                //if (ratio >= test * 0.9 && ratio <= test * 1.1)
                    return new Document(frame, quad);
            }
        }

        return null;
    }
}

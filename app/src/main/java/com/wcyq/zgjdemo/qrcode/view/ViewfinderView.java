package com.wcyq.zgjdemo.qrcode.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff.Mode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.wcyq.zgjdemo.qrcode.IMResUtil;
import com.wcyq.zgjdemo.qrcode.camera.CameraManager;

import java.util.Collection;
import java.util.HashSet;

public final class ViewfinderView extends View {
    private static final long ANIMATION_DELAY = 10;
    private static final int CORNER_RECT_HEIGHT = 40;
    private static final int CORNER_RECT_WIDTH = 8;
    private static final int OPAQUE = 255;
    private static final int[] SCANNER_ALPHA = new int[]{0, 64, 128, 192, OPAQUE, 192, 128, 64};
    private static final int SCANNER_LINE_HEIGHT = 10;
    private static final int SCANNER_LINE_MOVE_DISTANCE = 5;
    public static int scannerEnd = 0;
    public static int scannerStart = 0;
    private       CameraManager           cameraManager;
    private final int                     cornerColor;
    private final int                     frameColor;
    private       String                  labelText;
    private       int                     labelTextColor;
    private       float                   labelTextSize;
    private final int                     laserColor;
    private       Collection<ResultPoint> lastPossibleResultPoints;
    private final IMResUtil               mImResUtil;
    private final int                     maskColor;
    private final Paint paint = new Paint();
    private Collection<ResultPoint> possibleResultPoints;
    private Bitmap resultBitmap;
    private final int resultColor;
    private final int resultPointColor;
    private int scannerAlpha;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mImResUtil = new IMResUtil(context);
        TypedArray array = context.obtainStyledAttributes(attrs, this.mImResUtil.getStyleableArray("ViewfinderView"));
        this.laserColor = array.getColor(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_laser_color"), 65280);
        this.cornerColor = array.getColor(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_corner_color"), 65280);
        this.frameColor = array.getColor(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_frame_color"), 16777215);
        this.resultPointColor = array.getColor(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_result_point_color"), -1056964864);
        this.maskColor = array.getColor(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_mask_color"), 1610612736);
        this.resultColor = array.getColor(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_result_color"), -1342177280);
        this.labelTextColor = array.getColor(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_label_text_color"), -1862270977);
        this.labelText = array.getString(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_label_text"));
        this.labelTextSize = (float) array.getDimensionPixelSize(this.mImResUtil.getStyleable("ViewfinderView_device_qrcode_label_text_size"), (int) TypedValue.applyDimension(2, 16.0f, getResources().getDisplayMetrics()));
        this.paint.setAntiAlias(true);
        this.scannerAlpha = 0;
        this.possibleResultPoints = new HashSet(5);
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    public void onDraw(Canvas canvas) {
        Rect frame = this.cameraManager.getFramingRect();
        if (frame != null) {
            if (scannerStart == 0 || scannerEnd == 0) {
                scannerStart = frame.top;
                scannerEnd = frame.bottom;
            }
            drawExterior(canvas, frame, canvas.getWidth(), canvas.getHeight());
            if (this.resultBitmap != null) {
                this.paint.setAlpha(OPAQUE);
                canvas.drawBitmap(this.resultBitmap, (float) frame.left, (float) frame.top, this.paint);
                return;
            }
            drawFrame(canvas, frame);
            drawCorner(canvas, frame);
            drawLaserScanner(canvas, frame);
            drawTextInfo(canvas, frame);
            Collection<ResultPoint> currentPossible = this.possibleResultPoints;
            Collection<ResultPoint> currentLast = this.lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                this.lastPossibleResultPoints = null;
            } else {
                this.possibleResultPoints = new HashSet(5);
                this.lastPossibleResultPoints = currentPossible;
                this.paint.setAlpha(OPAQUE);
                this.paint.setColor(this.resultPointColor);
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(((float) frame.left) + point.getX(), ((float) frame.top) + point.getY(), 6.0f, this.paint);
                }
            }
            if (currentLast != null) {
                this.paint.setAlpha(127);
                this.paint.setColor(this.resultPointColor);
                for (ResultPoint point2 : currentLast) {
                    canvas.drawCircle(((float) frame.left) + point2.getX(), ((float) frame.top) + point2.getY(), 3.0f, this.paint);
                }
            }
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    private void drawTextInfo(Canvas canvas, Rect frame) {
        this.paint.setColor(this.labelTextColor);
        this.paint.setTextSize(TypedValue.applyDimension(2, this.labelTextSize, getResources().getDisplayMetrics()));
        this.paint.setTextAlign(Align.CENTER);
        canvas.drawText(this.labelText, (float) (frame.left + (frame.width() / 2)), (float) (frame.bottom + 80), this.paint);//去掉底部文字
    }

    private void drawCorner(Canvas canvas, Rect frame) {
        this.paint.setColor(this.cornerColor);
        canvas.drawRect((float) frame.left, (float) frame.top, (float) (frame.left + CORNER_RECT_WIDTH), (float) (frame.top + CORNER_RECT_HEIGHT), this.paint);
        canvas.drawRect((float) frame.left, (float) frame.top, (float) (frame.left + CORNER_RECT_HEIGHT), (float) (frame.top + CORNER_RECT_WIDTH), this.paint);
        canvas.drawRect((float) (frame.right - 8), (float) frame.top, (float) frame.right, (float) (frame.top + CORNER_RECT_HEIGHT), this.paint);
        canvas.drawRect((float) (frame.right - 40), (float) frame.top, (float) frame.right, (float) (frame.top + CORNER_RECT_WIDTH), this.paint);
        canvas.drawRect((float) frame.left, (float) (frame.bottom - 8), (float) (frame.left + CORNER_RECT_HEIGHT), (float) frame.bottom, this.paint);
        canvas.drawRect((float) frame.left, (float) (frame.bottom - 40), (float) (frame.left + CORNER_RECT_WIDTH), (float) frame.bottom, this.paint);
        canvas.drawRect((float) (frame.right - 8), (float) (frame.bottom - 40), (float) frame.right, (float) frame.bottom, this.paint);
        canvas.drawRect((float) (frame.right - 40), (float) (frame.bottom - 8), (float) frame.right, (float) frame.bottom, this.paint);
    }

    private void drawLaserScanner(Canvas canvas, Rect frame) {
        this.paint.setColor(this.laserColor);
        LinearGradient linearGradient = new LinearGradient((float) frame.left, (float) scannerStart, (float) frame.left, (float) (scannerStart + 10), shadeColor(this.laserColor), this.laserColor, TileMode.MIRROR);
        RadialGradient radialGradient = new RadialGradient((float) (frame.left + (frame.width() / 2)), (float) (scannerStart + 5), 360.0f, this.laserColor, shadeColor(this.laserColor), TileMode.MIRROR);
        SweepGradient sweepGradient = new SweepGradient((float) (frame.left + (frame.width() / 2)), (float) (scannerStart + 10), shadeColor(this.laserColor), this.laserColor);
        ComposeShader composeShader = new ComposeShader(radialGradient, linearGradient, Mode.ADD);
        this.paint.setShader(radialGradient);
        if (scannerStart <= scannerEnd) {
            canvas.drawOval(new RectF((float) (frame.left + 20), (float) scannerStart, (float) (frame.right - 20), (float) (scannerStart + 10)), this.paint);
            scannerStart += 5;
        } else {
            scannerStart = frame.top;
        }
        this.paint.setShader(null);
    }

    public int shadeColor(int color) {
        return Integer.valueOf("20" + Integer.toHexString(color).substring(2), 16).intValue();
    }

    private void drawFrame(Canvas canvas, Rect frame) {
        this.paint.setColor(this.frameColor);
        canvas.drawRect((float) frame.left, (float) frame.top, (float) (frame.right + 1), (float) (frame.top + 2), this.paint);
        canvas.drawRect((float) frame.left, (float) (frame.top + 2), (float) (frame.left + 2), (float) (frame.bottom - 1), this.paint);
        canvas.drawRect((float) (frame.right - 1), (float) frame.top, (float) (frame.right + 1), (float) (frame.bottom - 1), this.paint);
        canvas.drawRect((float) frame.left, (float) (frame.bottom - 1), (float) (frame.right + 1), (float) (frame.bottom + 1), this.paint);
    }

    private void drawExterior(Canvas canvas, Rect frame, int width, int height) {
        this.paint.setColor(this.resultBitmap != null ? this.resultColor : this.maskColor);
        canvas.drawRect(0.0f, 0.0f, (float) width, (float) frame.top, this.paint);
        canvas.drawRect(0.0f, (float) frame.top, (float) frame.left, (float) (frame.bottom + 1), this.paint);
        canvas.drawRect((float) (frame.right + 1), (float) frame.top, (float) width, (float) (frame.bottom + 1), this.paint);
        canvas.drawRect(0.0f, (float) (frame.bottom + 1), (float) width, (float) height, this.paint);
    }

    public void drawViewfinder() {
        this.resultBitmap = null;
        invalidate();
    }

    public void drawResultBitmap(Bitmap barcode) {
        this.resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        this.possibleResultPoints.add(point);
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public void setLabelTextColor(int labelTextColor) {
        this.labelTextColor = labelTextColor;
    }

    public void setLabelTextSize(float labelTextSize) {
        this.labelTextSize = labelTextSize;
    }
}
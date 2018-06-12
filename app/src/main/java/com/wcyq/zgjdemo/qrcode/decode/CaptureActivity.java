package com.wcyq.zgjdemo.qrcode.decode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.wcyq.zgjdemo.qrcode.FileUtil;
import com.wcyq.zgjdemo.qrcode.IMResUtil;
import com.wcyq.zgjdemo.qrcode.camera.CameraManager;
import com.wcyq.zgjdemo.qrcode.view.ViewfinderView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;


public class CaptureActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    private TextView  mBackm;
    private ImageView mBackmImg;
    private TextView  mTitle;
    private TextView  mOpenImag;
    public int REQ_ID_GALLERY = 0;
    private ImageView      mLightImage;
    private RelativeLayout mLightLinearLay;//以前是线性布局
    public static boolean isLightOn      = false;
    public static boolean isHasLightImag = false;
    private TextView  mTvLight;
    private IMResUtil mImResUtil;

    public static void startAction(Activity activity, Bundle bundle, int requestCode) {
        Intent intent = new Intent(activity, CaptureActivity.class);
        intent.putExtras(bundle);
        activity.startActivityForResult(intent, requestCode);
    }

    private CameraManager             cameraManager;
    private CaptureActivityHandler    handler;
    private ViewfinderView            viewfinderView;
    private boolean                   hasSurface;
    private Collection<BarcodeFormat> decodeFormats;
    private String                    characterSet;
    private InactivityTimer           inactivityTimer;
    private BeepManager               beepManager;
    private AmbientLightManager       ambientLightManager;


    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImResUtil = new IMResUtil(this);
        setContentView(mImResUtil.getLayout("activity_device_qrcode_capture"));
        //设置浸入式状态栏
        setColor(this, Color.BLACK);
        //        savedInstanceState.getBundle()
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        ambientLightManager = new AmbientLightManager(this);

        initView(getIntent().getExtras());
    }

    private void initView(Bundle bundle) {
        //判断是否为横屏状态
        if ("landscape".equals(bundle.getString("portraitOrLandscape"))) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        mBackmImg = (ImageView) findViewById(mImResUtil.getId("iv_qr_back"));
        try {
            InputStream backImg = getAssets().open("images/device_qrcode_scan_back.png");
            if (backImg != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(backImg);
                mBackmImg.setImageBitmap(bitmap);
                backImg.close();
            } else {
                mBackmImg.setVisibility(View.GONE);
            }
        } catch (IOException e) {
            mBackmImg.setVisibility(View.GONE);
            e.printStackTrace();
        }

        mLightLinearLay = (RelativeLayout) findViewById(mImResUtil.getId("btn_scan_light"));
        mLightImage = (ImageView) findViewById(mImResUtil.getId("iv_scan_light"));
        mTvLight = (TextView) findViewById(mImResUtil.getId("tv_scan_light"));

        dealImageLightView("images/device_qrcode_scan_flash_off.png");
        mBackm = (TextView) findViewById(mImResUtil.getId("tv_qr_back"));//mImResUtil.getId("tv_qr_back")
        mTitle = (TextView) findViewById(mImResUtil.getId("tv_qr_title"));
        mOpenImag = (TextView) findViewById(mImResUtil.getId("tv_qr_open_image"));
        viewfinderView = (ViewfinderView) findViewById(mImResUtil.getId("vv_qr_viewfinderView"));

        mBackm.setOnClickListener(this);
        mOpenImag.setOnClickListener(this);

        if (bundle == null) {
            return;
        }
        String back = bundle.getString("backText");
        String titileText = bundle.getString("titileText");
        String imgText = bundle.getString("imgText");
                String labelText = bundle.getString("labelText");
        if (back != null && !back.isEmpty()) {
            mBackm.setText(back);
        }
        if (titileText != null && !titileText.isEmpty()) {
            mTitle.setText(titileText);
        }
        if (imgText != null && !imgText.isEmpty()) {
            mOpenImag.setText(imgText);
        }
                if (labelText != null && !labelText.isEmpty()) {//去掉底部文字
                    viewfinderView.setLabelText(labelText);
                }

        String headColor = bundle.getString("headColor");
        String labelColor = bundle.getString("labelColor");

        mBackm.setTextColor(Color.parseColor(headColor));
        mTitle.setTextColor(Color.parseColor(headColor));
        mOpenImag.setTextColor(Color.parseColor(headColor));
        viewfinderView.setLabelTextColor(Color.parseColor(labelColor));

        float headSize = bundle.getFloat("headSize");
        float labelSize = bundle.getFloat("labelSize");
        if (headSize > 0) {
            mBackm.setTextSize(headSize);
            mTitle.setTextSize(headSize);
            mOpenImag.setTextSize(headSize);
        }
        if (labelSize > 0) {
            viewfinderView.setLabelTextSize(labelSize);
        }
    }

    /**
     * 沉浸式状态栏
     *
     * @param activity
     * @param color
     */
    public static void setColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 设置状态栏透明
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 生成一个状态栏大小的矩形
            View statusView = createStatusView(activity, color);
            // 添加 statusView 到布局中
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            decorView.addView(statusView);
            // 设置根布局的参数
            ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            rootView.setFitsSystemWindows(true);
            rootView.setClipToPadding(true);
        }
    }

    /**
     * 绘制一个和状态栏登高的矩形
     *
     * @param activity
     * @param color
     */
    private static View createStatusView(Activity activity, int color) {
        // 获得状态栏高度
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = activity.getResources().getDimensionPixelSize(resourceId);

        View statusView = new View(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight);
        statusView.setLayoutParams(params);
        statusView.setBackgroundColor(color);
        return statusView;
    }


    @Override
    public void onClick(View v) {

        int id = v.getId();
        if (id == mImResUtil.getId("tv_qr_back")) {
            this.finish();
        } else if (id == mImResUtil.getId("tv_qr_open_image")) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQ_ID_GALLERY);
        } else if (id == mImResUtil.getId("btn_scan_light")) {
            if (isLightOn) {
                isLightOn = false;
                dealImageLightView("images/device_qrcode_scan_flash_off.png");
                mTvLight.setText("打开手电筒");
                cameraManager.offLight();
            } else {
                isLightOn = true;
                dealImageLightView("images/device_qrcode_scan_flash_on.png");
                mTvLight.setText("关闭手电筒");
                cameraManager.openLight();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ID_GALLERY) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    String path = FileUtil.checkPicturePath(CaptureActivity.this, uri);//Device.getActivity()
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    bmOptions.inJustDecodeBounds = false;
                    bmOptions.inPurgeable = true;

                    Bitmap bmp = BitmapFactory.decodeFile(path, bmOptions);
                    decodeQRCode(bmp, this);
                }
            }
        }
    }

    /**
     * 解析二维码图片
     *
     * @param bitmap   要解析的二维码图片
     */
    public final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);

    @SuppressLint("StaticFieldLeak")
    public void decodeQRCode(final Bitmap bitmap, final Activity activity) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int[] pixels = new int[width * height];
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                    RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                    Result result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), HINTS);
                    Intent resultIntent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result.getText());
                    resultIntent.putExtras(bundle);
                    activity.setResult(RESULT_OK, resultIntent);
                    CaptureActivity.this.finish();
                    return result.getText();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Log.d("CaptureActivity", "result=" + result);
                Intent resultIntent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("result", "未能识别二维码");
                resultIntent.putExtras(bundle);
                activity.setResult(RESULT_OK, resultIntent);
                CaptureActivity.this.finish();
                //                Toast.makeText(CaptureActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }.execute();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());
        viewfinderView.setCameraManager(cameraManager);
        handler = null;

        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();


        decodeFormats = null;
        characterSet = null;
        SurfaceView surfaceView = (SurfaceView) findViewById(mImResUtil.getId("device_qrcode_preview_view"));
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(mImResUtil.getId("device_qrcode_preview_view"));
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, characterSet, cameraManager, mLightLinearLay);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        Log.d("wxl", "rawResult=" + rawResult);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            String resultString = rawResult.getText();
            // Then not from history, so beep/vibrate and we have an image to draw on
            beepManager.playBeepSoundAndVibrate();
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("result", resultString);
            resultIntent.putExtras(bundle);
            this.setResult(RESULT_OK, resultIntent);
        } else {
            //            this.setResult(RESULT_OK, resultIntent);
            //            Toast.makeText(CaptureActivity.this, "Scan failed!", Toast.LENGTH_SHORT).show();
        }
        CaptureActivity.this.finish();
    }

    public void dealImageLightView(String filePath) {
        try {
            InputStream lightImag = getAssets().open(filePath);
            if (lightImag != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(lightImag);
                isHasLightImag = true;
                mLightImage.setImageBitmap(bitmap);
                mLightLinearLay.setClickable(true);
                mLightLinearLay.setOnClickListener(this);
                lightImag.close();
            } else {
                isHasLightImag = false;
                mLightLinearLay.setVisibility(View.GONE);
                mLightLinearLay.setClickable(false);
            }
        } catch (IOException e) {
            isHasLightImag = false;
            mLightLinearLay.setVisibility(View.GONE);
            mLightLinearLay.setClickable(false);
            e.printStackTrace();
        }

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
}

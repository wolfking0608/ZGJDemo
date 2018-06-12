/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wcyq.zgjdemo.qrcode.decode;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.wcyq.zgjdemo.qrcode.IMResUtil;
import com.wcyq.zgjdemo.qrcode.camera.CameraManager;

import java.util.Collection;


/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureActivityHandler extends Handler {

    private static final String TAG = CaptureActivityHandler.class.getSimpleName();

    private final CaptureActivity activity;
    private final DecodeThread    decodeThread;
    private       State           state;
    private final CameraManager   cameraManager;
    private final IMResUtil       mImResUtil;
    private       RelativeLayout  mLightLinearLay;//LinearLayout

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    public CaptureActivityHandler(CaptureActivity activity,
                                  Collection<BarcodeFormat> decodeFormats,
                                  String characterSet,
                                  CameraManager cameraManager, RelativeLayout lightLinearLay) {
        this.activity = activity;
        mImResUtil = new IMResUtil(activity);
        mLightLinearLay = lightLinearLay;
        decodeThread = new DecodeThread(activity, decodeFormats, characterSet,
                new ViewfinderResultPointCallback(activity.getViewfinderView()));
        decodeThread.start();
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        Log.d(TAG, "CaptureActivityHandler " + CaptureActivityHandler.class.toString());
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {

        if((DecodeHandler.isWeakLight || CaptureActivity.isLightOn) && CaptureActivity.isHasLightImag){
            mLightLinearLay.setVisibility(View.VISIBLE);
        }else{
            mLightLinearLay.setVisibility(View.INVISIBLE);
        }

        int what = message.what;
        if (what ==  mImResUtil.getId("device_qrcode_restart_preview")) {
            restartPreviewAndDecode();

        } else if (what == mImResUtil.getId("device_qrcode_decode_succeeded")) {
            state = State.SUCCESS;
            Bundle bundle = message.getData();
            Bitmap barcode = null;
            float scaleFactor = 1.0f;
            if (bundle != null) {
                byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
                if (compressedBitmap != null) {
                    barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                    // Mutable copy:
                    barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                }
                scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
            }
            activity.handleDecode((Result) message.obj, barcode, scaleFactor);

        } else if (what == mImResUtil.getId("device_qrcode_decode_failed")) {
            state = State.PREVIEW;

            cameraManager.requestPreviewFrame(decodeThread.getHandler(), mImResUtil.getId("device_qrcode_decode"));

        } else if (what == mImResUtil.getId("device_qrcode_return_scan_result")) {
            activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
            activity.finish();

        } else if (what == mImResUtil.getId("device_qrcode_launch_product_query")) {
            String url = (String) message.obj;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.setData(Uri.parse(url));

            ResolveInfo resolveInfo =
                    activity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            String browserPackageName = null;
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                browserPackageName = resolveInfo.activityInfo.packageName;
                Log.d(TAG, "Using browser in package " + browserPackageName);
            }

            // Needed for default Android browser / Chrome only apparently
            if ("com.android.browser".equals(browserPackageName) || "com.android.chrome".equals(browserPackageName)) {
                intent.setPackage(browserPackageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackageName);
            }

            try {
                activity.startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
                Log.w(TAG, "Can't find anything to handle VIEW of URI " + url);
            }
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();

        Message quit = Message.obtain(decodeThread.getHandler(), mImResUtil.getId("device_qrcode_quit"));
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(mImResUtil.getId("device_qrcode_decode_succeeded"));
        removeMessages(mImResUtil.getId("device_qrcode_decode_failed"));
    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            //decodeThread.getHandler()拿到DecodeHandler
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), mImResUtil.getId("device_qrcode_decode"));
            activity.drawViewfinder();
        }
    }

}

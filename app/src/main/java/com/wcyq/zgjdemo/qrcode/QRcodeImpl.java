package com.wcyq.zgjdemo.qrcode;

/**
 * Author: GuangYang
 * Date: 2017/12/21 20:38
 * Description：  注释掉了
 */
public class QRcodeImpl {
    //顶部文字
    public String backText;
    public String titileText;
    public String imgText;
    public String headColor;
    public float headSize;
    //扫描框下部文字
    public String labelText;
    public String labelColor;
    public String portraitOrLandscape;
    public float labelSize;

    private static final int REQUEST_CODE = 1001;

//    public static native void nativeEvent(String qrcodeInfo);
//
//    //    int backColor,int titleColor,int imgColor,int labelColor,
////    float headSize,float labelSize, String backText,
////    String titileText,String imgText,String labelText
//    public void startScanQRcode() {
//        Bundle bundle = new Bundle();
//        bundle.putString("headColor", headColor);
//        bundle.putString("labelColor", labelColor);
//        bundle.putFloat("headSize", headSize);
//        bundle.putFloat("labelSize", labelSize);
//        bundle.putString("backText", backText);
//        bundle.putString("titileText", titileText);
//        bundle.putString("imgText", imgText);
//        bundle.putString("labelText", labelText);
//        bundle.putString("portraitOrLandscape", portraitOrLandscape);
//        CaptureActivity.startAction(1, bundle, REQUEST_CODE);//Device.getActivity()
//    }
//
//    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_CODE) {
//            if (resultCode == RESULT_OK) { //RESULT_OK = -1
//                Bundle bundle = data.getExtras();
//                final String scanResult = bundle.getString("result");
//                Device.getInstance().run_in_main_thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.e("thread", String.format("tid=%d", Thread.currentThread().getId()));
//                        nativeEvent(scanResult);
//                    }
//                });
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public static String generateQRcode(int dimension, int format, int quality, String path, String encodeInfo) {
//        if(dimension < 0){
//            dimension = 0 ;
//        }
//        if(format != Device.IMAGE_PNG && format != Device.IMAGE_JPG){
//            format = Device.IMAGE_JPG;
//        }
//        if(quality < 0 ){
//            quality = 0;
//        }
//        if(quality > 100){
//            quality = 100;
//        }
//        try {
//            Bitmap mBitmap = QRCodeEncoder.encodeAsBitmap(encodeInfo, dimension);
//            String fileName = FileUtil.bmp2File(mBitmap, path, format == Device.IMAGE_PNG ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, quality);
//            return fileName;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return "";
//    }

}

package com.wcyq.zgjdemo.qrcode;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileUtil {

    private static String TAG = "FileUtil";

    public static String randomFileName(String ext) {
        return Long.toString(System.currentTimeMillis()) + ext;
    }

    public static boolean deleteFile(String name) {
        File file = new File(name);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static String rotateAndSaveBitmap(File file, int outW, int outH, String outPath, Bitmap.CompressFormat format, int quality) {

        int originW;
        int originH;
        int orientation = ExifInterface.ORIENTATION_NORMAL;

        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
            int h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);

            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    return null;
                case ExifInterface.ORIENTATION_ROTATE_90:
                case ExifInterface.ORIENTATION_ROTATE_270:
                    originW = h;
                    originH = w;
                    break;
                default:
                    originW = w;
                    originH = h;
                    break;
            }

        } catch (Exception e) {
            return null;
        }

        // boost decode bitmap performance
        int sampleSize;
        if (outW <= 0 && outH <= 0) {
            sampleSize = 1;
            outW = originW;
            outH = originH;
        } else if (outW <= 0 || outH <= 0) {
            if (outW <= 0) {
                sampleSize = originH / outH;
                outW = (int) (originW * (outH / (float) originH));
            } else {
                sampleSize = originW / outW;
                outH = (int) (originH * (outW / (float) originW));
            }
        } else {
            sampleSize = Math.min(originW / outW, originH / outH);
        }

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = sampleSize;
        bmOptions.inPurgeable = true;

        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);

        float scaleX = outW;
        float scaleY = outH;

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                scaleX /= bmp.getWidth();
                scaleY /= bmp.getHeight();
                matrix.postScale(scaleX, scaleY);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                scaleX /= bmp.getWidth();
                scaleY /= bmp.getHeight();
                matrix.setScale(-1, 1);
                matrix.postScale(scaleX, scaleY);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                scaleX /= bmp.getWidth();
                scaleY /= bmp.getHeight();
                matrix.setRotate(180);
                matrix.postScale(scaleX, scaleY);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                scaleX /= bmp.getWidth();
                scaleY /= bmp.getHeight();
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                matrix.postScale(scaleX, scaleY);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                scaleX /= bmp.getWidth();
                scaleY /= bmp.getHeight();
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                matrix.postScale(scaleX, scaleY);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                scaleX /= bmp.getHeight();
                scaleY /= bmp.getWidth();
                matrix.setRotate(90);
                matrix.postScale(scaleX, scaleY);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                scaleX /= bmp.getWidth();
                scaleY /= bmp.getHeight();
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                matrix.postScale(scaleX, scaleY);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                scaleX /= bmp.getHeight();
                scaleY /= bmp.getWidth();
                matrix.setRotate(-90);
                matrix.postScale(scaleX, scaleY);
                break;
            default:
                break;
        }

        Bitmap bmpNew = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        String newFile = bmp2File(bmpNew, outPath, format, quality);
        bmpNew.recycle();
        return newFile;
    }

    private static Point getBitmapSize(String file) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, bmOptions);
        int originW = bmOptions.outWidth;
        int originH = bmOptions.outHeight;
        return new Point(originW, originH);
    }

    @SuppressWarnings("deprecation")
    public static String saveBitmap(File f, int outW, int outH, String outPath, Bitmap.CompressFormat format, int quality) {
        String rotateFile = rotateAndSaveBitmap(f, outW, outH, outPath, format, quality);
        if (null != rotateFile)
            return rotateFile;

        if (f == null) return "";

        String file = f.getAbsolutePath();
        // Get the dimensions of the bitmap
        Point size = getBitmapSize(file);
        int originW = size.x;
        int originH = size.y;

        // Determine how much to scale down the image
        int scaleFactor;
        if (outW <= 0 && outH <= 0) {
            scaleFactor = 1;
            outW = originW;
            outH = originH;
        } else if (outW <= 0 || outH <= 0) {
            if (outW <= 0) {
                scaleFactor = originH / outH;
                outW = (int) (originW * (outH / (float) originH));
            } else {
                scaleFactor = originW / outW;
                outH = (int) (originH * (outW / (float) originW));
            }
        } else {
            scaleFactor = Math.min(originW / outW, originH / outH);
        }

        // Decode the image file into a Bitmap sized to fit out size
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bmp = BitmapFactory.decodeFile(file, bmOptions);

        String newFile;
        if (bmp == null) {
            return "";
        }
        if (bmp.getWidth() != outW && bmp.getHeight() != outH) {
            Bitmap bmpNew = Bitmap.createScaledBitmap(bmp, outW, outH, true);
            newFile = bmp2File(bmpNew, outPath, format, quality);
            bmpNew.recycle();
        } else {
            newFile = bmp2File(bmp, outPath, format, quality);
        }
        bmp.recycle();
        return newFile;
    }

    public static Point calcScaleSize(String file, int outW, int outH) {

        int originW = 0;
        int originH = 0;

        try {
            ExifInterface exif = new ExifInterface(file);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
            int h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);

            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                case ExifInterface.ORIENTATION_ROTATE_270:
                    originW = h;
                    originH = w;
                    break;
                default:
                    originW = w;
                    originH = h;
                    break;
            }

        } catch (Exception e) {

        }

        if (0 == originW || 0 == originH) {
            Point size = getBitmapSize(file);
            originW = size.x;
            originH = size.y;
        }

        if (outW <= 0 && outH <= 0) {
            outW = originW;
            outH = originH;
        } else if (outW <= 0 || outH <= 0) {
            if (outW <= 0) {
                outW = (int) (originW * (outH / (float) originH));
            } else {
                outH = (int) (originH * (outW / (float) originW));
            }
        } else {
        }

        return new Point(outW, outH);
    }

    public static String bmp2File(Bitmap bmp, String filePath, Bitmap.CompressFormat format, int quality) {
        String fileName = FileUtil.randomFileName(Bitmap.CompressFormat.PNG == format ? ".png" : ".jpg");
        String fullPath = filePath + fileName;
        deleteFile(fileName);
        File file = new File(fullPath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        bmp.compress(format, quality, fOut);
        try {
            fOut.flush();
            fOut.close();
            fOut = null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (null != fOut) {
                    fOut.close();
                    fOut = null;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return fileName;
    }

    public static final String insertImage(ContentResolver cr, String imagePath, String name, String description) throws FileNotFoundException {
        FileInputStream inStream = new FileInputStream(imagePath);
        return insertImage(cr, inStream, name, description);
    }

    /**
     * A copy of the Android internals insertImage method, this method populates
     * the meta data with DATE_ADDED and DATE_TAKEN. This fixes a common problem
     * where media that is inserted manually gets saved at the end of the
     * gallery (because date is not populated).
     *
     * @see Images.Media#insertImage(ContentResolver,
     * Bitmap, String, String)
     */
    public static final String insertImage(ContentResolver cr, FileInputStream inStream, String title, String description) {

        ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, title);
        values.put(Images.Media.DISPLAY_NAME, title);
        values.put(Images.Media.DESCRIPTION, description);
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        // Add the date meta data to ensure the image is added at the front of
        // the gallery
        values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());

        Uri url = null;
        String stringUrl = null; /* value to be returned */

        try {
            url = cr.insert(Images.Media.EXTERNAL_CONTENT_URI, values);

            if (inStream != null) {
                OutputStream imageOut = cr.openOutputStream(url);
                byte[] buffer = new byte[1024];
                int count = 0;
                while ((count = inStream.read(buffer)) >= 0) {
                    imageOut.write(buffer, 0, count);
                }
                imageOut.flush();
                inStream.close();
                long id = ContentUris.parseId(url);
                // Wait until MINI_KIND thumbnail is generated.
                Bitmap miniThumb = Images.Thumbnails.getThumbnail(cr, id, Images.Thumbnails.MINI_KIND, null);
                // This is for backward compatibility.
                storeThumbnail(cr, miniThumb, id, 50F, 50F, Images.Thumbnails.MICRO_KIND);
            } else {
                cr.delete(url, null, null);
                url = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (url != null) {
                cr.delete(url, null, null);
                url = null;
            }
        }

        if (url != null) {
            stringUrl = url.toString();
        }

        return stringUrl;
    }

    /**
     * A copy of the Android internals StoreThumbnail method, it used with the
     * insertImage to populate the
     * android.provider.MediaStore.Images.Media#insertImage with all the correct
     * meta data. The StoreThumbnail method is private so it must be duplicated
     * here.
     *
     * @see Images.Media (StoreThumbnail private
     * method)
     */
    private static final Bitmap storeThumbnail(ContentResolver cr, Bitmap source, long id, float width, float height, int kind) {

        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

        ContentValues values = new ContentValues(4);
        values.put(Images.Thumbnails.KIND, kind);
        values.put(Images.Thumbnails.IMAGE_ID, (int) id);
        values.put(Images.Thumbnails.HEIGHT, thumb.getHeight());
        values.put(Images.Thumbnails.WIDTH, thumb.getWidth());

        Uri url = cr.insert(Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            thumbOut.close();
            return thumb;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    // /////////////////////////////////////////////////
    @SuppressLint("NewApi")
    public static String checkPicturePath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}

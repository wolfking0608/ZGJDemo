package com.wcyq.zgjdemo.qrcode;

import android.content.Context;
import android.util.Log;
import java.lang.reflect.Field;

public class IMResUtil {

    private static final String TAG = "IMResUtil";
    private static IMResUtil instance;
    private Context context;
    private static Class id = null;
    private static Class layout = null;
    private static Class style = null;
    private static Class attr = null;
    private static Class styleable = null;

    public IMResUtil(Context paramContext) {
        this.context = paramContext.getApplicationContext();

        try {
            layout = Class.forName(this.context.getPackageName() + ".R$layout");
        } catch (ClassNotFoundException localClassNotFoundException2) {
            Log.e(TAG,"localClassNotFoundException2 = " + localClassNotFoundException2.toString());
        }
        try {
            id = Class.forName(this.context.getPackageName() + ".R$id");
        } catch (ClassNotFoundException localClassNotFoundException3) {
            Log.e(TAG,"localClassNotFoundException3 = " + localClassNotFoundException3.toString());
        }

        try {
            style = Class.forName(this.context.getPackageName() + ".R$style");
        } catch (ClassNotFoundException localClassNotFoundException5) {
            Log.e(TAG,"localClassNotFoundException5 = " + localClassNotFoundException5.toString());
        }

        try {
            attr = Class.forName(this.context.getPackageName() + ".R$attr");
        } catch (ClassNotFoundException localClassNotFoundException10) {
            Log.e(TAG,"localClassNotFoundException10 = " + localClassNotFoundException10.toString());
        }

        try {
            styleable = Class.forName(this.context.getPackageName() + ".R$styleable");
        } catch (ClassNotFoundException localClassNotFoundException10) {
            Log.e(TAG,"localClassNotFoundException10 = " + localClassNotFoundException10.toString());
        }
    }

    public static IMResUtil getResofR(Context paramContext) {
        if (instance == null)
            instance = new IMResUtil(paramContext);
        return instance;
    }


    public int getId(String paramString) {
        return getResofR(id, paramString);
    }

    public  int getLayout(String paramString) {
        return getResofR(layout, paramString);
    }

    public int getStyle(String paramString) {
        return getResofR(style, paramString);
    }

    public  int getAttr(String paramString) {return getResofR(attr, paramString);}

    public  int getStyleable(String paramString) {return getResofR(styleable, paramString);}

    public int [] getStyleableArray(String paramString){
        try {
            Class clz = Class.forName(context.getPackageName()+".R$styleable");
            Field field = clz.getField(paramString);
            Object object = field.get(clz);
            int[] attr = (int[]) object;
            for (int i = 0; i < attr.length; i++) {
                Log.d(TAG, attr[i]+"");
            }
            return attr;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    private  int getResofR(Class<?> paramClass, String paramString) {
        if (paramClass == null) {
            Log.d(TAG,"getRes(null," + paramString + ")");
            Log.d(TAG,"Class is null ,ResClass is not initialized.");
            return -1;
//            throw new IllegalArgumentException("ResClass is not initialized.");
        }
        try {
            Field localField = paramClass.getField(paramString);
            int k = localField.getInt(paramString);
            return k;
        } catch (Exception localException) {
            Log.e(TAG,"getRes(" + paramClass.getName() + ", " + paramString + ")");
            Log.e(TAG,"Error getting resource. Make sure you have copied all resources (res/) from SDK to your project. ");
            Log.e(TAG,localException.getMessage());
        }
        return -1;
    }
}

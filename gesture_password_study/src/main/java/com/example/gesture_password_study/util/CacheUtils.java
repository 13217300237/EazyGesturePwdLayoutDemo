package com.example.gesture_password_study.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 文件管理类
 * Created by reeta.zou on 2017/5/25.
 */

public class CacheUtils {

    public static String get_CACHE_GESTURE_PWD_INT() {
        return "gesture_pwd_int.txt";//用于保存 手势密码串的文件
    }

    /**
     * 获取cache文件的位置
     */
    public static String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }


    /**
     * 清空指定的文件
     *
     * @param fileName
     */
    public void clearConetent(Context context, String fileName) {
        writeFile(context, fileName, "");
    }


    /**
     * 保存文件到应用缓存
     *
     * @param fileName
     * @param mContent
     */
    public static void writeFile(Context context, String fileName, String mContent) {
        File fileDir = new File(getDiskCachePath(context));
        File mFile = new File(getDiskCachePath(context) + "/" + fileName);
        if (TextUtils.isEmpty(mContent)) {
            if (mFile.exists()) {
                mFile.delete();
            }
            return;
        }
        FileOutputStream fos = null;
        try {
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
            if (!mFile.exists()) {
                mFile.createNewFile();
            }
            if (mContent.length() > 1) {
                fos = new FileOutputStream(mFile);
                fos.write(mContent.getBytes());
                fos.flush();// 刷新缓冲区
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取指定文件内容
     *
     * @param fileName
     * @return
     */
    public static String readFile(Context context, String fileName) {
        File mFile = new File(getDiskCachePath(context) + "/" + fileName);
        if (!mFile.exists()) {
            return "";
        }
        FileInputStream fis = null;
        InputStreamReader inputStreamReader = null;
        try {
            fis = new FileInputStream(mFile);
            inputStreamReader = new InputStreamReader(fis, "utf-8");
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuffer sb = new StringBuffer("");
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return "";
    }

}

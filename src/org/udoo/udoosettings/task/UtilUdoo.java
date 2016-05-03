package org.udoo.udoosettings.task;

import android.util.Log;

import org.udoo.udoosettings.interfaces.OnResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;

/**
 * Created by harlem88 on 16/03/16.
 */
public class UtilUdoo {

    private static final String TAG = "ChangeParameterTask";

    private static final String VIDEO_OUT = "video_output=";
    private static final String UENV_PATH = "/data/uEnv.txt";


    public static void WriteParameter(final String param, final OnResult<Boolean> onResult) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                String lineParam = "";
                try {
                    if (param != null && param.length() > 0) {

                        File file = new File(UENV_PATH);
                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        String line, oldtext = "";
                        while ((line = reader.readLine()) != null) {

                            if (line.startsWith(VIDEO_OUT))
                                lineParam = line;

                            oldtext += line + "\r\n";
                        }
                        reader.close();
                        String newtext = oldtext.replace(lineParam, VIDEO_OUT + param);

                        FileWriter writer = new FileWriter(UENV_PATH);
                        writer.write(newtext);
                        writer.close();
                        success = true;
                    }

                } catch (IOException e) {
                    Log.e(TAG, "" + e.getMessage());
                    if (onResult != null)
                        onResult.onError(e);
                }
                if (onResult != null)
                    onResult.onSuccess(success);

            }
        });
    }

    public static void ReadParameter(final OnResult<String> onResult) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                String value = "";
                try {
                    FileInputStream fileInputStream = new FileInputStream(UENV_PATH);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                    String line = bufferedReader.readLine();
                    boolean find = false;
                    while (line != null && !find) {
                        if (line.startsWith(VIDEO_OUT)) {
                            value = line.substring(VIDEO_OUT.length(), line.length());
                            find = true;
                        }
                        line = bufferedReader.readLine();
                    }
                    fileInputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "" + e.getMessage());
                    if (onResult != null)
                        onResult.onError(e);
                }
                if (onResult != null)
                    onResult.onSuccess(value);
            }
        });
    }

    public static boolean ExecuteCommandLine(String commandLine) {
        boolean retval = false;

        try {
            Process process = Runtime.getRuntime().exec(commandLine);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuffer output = new StringBuffer();
            char[] buffer = new char[4096];
            int read;

            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }

            reader.close();

            process.waitFor();

            Log.d("executeCommandLine", output.toString());

            retval = (process.exitValue() == 0);

        } catch (IOException e) {
            throw new RuntimeException("Unable to execute '" + commandLine + "'", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to execute '" + commandLine + "'", e);
        }

        return retval;
    }


    public static void Get(final String key, final OnResult<String> onGetPropertyResult) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Class clazz = Class.forName("android.os.SystemProperties");
                    if (clazz != null) {
                        Method method = clazz.getDeclaredMethod("get", String.class);
                        if (method != null) {
                            String prop = (String) method.invoke(null, key);
                            if (onGetPropertyResult != null)
                                onGetPropertyResult.onSuccess(prop);
                        } else {
                            Log.e(TAG, "Cannot reflect method get on class android.os.SystemProperties");
                            if (onGetPropertyResult != null)
                                onGetPropertyResult.onError(new Throwable("Cannot reflect method get on class android.os.SystemProperties"));
                        }
                    } else {
                        Log.e(TAG, "Cannot reflect android.os.SystemProperties");
                        if (onGetPropertyResult != null)
                            onGetPropertyResult.onError(new Throwable("Cannot reflect method get on class android.os.SystemProperties"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception during reflection: " + e.getMessage());
                    if (onGetPropertyResult != null)
                        onGetPropertyResult.onError(e);
                }
            }
        });
    }

    public static void Set(final String key, final String value, final OnResult<Boolean> onSetPropertyResult) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean success = false;
                    Class clazz = Class.forName("android.os.SystemProperties");
                    if (clazz != null) {
                        Method method = clazz.getDeclaredMethod("set", String.class, String.class);
                        if (method != null) {
                            method.invoke(null, key, value);
                            success = true;
                        } else {
                            Log.e(TAG, "Cannot reflect method get on class android.os.SystemProperties");
                        }
                    } else {
                        Log.e(TAG, "Cannot reflect android.os.SystemProperties");
                    }
                    if (onSetPropertyResult != null)
                        onSetPropertyResult.onSuccess(success);

                } catch (Exception e) {
                    Log.e(TAG, "Exception during reflection: " + e.getMessage());

                    if (onSetPropertyResult != null)
                        onSetPropertyResult.onError(e);
                }
            }
        });
    }
}

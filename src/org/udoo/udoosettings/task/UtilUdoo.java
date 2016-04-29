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
}

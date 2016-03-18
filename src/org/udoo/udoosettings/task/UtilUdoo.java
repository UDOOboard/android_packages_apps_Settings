package org.udoo.udoosettings.task;

import android.util.Log;

import org.udoo.udoosettings.interfaces.OnResult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
                try {
                    if (param != null && param.length() > 0) {
                        RandomAccessFile randomAccessFile = new RandomAccessFile(UENV_PATH, "rw");
                        String line;
                        long pos = 0;
                        while (!success && (line = randomAccessFile.readLine()) != null) {
                            if (line.startsWith(VIDEO_OUT)) {
                                long endLine = randomAccessFile.getFilePointer();
                                byte[] remainingBytes = new byte[(int) (randomAccessFile.length() - endLine)];
                                randomAccessFile.read(remainingBytes);
                                randomAccessFile.getChannel().truncate(pos);
                                randomAccessFile.seek(pos);
                                randomAccessFile.writeBytes(VIDEO_OUT + param + "\n");
                                randomAccessFile.write(remainingBytes);
                                randomAccessFile.close();
                                success = true;
                            } else
                                pos += randomAccessFile.getFilePointer();
                        }

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
                    while (line != null && line.length() > 0 && !find) {
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

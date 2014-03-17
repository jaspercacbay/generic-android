package com.cajama.background;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jasper on 1/24/14.
 */
public class TestUploadAsyncTask extends AsyncTask<File, Integer, String> {
    long totalSize;
    String server;
    Context context;
    OnAsyncResult onAsyncResult;
    Notification notif;
    NotificationManager nm;
    int lastPercent = 0;
    NotificationCompat.Builder nc;
    File currentFile;

    public boolean checkIfInitSent(File chunk, File... files) {
        for (int i = 0; i<files.length; i++){
            if(files[i].getName().endsWith(".zip") && files[i].getName().startsWith(chunk.getName().replaceAll(".%06d.part", "")) && files[i].exists()) {
                return false;
            }
        }
        return true;
    }

    public TestUploadAsyncTask(String server, Context context) {
        this.server = NetworkUtil.checkWebAddress(server);
        System.out.println(server);
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        System.out.println("pre-execute");
    }

    @Override
    protected String doInBackground(File... files) {
        int reportsToSend = files.length;
        HttpPost post;
        nc.setTicker("Uploading " + reportsToSend + " reports!");

        for (int i = 0; i < reportsToSend; i++) {
            if (files[i].getName().endsWith(".zip") && files[i].exists()) {
                lastPercent = 0;
                currentFile = files[i];
                post = new HttpPost(this.server.concat("api/init/"));
                HttpClient client = new DefaultHttpClient();

                nc.setContentTitle(currentFile.getName());
                try {
                    CustomMultiPartEntity custom = new CustomMultiPartEntity(new CustomMultiPartEntity.ProgressListener() {
                        @Override
                        public void transferred(long num) {
                            int currentPercent = (int) ((num / (float) totalSize)*100);
                            if (currentPercent > lastPercent) {
                                publishProgress(currentPercent);
                                lastPercent = currentPercent;
                            }
                        }
                    });
                    ContentBody cbFile = new FileBody(currentFile, "text/plain");
                    ContentBody cbFilename = new StringBody(currentFile.getName());
                    ContentBody cbName = new StringBody("file");

                    custom.addPart("name", cbName);
                    custom.addPart("filename", cbFilename);
                    custom.addPart("file", cbFile);
                    custom.addPart("checksum", new StringBody(Files.hash(currentFile, Hashing.md5()).toString()));

                    totalSize = custom.getContentLength();
                    System.out.println(totalSize);

                    post.setEntity(custom);
                    HttpResponse response = client.execute(post);
                    InputStream inputStream = response.getEntity().getContent();
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder sb = new StringBuilder();
                    String serverResponse;

                    while ( (serverResponse = r.readLine()) != null ) {
                        sb.append(serverResponse);
                    }

                    serverResponse = sb.toString().trim();
                    System.out.println(serverResponse);

                    if (serverResponse.equals("OK") ) {
                        currentFile.delete();
                        onAsyncResult.onResult(1, currentFile.getName(), serverResponse);
                    }
                    else if (sb.toString().trim().startsWith("RETYPE")) {
                        currentFile.delete();
                        onAsyncResult.onResult(-1, currentFile.getName(), serverResponse);
                    }
                    else if (sb.toString().equals("EXISTS")) {
                        currentFile.delete();
                        onAsyncResult.onResult(2, currentFile.getName(), serverResponse);
                    }
                    else {
                        System.out.println("failed: " + currentFile.getName());
                        onAsyncResult.onResult(0, currentFile.getName(), serverResponse);
                    }
                    inputStream.close();
                    r.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    onAsyncResult.onResult(0, currentFile.getName(), e.toString());
                    continue;
                }
            }
        }

        for (int i = 0; i < reportsToSend; i++) {
            if (files[i].getName().endsWith("part") && files[i].exists() && checkIfInitSent(files[i], files)) {
                lastPercent = 0;
                currentFile = files[i];
                post = new HttpPost(this.server.concat("api/chunk/"));
                HttpClient client = new DefaultHttpClient();

                nc.setContentTitle(currentFile.getName());
                try {
                    CustomMultiPartEntity custom = new CustomMultiPartEntity(new CustomMultiPartEntity.ProgressListener() {
                        @Override
                        public void transferred(long num) {
                            int currentPercent = (int) ((num / (float) totalSize)*100);
                            if (currentPercent > lastPercent) {
                                publishProgress(currentPercent);
                                lastPercent = currentPercent;
                            }
                        }
                    });
                    ContentBody cbFile = new FileBody(currentFile, "text/plain");
                    ContentBody cbFilename = new StringBody(currentFile.getName());
                    ContentBody cbName = new StringBody("file");

                    custom.addPart("name", cbName);
                    custom.addPart("filename", cbFilename);
                    custom.addPart("file", cbFile);

                    totalSize = custom.getContentLength();
                    System.out.println(totalSize);

                    post.setEntity(custom);
                    HttpResponse response = client.execute(post);
                    InputStream inputStream = response.getEntity().getContent();
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder sb = new StringBuilder();
                    String serverResponse;

                    while ( (serverResponse = r.readLine()) != null ) {
                        sb.append(serverResponse);
                    }

                    serverResponse = sb.toString().trim();
                    System.out.println(serverResponse);

                    if (serverResponse.equals("OK") ) {
                        currentFile.delete();
                        onAsyncResult.onResult(1, currentFile.getName(), serverResponse);
                    }
                    else if (sb.toString().trim().startsWith("RETYPE")) {
                        currentFile.delete();
                        onAsyncResult.onResult(-1, currentFile.getName(), serverResponse);
                    }
                    else if (sb.toString().equals("EXISTS")) {
                        currentFile.delete();
                        onAsyncResult.onResult(2, currentFile.getName(), serverResponse);
                    }
                    else {
                        System.out.println("failed: " + currentFile.getName());
                        onAsyncResult.onResult(0, currentFile.getName(), serverResponse);
                    }
                    inputStream.close();
                    r.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    onAsyncResult.onResult(0, currentFile.getName(), e.toString());
                    continue;
                }
            }
        }
        return "done";
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        System.out.println("progress: " + (int) progress[0]);
        /*if (notif.contentView != null) {
            notif.contentView.setProgressBar(R.id.status_progress, 100, (int) (progress[0]), false);
            notif.contentView.setTextViewText(R.id.status_text, "Uploading... "+(int) (progress[0])+"%");
            nm.notify(10001, notif);
        }*/
        if (nc != null && nm != null) {
            nc.setProgress(100, (int) (progress[0]), false);
            nc.setContentText("Uploading... "+(int) (progress[0])+"%");
            nc.setNumber((int) progress[0]);
            nm.notify(10001, nc.build());
        }
    }

    @Override
    protected void onPostExecute(String response) {
        //String message = "failed";
        //int resultCode = 0;

        nm.cancel(10001);

        /*nc.setContentText("yeah");
        nc.setContentTitle("Success!");
        nm.notify(10002, nc.build());*/

        /*if (response.equals("OK")) {
            message = currentFile.getName();
            resultCode = 1;
            currentFile.delete();
            System.out.println("upload successful");
            Toast.makeText(this.context, message + " has been uploaded!", Toast.LENGTH_LONG).show();
        }
        else if (response.startsWith("RETYPE")) {
            message = currentFile.getName();
            resultCode = -1;
            currentFile.delete();
            System.out.println("retype credentials");
            Toast.makeText(this.context, "Credentials not up to date!", Toast.LENGTH_LONG).show();
        }
        else {
            System.out.println("failed!!!");
            Toast.makeText(this.context, "failed upload!", Toast.LENGTH_LONG).show();
        }

        onAsyncResult.onResult(resultCode, message);*/
        //cancel(true);
        //stopSelf();
    }

    public interface OnAsyncResult {
        public abstract void onResult(int resultCode, String message, String response);
    }

    public void setOnResultListener(OnAsyncResult onAsyncResult) {
        if (onAsyncResult != null) this.onAsyncResult = onAsyncResult;
    }

    public void setNotification(Notification notification) {
        this.notif = notification;
    }

    public void setNotificationManager(NotificationManager notificationManager) {
        this.nm = notificationManager;
    }

    public void setBuilder(NotificationCompat.Builder builder) {
        this.nc = builder;
        nc.setTicker("Uploading reports");
        this.notif = builder.build();
    }

    public void deleteOneWeek (File o) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date filedate = format.parse(o.getName().split("_")[0]+o.getName().split("_")[1]);
        Date now = new Date();
        int daysDiff = (int) (now.getTime() - filedate.getTime()) / (24 * 60 * 60 * 1000);

        if (daysDiff >= 7 && o.exists()) {
            o.delete();
        }
    }
}

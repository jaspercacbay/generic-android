package com.cajama.background;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cajama.malarialite.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Created by Jasper on 4/1/14.
 */
public class QueryAsyncTask extends AsyncTask<String, Integer, String> {
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

    public QueryAsyncTask(String server, Context context) {
        this.server = NetworkUtil.checkWebAddress(server);
        System.out.println(server);
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        System.out.println("pre-execute");
    }

    @Override
    protected String doInBackground(String... strings) {
        int queries = strings.length;
        System.out.println("to query: " + queries);
        String all = strings[0];
        System.out.println("init: " + all);

        for (int i=1; i<strings.length; i++) all += "," + strings[i];
        System.out.println("all: " + all);

        HttpPost post;
        nc.setTicker("Querying " + queries + " validations!");

        this.server += context.getString(R.string.api_validate);
        System.out.println(this.server);

        post = new HttpPost(this.server);
        HttpClient client = new DefaultHttpClient();

        nc.setContentTitle("Querying");
        try {
            MultipartEntity mp = new MultipartEntity();
            ContentBody stringBody = new StringBody(all.trim());
            mp.addPart("string", stringBody);
            post.setEntity(mp);

            Log.d("valid", String.valueOf(post.getRequestLine()));

            HttpResponse response = client.execute(post);
            Log.d("valid", "response: " + response.getStatusLine());

            BufferedReader getReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 8192);
            final StringBuilder getResultBuilder = new StringBuilder();
            String getResult;
            try {
                while ((getResult = getReader.readLine()) != null) {
                    getResultBuilder.append(getResult);
                }
            } catch (Exception e) {
                Log.d("valid", "error in reading get result");
                e.printStackTrace();
                //stopSelf();
            }

            getReader.close();

            onAsyncResult.onResult(1, getResultBuilder.toString(), String.valueOf(response.getStatusLine()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
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
            nm.notify(10002, nc.build());
        }
    }

    @Override
    protected void onPostExecute(String response) {
        //String message = "failed";
        //int resultCode = 0;

        nm.cancel(10002);

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
        nc.setTicker("Querying validations");
        this.notif = builder.build();
    }
}

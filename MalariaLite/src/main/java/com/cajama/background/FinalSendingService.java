package com.cajama.background;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.cajama.malarialite.R;
import com.cajama.malarialite.entryLogs.QueueLogActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jasper on 8/4/13.
 */
public class FinalSendingService extends Service {
    final String TAG = "FinalSendingService";
    NotificationManager notificationManager;
    NotificationCompat.Builder mBuilder;
    public static final String BROADCAST_ACTION_QUEUE = "com.cajama.malarialite.entryLogs.QueueLogActivity";
    public static final String BROADCAST_ACTION_SENT = "com.cajama.malarialite.entryLogs.SentLogActivity";
    String onResult = "";
    static TestUploadAsyncTask asyncTask;
    File sentList, reportsDirectory;
    File[] reports;
    int count, tries=0;
    private final Handler handler1 = new Handler();
    private final Handler handler2 = new Handler();
    Intent intentQueueLog, intentSentLog;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("send") != null && intent.getStringExtra("send").equals("send")) {
                reports = reportsDirectory.listFiles();
                if (NetworkUtil.getConnectivityStatus(getApplicationContext()) != 0) sendFile();
            }
        }
    };

    TestUploadAsyncTask.OnAsyncResult onAsyncResult = new TestUploadAsyncTask.OnAsyncResult() {
        @Override
        public void onResult(int resultCode, String message, String response) {
            try {
                handler1.removeCallbacks(sendUpdatesToQueue);
                handler1.postDelayed(sendUpdatesToQueue, 1000);
                append_report(resultCode, message, response);
            } catch (Exception e) {
                Log.d(TAG, "error!");
                e.printStackTrace();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(receiver, new IntentFilter("com.cajama.background.FinalSendingService"));

        intentQueueLog = new Intent(BROADCAST_ACTION_QUEUE);
        intentSentLog = new Intent(BROADCAST_ACTION_SENT);

        mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setContentTitle("Uploading...");
        mBuilder.setContentText("ZipFile");
        mBuilder.setProgress(100, 0, false);
        mBuilder.setNumber(0);
        mBuilder.setTicker("Starting to upload reports!");
        mBuilder.setSmallIcon(R.drawable.icon);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent resultIntent = new Intent(this, QueueLogActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            //stackBuilder.addParentStack(QueueLogActivity.class);
            stackBuilder.addNextIntentWithParentStack(resultIntent);

            //stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
        }
        else {
            Intent intent = new Intent(this, QueueLogActivity.class);
            PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
            mBuilder.setContentIntent(activity);
        }

        notificationManager = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);

        asyncTask = new TestUploadAsyncTask(/*getString(R.string.server_address) ,getApplicationContext());*/PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.connection_pref), getString(R.string.server_address)), getApplicationContext());
        asyncTask.setNotificationManager(notificationManager);
        asyncTask.setBuilder(mBuilder);
        asyncTask.setOnResultListener(onAsyncResult);

        reportsDirectory = new File(getApplicationContext().getExternalFilesDir(null), "ZipFiles");
        if (!reportsDirectory.exists()) reportsDirectory.mkdir();
        reports = reportsDirectory.listFiles();

        sentList = new File(getApplicationContext().getExternalFilesDir(null), "sent_log.txt");
        if (!sentList.exists()) {
            try {
                sentList.createNewFile();
                Log.d(TAG, "Created sentlist file");
            } catch (IOException e) {
                Log.d(TAG, "Failed to create sentList.txt!");
                e.printStackTrace();
            }
        }

        count = 0;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
    }

    private Runnable sendUpdatesToQueue = new Runnable() {
        @Override
        public void run() {
            updateIntent(intentQueueLog);
        }
    };

    private Runnable sendUpdatesToSent = new Runnable() {
        @Override
        public void run() {
            updateIntent(intentSentLog);
        }
    };

    private void updateIntent(Intent intent) {
        intent.putExtra("update", "update");
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //int ret = super.onStartCommand(intent, flags, startId);
        Bundle extras = null;
        if (intent!=null) extras = intent.getExtras();
        String result = "";
        if (extras != null) result = extras.getString("message");

        if (result != "") Log.d(TAG, result);

        if (NetworkUtil.getConnectivityStatus(getApplicationContext()) != 0) {
            Log.d(TAG, "connected!");
            if (!(result.length() == 0)) {
                try {
                    String[] split = result.split("\n");
                    byte[] skByte = split[1].getBytes();
                    MessageDigest sha = MessageDigest.getInstance("SHA-1");
                    skByte = sha.digest(skByte);
                    //skByte = Arrays.copyOf(skByte, 16);
                    /*RSA rsa = new RSA(skByte);
                    new PostStringAsyncTask().execute(rsa.encryptRSA(skByte));*/
                    Log.d(TAG, "Posting new user and pass");
                    new PostStringAsyncTask().execute(split[0]+"\n"+byteArrayToHexString(skByte));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "failed to encrypt retpyed password");
                }
            }
            else sendFile();
            return START_STICKY;
        }
        else {
            return START_STICKY;
        }
    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result +=
                    Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

    public void sendFile() {
        //reports = reportsDirectory.listFiles();
        if (reportsDirectory.listFiles().length > 0) {
            Log.d(TAG, "# of files to send: " + String.valueOf(reportsDirectory.listFiles().length));
            if (asyncTask.getStatus() == AsyncTask.Status.PENDING) {
                /*FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File directory, String fileName) {
                        return fileName.endsWith(".zip");
                    }
                };
                reports = reportsDirectory.listFiles(filter);
                if (reports.length == 0) {
                    reports = reportsDirectory.listFiles();
                }*/

                reports = reportsDirectory.listFiles();
                asyncTask.execute(reports);
            }
            else if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                Log.d(TAG, "task already running: " + reports.length + " on queue");
            }
            else if (asyncTask.getStatus() == AsyncTask.Status.FINISHED) {
                Log.d(TAG, "task finished!");
                asyncTask = new TestUploadAsyncTask(/*getString(R.string.server_address), getApplicationContext());//*/PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.connection_pref), getString(R.string.server_address)), getApplicationContext());
                asyncTask.setNotificationManager(notificationManager);
                asyncTask.setBuilder(mBuilder);
                asyncTask.setOnResultListener(onAsyncResult);
                /*FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File directory, String fileName) {
                        return fileName.endsWith(".zip");
                    }
                };
                reports = reportsDirectory.listFiles(filter);
                if (reports.length == 0) {
                    reports = reportsDirectory.listFiles();
                }*/
                reports = reportsDirectory.listFiles();
                asyncTask.execute(reports);
            }
        }
        else {
            Log.d(TAG, "No reports to send!");
            //stopSelf();
        }
    }

    /*public void sendFile(int count) {
        //if (reportsDirectory.exists()) {    //
            //reports = reportsDirectory.listFiles(); //
            if (count < reports.length) {
            //for (File report : reports) {
                Log.d(TAG, "# of files to send: " + String.valueOf(reports.length));
                if(asyncTask.getStatus() == AsyncTask.Status.PENDING) {
                    asyncTask.execute(reports[count]);
                }
                else if(asyncTask.getStatus() == AsyncTask.Status.RUNNING){
                    //reports = reportsDirectory.listFiles();
                    Log.d(TAG, "task alreading running: " + reports.length + " on queue");
                    //asyncTask.execute(reports);
                }
                else if(asyncTask.getStatus() == AsyncTask.Status.FINISHED){
                    //asyncTask.execute(report);
                    //reports = reportsDirectory.listFiles();
                    //stopSelf();
                    //asyncTask.execute(reports);
                    Log.d(TAG, "task finished!");
                    sendNext();
                }
            }
            else {
                Log.d(TAG, "No reports to send");
                stopSelf();
            }
            //}
        //}
    }*/

    public void append_report(int resultCode, String filename, String message2) throws IOException {
        if (resultCode == 1 || resultCode == 2) {

            FileWriter fileWriter = new FileWriter(sentList, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            final String str = filename.split("\\.zip")[0];
            File root = new File(getExternalFilesDir(null), "ZipFiles");
            System.out.println("Checking if send report is the last part: " + root.getPath());

            if (root.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.startsWith(str);
                }
            }).length == 0) {
                String[] split = str.split("_");
                bufferedWriter.write(split[2]+"\n");
                bufferedWriter.write(split[1]+"\n");
                bufferedWriter.write(split[0]+"\n");
                bufferedWriter.close();
                Log.d(TAG, split[0] + "_" + split[1] + "_" + split[2] + " added to sent list");
                handler2.removeCallbacks(sendUpdatesToSent);
                handler2.postDelayed(sendUpdatesToSent, 1000);
            }
            //sendNext();
            if (resultCode == 2) System.out.println(filename + " was received before, di mo lang nakuha OK ni server!");
        }
        else if (resultCode == -1) {
            startDialog(-1);
        }
        else {
            Log.d(TAG, filename + " not added to sent list");
            //sendNext();
        }
        File f = new File(getExternalFilesDir(null), "log.txt");
        FileWriter fw = new FileWriter(f, true);
        fw.append(new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "\n" +  filename + ": " + message2 + "\n-------------\n");
        fw.close();
    }

    public void startDialog(int tries) {
        Intent intent = new Intent(this.getApplicationContext(), DialogActivity.class);
        intent.putExtra("passwd", this.getClass().getCanonicalName());
        intent.putExtra("tries", String.valueOf(tries));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void checkResult() {
        Log.d(TAG, "onResult = " + onResult);
        if (onResult.equals("OK") || onResult.endsWith("5")) sendNext();
        else {
            tries++;
            if (tries < 5) startDialog(5-tries);
            else sendNext();
        }
    }

    public void sendNext() {
        count++;
        //sendFile(count);
    }

    public class PostStringAsyncTask extends AsyncTask<String, Void, String> {
        String url = /*getString(R.string.server_address).concat(getString(R.string.api_retry)); //*/PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getString(R.string.connection_pref), getString(R.string.server_address)).concat(getString(R.string.api_retry));

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, strings[0]);
            HttpPost post = null;
            HttpClient client = null;
            MultipartEntity mp = null;

            try {
                client = new DefaultHttpClient();
                post = new HttpPost(url);

                mp = new MultipartEntity();
                ContentBody stringBody = new StringBody(strings[0]);
                mp.addPart("message", stringBody);
                post.setEntity(mp);

                Log.d(TAG, String.valueOf(post.getRequestLine()));

                HttpResponse response = client.execute(post);
                Log.d(TAG, "response: "+ response.getStatusLine());

                BufferedReader getReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 8192);
                final StringBuilder getResultBuilder = new StringBuilder();
                String getResult;
                try {
                    while ((getResult = getReader.readLine()) != null) {
                        getResultBuilder.append(getResult);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "error in reading get result");
                    e.printStackTrace();
                    //stopSelf();
                }

                getReader.close();

                onResult = getResultBuilder.toString();
                checkResult();
            } catch (Exception e) {
                e.printStackTrace();
                //stopSelf();
            }

            return null;
        }
    }

    /*public static class HttpMultiPartPost extends AsyncTask<File, Integer, String> {
        long totalSize;
        String server;
        Context context;
        OnAsyncResult onAsyncResult;
        Notification notif;
        NotificationManager nm;
        int lastPercent = 0;
        File currentFile;

        public HttpMultiPartPost(String server, Context context) {
            this.server = server;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            System.out.println("pre-execute");
        }

        @Override
        protected String doInBackground(File... files) {
            HttpClient client = new DefaultHttpClient();
            //HttpContext httpContext = new BasicHttpContext();
            HttpPost post = new HttpPost(this.server);

            currentFile = files[0];

            try {
                CustomMultiPartEntity custom = new CustomMultiPartEntity(new CustomMultiPartEntity.ProgressListener() {
                    @Override
                    public void transferred(long num) {
                        int currentPercent = (int) ((num / (float) totalSize)*100);
                        if (currentPercent > lastPercent ) {//&& currentPercent % 5 == 0) {
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

                //if (str.equals("OK") || str.startsWith("RETYPE")) {
                    //currentFile.delete();
                    //onAsyncResult.onResult(1, currentFile.getName());
                    //return "ok";
                //}
                //else if (sb.toString().trim().startsWith("RETYPE")) {
                    //currentFile.delete();
                    //onAsyncResult.onResult(-1, currentFile.getName());
                    //return "retype";
                //}
                //else onAsyncResult.onResult(0, "failed");

                //return "fail";
                return serverResponse;

            } catch (Exception e) {
                e.printStackTrace();
                return "fail";
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            System.out.println("progress: " + (int) progress[0]);
            if (notif.contentView != null) {
                notif.contentView.setProgressBar(R.id.status_progress, 100, (int) (progress[0]), false);
                notif.contentView.setTextViewText(R.id.status_text, "Uploading... "+(int) (progress[0])+"%");
                nm.notify(10001, notif);
            }
        }

        @Override
        protected void onPostExecute(String response) {
            String message = "failed";
            int resultCode = 0;

            nm.cancel(10001);

            if (response.equals("OK")) {
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

            onAsyncResult.onResult(resultCode, message);
            //cancel(true);
            //stopSelf();
        }

        public interface OnAsyncResult {
            public abstract void onResult(int resultCode, String message);
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
    }*/
}

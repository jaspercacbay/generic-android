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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.cajama.malarialite.R;
import com.cajama.malarialite.entryLogs.SentLogActivity;
import com.jamesmurty.utils.XMLBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by Jasper on 3/28/14.
 */
public class ValidationService extends Service {
    final String TAG = "ValidationService";
    NotificationManager notificationManager;
    NotificationCompat.Builder mBuilder;
    public static final String BROADCAST_ACTION_SENT = "com.cajama.malarialite.entryLogs.SentLogActivity";
    String onResult = "";
    File[] query;
    private final Handler handler = new Handler();
    Intent sentLog;
    static QueryAsyncTask asyncTask;
    ArrayList<String> list;
    File root;

    QueryAsyncTask.OnAsyncResult onAsyncResult = new QueryAsyncTask.OnAsyncResult() {
        @Override
        public void onResult(int resultCode, String message, String response) {
            append_validation(resultCode, message, response);
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("query") != null && intent.getStringExtra("query").equals("query")) {
                //reports = reportsDirectory.listFiles();
                if (NetworkUtil.getConnectivityStatus(getApplicationContext()) != 0) sendQuery();
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
        registerReceiver(receiver, new IntentFilter("com.cajama.background.ValidationService"));

        sentLog = new Intent(BROADCAST_ACTION_SENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Validation...");
        mBuilder.setContentText("test");
        mBuilder.setSmallIcon(R.drawable.icon);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent resultIntent = new Intent(this, SentLogActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            //stackBuilder.addParentStack(QueueLogActivity.class);
            stackBuilder.addNextIntentWithParentStack(resultIntent);

            //stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
        }
        else {
            Intent intent = new Intent(this, SentLogActivity.class);
            PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
            mBuilder.setContentIntent(activity);
        }

        notificationManager = (NotificationManager) getApplicationContext().getSystemService(getApplicationContext().NOTIFICATION_SERVICE);

        asyncTask = new QueryAsyncTask(/*getString(R.string.server_address) ,getApplicationContext());*/PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.connection_pref), getString(R.string.server_address)), getApplicationContext());
        asyncTask.setNotificationManager(notificationManager);
        asyncTask.setBuilder(mBuilder);
        asyncTask.setOnResultListener(onAsyncResult);
    }

    private void sendQuery() {
        root = new File(getExternalFilesDir(null), "Reports");
        if (!root.exists()) root.mkdirs();
        query = root.listFiles();
        System.out.println("query.length = " + query.length);
        if (query.length > 0) {
            list = new ArrayList<String>();
            String all = "";
            for (int i=0; i<query.length; i++) {
                File f = new File(query[i], "validation.xml");
                System.out.println(f.getAbsolutePath());
                if (!f.exists()) {
                    System.out.println(query[i].getName() + " dne!");//{str[i] = query[i].getName();System.out.println(str[i]);}
                    //list.add(query[i].getName());
                    all += query[i].getName() + ",";
                    list.add(query[i].getName());
                }
                else System.out.println("next");
            }
            if (all.length()!=0) {
                all = all.substring(0, all.length()-1);
                System.out.println("all: " + all);
            }
            else {
                System.out.println("nothing to query");
            }

            if (all != null && all.length() > 0) {
                Log.d(TAG, "# of queries: " + String.valueOf(query.length));
                if (asyncTask.getStatus() == AsyncTask.Status.PENDING) {

                    asyncTask.execute(all);
                }
                else if (asyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                    Log.d(TAG, "task already running!");
                }
                else {
                    asyncTask = new QueryAsyncTask(/*getString(R.string.server_address), getApplicationContext());//*/PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.connection_pref), getString(R.string.server_address)), getApplicationContext());
                    asyncTask.setNotificationManager(notificationManager);
                    asyncTask.setBuilder(mBuilder);
                    asyncTask.setOnResultListener(onAsyncResult);

                    asyncTask.execute(all);
                }
            }
            else {
                Log.d(TAG, "No queries!");
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
    }

    private Runnable sendUpdatesToSent = new Runnable() {
        @Override
        public void run() {
            updateIntent(sentLog);
        }
    };

    private void updateIntent(Intent intent) {
        intent.putExtra("update", "update");
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (NetworkUtil.getConnectivityStatus(getApplicationContext()) != 0) {
            Log.d(TAG, "connected");
            sendQuery();
            return START_STICKY;
        }
        else {
            Log.d(TAG, "no internet!");
            return START_STICKY;
        }
    }

    private void append_validation(int resultCode, String message, String response) {
        if (resultCode == 1 && response.contains("200 OK")) {
            System.out.println(message);
            //{"20140402_112204_jasper": ["blah", "blahblahblah"]}

            String[] tags = new String[]{"diagnosis", "remarks"};
            String content = "";

            try {
                JSONObject reports = new JSONObject(message);
                for (int i=0; i<list.size(); i++) {
                    if (reports.has(list.get(i))) {
                        JSONArray jsonArray = reports.getJSONArray(list.get(i));
                        XMLBuilder builder = XMLBuilder.create("validation");
                        builder.element(tags[0]).text(jsonArray.getString(0));
                        builder.element(tags[1]).text(jsonArray.getString(1));
                        Properties outputProperties = new Properties();
                        outputProperties.put(javax.xml.transform.OutputKeys.METHOD, "xml");
                        outputProperties.put(javax.xml.transform.OutputKeys.INDENT, "yes");
                        outputProperties.put("{http://xml.apache.org/xslt}indent-amount", "2");
                        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");

                        content = builder.asString(outputProperties);

                        File report = new File(root, "/" + list.get(i) + "/validation.xml");
                        OutputStream os = new FileOutputStream(report, false);
                        os.write(content.getBytes());
                        os.close();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            handler.removeCallbacks(sendUpdatesToSent);
            handler.postDelayed(sendUpdatesToSent, 1000);
        }
        else {
            Log.d(TAG, "failed");
        }
    }
}

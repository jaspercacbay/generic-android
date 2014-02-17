package com.cajama.background;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.cajama.malarialite.R;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Jasper on 9/8/13.
 */
public class UpdateService extends Service {
    final String TAG = "UpdateService";
    String versionName;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        //Toast.makeText(this, TAG + " Started", Toast.LENGTH_LONG).show();

        try {
            PackageInfo info = getPackageManager().getPackageInfo(this.getPackageName(), 0);
            versionName = info.versionName;
            Log.d(TAG, "version name = " + versionName);
            if (internetAvailable()) new DownloadApkAsyncTask(getApplicationContext()).execute(new URL(getString(R.string.server_address).concat(getString(R.string.api_apk))));
            else {
                Log.d(TAG, "no internet");
                //Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } finally {
            stopSelf();
            return ret;
        }
    }

    public boolean internetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (info != null) {
                if (info.isConnected()) return true;
            }
        }
        return false;
    }

    public class DownloadApkAsyncTask extends AsyncTask<URL, Integer, Boolean> {
        private static final String TAG = "DownloadApkAsyncTask";
        NotificationHelper nh;
        Intent intent;

        public DownloadApkAsyncTask(Context context) {
            nh = new NotificationHelper(context);
        }

        @Override
        protected Boolean doInBackground(URL... urls) {
            boolean succeeded = false;
            intent = Update(urls[0]);
            return true;
        }

        protected void onProgressUpdate(Integer... progress) {
            //This method runs on the UI thread, it receives progress updates
            //from the background thread and publishes them to the status bar
            nh.progressUpdate(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result)    {
            //The task is complete, tell the status bar about it
            //nh.completed();
            Log.d(TAG, "asynctask done!");
            if (intent != null) {
                nh.createNotification(1);
                Log.d(TAG, "may update na i-install");
                startActivity(intent);
                nh.completed(1);
            }
            else {
                Log.d(TAG, "di kelangan i-update");
            }
        }

        public Intent Update(URL apkurl){
            Intent intent =  null;
            try {

                HttpClient client = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();

                HttpParams params = new BasicHttpParams();
                params.setParameter("http.protocol.handle-redirects", false);

                BasicCookieStore cookieStore = new BasicCookieStore();
                localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

                MultipartEntity mp = new MultipartEntity();
                ContentBody stringBody = new StringBody(versionName);
                mp.addPart("message", stringBody);

                HttpPost post = new HttpPost(apkurl.toString());
                post.setParams(params);
                post.setEntity(mp);


                HttpResponse response = client.execute(post, localContext);

                Log.d(TAG, "response: " + response.getStatusLine());

                if (response.getStatusLine().getStatusCode() == 200) {
                    return null;
                }

                String redirect = "";

                Header locationHeader = response.getFirstHeader("location");

                if (locationHeader != null) {
                    redirect = locationHeader.getValue();
                    Log.d(TAG, redirect);

                    HttpClient client1 = new DefaultHttpClient();
                    HttpGet get1 = new HttpGet(redirect);
                    HttpResponse response1 = client1.execute(get1);

                    InputStream is = response1.getEntity().getContent();
                    int contentLength = (int) response1.getEntity().getContentLength();

                    String[] split = redirect.split("/");
                    Log.d(TAG, split[split.length-1]);

                    File file = new File(Environment.getExternalStorageDirectory() + "/Android/data/com.cajama.malaria/files/", split[split.length-1]);
                    //File file = new File(getApplicationContext().getExternalFilesDir(null), split[split.length-1]);
                    if (file.exists()) file.delete();
                    file.createNewFile();

                    FileOutputStream fos = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int len = 0;
                    int total = 0;
                    while ((len = is.read(buffer)) != -1) {
                        total += len;
                        publishProgress(total * 100 / contentLength);
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    is.close();

                    PackageInfo info = getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), 0);
                    Log.d(TAG, "download apk version name = "+ info.versionName);
                    if (!info.versionName.equals(versionName)) {
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Android/data/com.cajama.malaria/files/" + file.getName())), "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    Log.d(TAG, "Download done!");
                }
                else {
                    Log.d(TAG, "failed redirect");
                }
            } catch (Exception e) {
                Log.e(TAG, "erroooooorrrrr!!!!!!");
                e.printStackTrace();
            } finally {
                return intent;
            }
        }
    }
}

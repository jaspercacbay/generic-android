package com.cajama.background;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Jasper on 8/8/13.
 */
public class SyncDBAsyncTask extends AsyncTask<String, Void, String> {
    String url;
    String TAG = "SyncDBAsyncTask";
    OnAsyncResult onAsyncResult;
    
    public SyncDBAsyncTask(String url, String api) {
    	this.url = NetworkUtil.checkWebAddress(url).concat(api);
        Log.d(TAG, url);
    }

    public void setOnResultListener(OnAsyncResult onAsyncResult) {
        if (onAsyncResult != null) this.onAsyncResult = onAsyncResult;
    }

    @Override
    protected String doInBackground(String... strings) {
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

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, String.valueOf(post.getRequestLine()));

        try {
        	HttpResponse response = client.execute(post);

            InputStream is = response.getEntity().getContent();//getResponse.getEntity().getContent();

            BufferedReader br = null;
            final StringBuilder sb = new StringBuilder();

            String line;
            try {

                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //line = sb.toString();
            Log.d(TAG, sb.toString());
            //Log.d(TAG, "OK");

            if (sb.toString().trim().equals("OK")) {
            	onAsyncResult.onResult(0, "updated");
            }
            //else if (sb.toString().trim().startsWith("<")) onAsyncResult.onResult(-1, "failed");
            else onAsyncResult.onResult(1, sb.toString().trim());

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "error!");
            onAsyncResult.onResult(-1, "failed");
        }

        return null;
    }

    public interface OnAsyncResult {
        public abstract void onResult(int resultCode, String message);
    }
}
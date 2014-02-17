package com.cajama.malarialite.encryption;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Jasper on 8/8/13.
 */
public class GetPublicKeyTask extends AsyncTask<String, Void, String> {
    String url = "http://10.40.93.115/api/key";
    String TAG = "GetPublicKeyTask";
    OnGetKeyResult onAsyncResult;

    public void setOnResultListener(OnGetKeyResult onAsyncResult) {
        if (onAsyncResult != null) this.onAsyncResult = onAsyncResult;
    }

    @Override
    protected String doInBackground(String... strings) {
    	//Log.d(TAG, String.valueOf(R.string.server_send_address));
        HttpGet get = null;
        HttpClient client = null;

        try {
            client = new DefaultHttpClient();
            get = new HttpGet(url);

            HttpResponse getResponse = client.execute(get);

            InputStream is = getResponse.getEntity().getContent();

            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();

            String line;
            try {

                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
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

            onAsyncResult.onResult(1, sb.toString());

            Log.d(TAG, sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "error!");
            onAsyncResult.onResult(0, "failed");
        }

        // TODO Auto-generated method stub
        return null;
    }

    public interface OnGetKeyResult {
        public abstract void onResult(int resultCode, String message);
    }
}
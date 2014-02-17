package com.cajama.malarialite.newreport;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.format.Time;
import android.util.Base64;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cajama.malarialite.encryption.AES;
import com.cajama.malarialite.encryption.RSA;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

/**
 * Created by GMGA on 8/5/13.
 */
public class AssembleData {
    private Handler handler = new Handler();
    Intent intentFinish;
    ProgressBar progressBar;
    TextView textView;
    ArrayList<String> entryList,fileList, accountData;
    String USERNAME;
    Context c;
    AES aes;

    public static final String BROADCAST_FINISH = "com.cajama.malarialite.newreport.NewReportActivity";
    private static final String PATIENT_TXT_FILENAME = "textData.xml";
    private static final String ACCOUNT_TXT_FILENAME = "accountData.xml";
    private static final String PATIENT_ZIP_FILENAME = "entryData.zip";
    private static final String AES_FILENAME = "cipherZipFile.zip";

    public AssembleData(Context c,ArrayList<String> entryList ,ArrayList<String> fileList, ArrayList<String> accountData, String USERNAME){
        this.c = c;
        this.entryList=entryList;
        this.fileList = fileList;
        this.accountData = accountData;
        this.USERNAME = USERNAME;
        intentFinish = new Intent(BROADCAST_FINISH);
    }
    private String[] getFirstZipArray(){
        try{
            fileList.add(0,c.getExternalFilesDir(null).getPath() + "/" + PATIENT_TXT_FILENAME);
        }
        catch (Exception e){
            Log.v("Error", "arrayList error");
        }
        String[] entryData = new String[fileList.size()];

        return fileList.toArray(entryData);

    }

    private String[] getSecondZipArray(){
        String[] travelData = new String[2];
        travelData[0] = c.getExternalFilesDir(null).getPath() + "/" + ACCOUNT_TXT_FILENAME;
        travelData[1] = c.getExternalFilesDir(null).getPath() + "/" + AES_FILENAME;
        return travelData;
    }

    public void setView (ProgressBar progressBar, TextView textView) {
        this.progressBar = progressBar;
        this.textView = textView;
    }

    public void start() {

        //create patient details file
        File entryFile = new File (c.getExternalFilesDir(null), PATIENT_TXT_FILENAME);
        MakeTextFile patient = new MakeTextFile(entryFile,entryList, false);
        patient.writeTextFile();

        //compress patient data file and images to a zip file
        File zipFile1 = new File (c.getExternalFilesDir(null), PATIENT_ZIP_FILENAME);
        Compress firstZip = new Compress(getFirstZipArray(),zipFile1.getPath());
        firstZip.zip();

        //hash secret key
        try {
            Log.v("AES","Start AES");
            byte[] skByte = accountData.get(1).getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            skByte = sha.digest(skByte);
            System.out.println(skByte.length);
            if (Build.VERSION.SDK_INT >= 9)
                skByte = Arrays.copyOf(skByte, 16); // use only first 128 bit
            else {
                byte[] src = new byte[16];
                System.arraycopy(skByte, 0, src, 0, 16);
                skByte = src;
            }
            /*byte[] src = skByte;
            System.arraycopy(src, 0, skByte, 0, 16);*/

            SecretKeySpec secretKey = new SecretKeySpec(skByte, "AES");

            Log.v("SeckretKeybase64", Base64.encodeToString(skByte,Base64.DEFAULT));

            //AES encrypt patient zip file
            //Log.v("AES","new AES");
            aes = new AES(secretKey);
            aes.setLayout(progressBar, textView);
            File AESFile = new File(c.getExternalFilesDir(null),AES_FILENAME);
            aes.encryptAES(zipFile1,AESFile);
            Thread.sleep(1000);
            progressBar.setIndeterminate(true);
            Log.v("AES","end AES");
            //decryption test
            /*File test = new File(c.getExternalFilesDir(null),"clearZip.zip");
            aes.decryptAES(AESFile,test);*/

            //RSA encrypt private key
            Log.v("ENCRYPTION","Start RSA");
            //RSA rsa = new RSA();
            RSA rsa = new RSA(skByte);
            //Log.v("ENCRYPTION","set RSA");
            Log.v("ENCRYPTION", "Private key:" + Base64.encodeToString(skByte,Base64.DEFAULT));
            accountData.set(1, rsa.encryptRSA(skByte));

            Log.v("ENCRYPTION","End RSA");

            //RSA decryption test
            //Log.v("ENCRYPTION", rsa.decryptRSA(Base64.decode(accountData.get(1),Base64.DEFAULT)));
        } catch (Exception e){
            Log.v("Encryption","exception" + e);
        }

        //create private key text file
        File accountFile = new File(c.getExternalFilesDir(null),ACCOUNT_TXT_FILENAME);
        MakeTextFile account = new MakeTextFile(accountFile,accountData, false);
        account.writeTextFile();

        //compress patient zip file and private key text file to a 2nd zip file
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        File zipFile2 = new File (c.getExternalFilesDir("ZipFiles"), today.format("%m%d%Y_%H%M%S")+"_"+ USERNAME + ".zip");
        Compress secondZip = new Compress(getSecondZipArray(),zipFile2.getPath());
        secondZip.zip();

        handler.removeCallbacks(finish);
        handler.postDelayed(finish, 1000);
    }

    private Runnable finish = new Runnable() {
        @Override
        public void run() {
            intentFinish.putExtra("finish", "finish");
            c.sendBroadcast(intentFinish);
        }
    };

}
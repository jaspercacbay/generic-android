package com.cajama.malarialite.encryption;

import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by GMGA on 8/4/13.
 */
public class AES {
    SecretKeySpec sk;
    int size, total;
    //int myProgress;
    ProgressBar progressBar;
    TextView textView;

    static final String TAG = "SymmetricAlgorithmAES";
    public AES(SecretKeySpec sk){
        this.sk = sk;
    }

    public void encryptAES(File clearTextFile, File cipherTextFile){
        Log.v(TAG,"Start encryption");

        try{
            Cipher cipher = Cipher.getInstance("AES");
            Log.v(TAG,"New cipher" + sk);
            cipher.init(Cipher.ENCRYPT_MODE,sk);
            Log.v(TAG,"Cipher created");

            FileInputStream fis = new FileInputStream(clearTextFile);
            FileOutputStream fos = new FileOutputStream(cipherTextFile);
            CipherOutputStream cos = new CipherOutputStream(fos,cipher);
            Log.v(TAG,"Streams created");
            long startTime = System.currentTimeMillis();
            byte[] block = new byte[262144];
            total = fis.available();
            progressBar.setIndeterminate(false);
            Thread.sleep(1000);
            while ((size = fis.read(block)) != -1) {
                progressBar.setProgress((int) (100 - ((fis.available()/(float)total) * 100)));
                cos.write(block, 0, size);
                Log.v("AES","Size:"+String.valueOf(fis.available()));
            }
            long difference = System.currentTimeMillis() - startTime;
            Log.v("AES","Time" + String.valueOf(difference/1000));
            cos.close();

        } catch (Exception e){ Log.v(TAG,"AES encryption error");}
    }

    public void decryptAES(File cipherTextFile,File clearTextFile){
        try{
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,sk);

            FileInputStream fis = new FileInputStream(cipherTextFile);
            CipherInputStream cis = new CipherInputStream(fis,cipher);
            FileOutputStream fos = new FileOutputStream(clearTextFile);

            byte[] block = new byte[8];
            while ((size = cis.read(block)) != -1) {
                fos.write(block, 0, size);
            }
            fos.close();

        } catch (Exception e){  Log.v(TAG,"AES decryption error"); }
    }

    /*public void setSize(int total) {
        this.total = total;
    }

    public int getSize() {
        return total;
    }*/

    public void setLayout (ProgressBar progressBar, TextView textView) {
        this.progressBar = progressBar;
        this.textView = textView;
    }
}
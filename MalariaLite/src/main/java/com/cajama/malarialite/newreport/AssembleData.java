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
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
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
    File AESFile;
    String [] parts;
    Time today;

    public static final String BROADCAST_FINISH = "com.cajama.malarialite.newreport.NewReportActivity";
    private static final String PATIENT_TXT_FILENAME = "textData.xml";
    private static final String ACCOUNT_TXT_FILENAME = "accountData.xml";
    private static final String PATIENT_ZIP_FILENAME = "entryData.zip";
    private static final String AES_FILENAME = "cipherZipFile.zip";

    public AssembleData(Context c,ArrayList<String> entryList ,ArrayList<String> fileList, ArrayList<String> accountData, String USERNAME, Time created){
        this.c = c;
        this.entryList=entryList;
        this.fileList = fileList;
        this.accountData = accountData;
        this.USERNAME = USERNAME.toLowerCase();
        this.today = created;
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

    private String[] getThirdZipArray(String asdf) {
        String[] travelData = new String[3];
        //travelData[0] = c.getExternalFilesDir(null).getPath() + "/" + asdf;
        travelData[0] = c.getExternalFilesDir(null).getPath() + "/" + ACCOUNT_TXT_FILENAME;
        travelData[1] = c.getExternalFilesDir(null).getPath() + "/" + "cipher_listahan";
        return travelData;
    }

    public void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
        byte[] buf = new byte[(int) numBytes];
        int val = raf.read(buf);
        if(val != -1) {
            bw.write(buf);
        }
    }

    public String splitFile(String fname) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(c.getExternalFilesDir(null).getPath() + "/" + fname, "r");
        long sourceSize = raf.length();
        long remainingBytes;

        int maxReadBufferSize = 128 * 1024; //128KB chunks
        int numSplits = (int) (Math.floor(sourceSize / maxReadBufferSize) + 1);
        Log.d("assemble", String.valueOf(sourceSize));
        Log.d("assemble", String.valueOf(numSplits));
        if (numSplits > 1) {
            remainingBytes = sourceSize % (numSplits-1);
        } else {
            remainingBytes = sourceSize;
            numSplits = 0;
        }

        parts = new String[numSplits+1];

        File tobedisgested;
        HashCode md5;
        String md5Hex;

        Files.touch(new File(c.getExternalFilesDir(null), fname + ".txt"));
        PrintWriter writer = new PrintWriter(c.getExternalFilesDir(null).getPath() + "/" + fname + ".txt", "UTF-8");

        for(int destIx=1; destIx <=numSplits; destIx++) {
            Files.touch(new File(c.getExternalFilesDir("ZipFiles").getPath() + "/" + fname+String.format("%05d", destIx)+".part"));
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(c.getExternalFilesDir("ZipFiles").getPath() + "/" + fname+String.format("%05d", destIx)+".part"));

            readWrite(raf, bw, maxReadBufferSize);
            bw.close();

            tobedisgested = new File(c.getExternalFilesDir("ZipFiles").getPath() + "/" + fname+String.format("%05d", destIx)+".part");
            md5 = Files.hash(tobedisgested, Hashing.md5());
            md5Hex = md5.toString();

            writer.println(fname+String.format("%05d", destIx)+".part"+" "+md5Hex);
            parts[destIx-1] = fname+String.format("%05d", destIx)+".part";
        }

        if(remainingBytes > 0) {
            Files.touch(new File(c.getExternalFilesDir("ZipFiles").getPath() + "/" + fname+String.format("%05d", numSplits+1)+".part"));
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(c.getExternalFilesDir("ZipFiles").getPath() + "/" + fname+String.format("%05d", numSplits+1)+".part"));
            readWrite(raf, bw, remainingBytes);
            bw.close();

            tobedisgested = new File(c.getExternalFilesDir("ZipFiles").getPath() + "/" + fname+String.format("%05d", numSplits+1)+".part");
            md5 = Files.hash(tobedisgested, Hashing.md5());
            md5Hex = md5.toString();

            writer.println(fname+String.format("%05d", numSplits+1)+".part"+" "+md5Hex);
            parts[numSplits] = fname+String.format("%05d", numSplits+1)+".part";
        }

        raf.close();
        writer.close();
        return fname + ".txt";
    }

    public void setView (ProgressBar progressBar, TextView textView) {
        this.progressBar = progressBar;
        this.textView = textView;
    }

    public void start() throws Exception {

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
            AESFile = new File(c.getExternalFilesDir(null),AES_FILENAME);
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

            Log.v("ENCRYPTION","End RSA");//hich i would like to read in Java and split this file into n (user input) number of files through the code dynamically.

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

        String nowname = today.format("%m%d%Y_%H%M%S")+"_"+ USERNAME + ".zip";

        File zipFile2 = new File (c.getExternalFilesDir(null), nowname);
        Compress secondZip = new Compress(getSecondZipArray(),zipFile2.getPath());
        secondZip.zip();

        String listahanname = splitFile(nowname);

        File listahan = new File(c.getExternalFilesDir(null), listahanname);
        File AESFile2 = new File(c.getExternalFilesDir(null),"cipher_listahan");
        aes.encryptAES(listahan, AESFile2);

        File zipFile3 = new File (c.getExternalFilesDir("ZipFiles"), nowname);
        Compress thirdZip = new Compress(getThirdZipArray(listahanname),zipFile3.getPath());
        thirdZip.zip();

        entryFile.delete();
        AESFile.delete();
        zipFile1.delete();
        zipFile2.delete();
        accountFile.delete();
        listahan.delete();
        AESFile2.delete();

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
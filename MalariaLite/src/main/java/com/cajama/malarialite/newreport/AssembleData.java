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
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import java.io.File;
import java.io.PrintWriter;
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
    Time today;
    String reportRoot;
    long fsize;
    int kbsize;

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

    private String[] getThirdZipArray() {
        String[] travelData = new String[2];
        travelData[0] = c.getExternalFilesDir(null).getPath() + "/" + ACCOUNT_TXT_FILENAME;
        travelData[1] = c.getExternalFilesDir(null).getPath() + "/" + "cipher_listahan";
        return travelData;
    }

    public File splitFile(File o) throws Exception {
        File chunkfile;
        ByteSink chunk;
        HashCode md5;
        byte[] buf;

        ByteSource orig = Files.asByteSource(o);

        fsize = o.length();

        kbsize = 32;
        int maxReadBufferSize = kbsize * 1024; //32KB chunks

        File chunklist = new File(o.getParent(), o.getName() + ".txt");
        Files.touch(chunklist);
        PrintWriter writer = new PrintWriter(chunklist, "UTF-8");

        int destIx=1;

        do {
            buf = orig.slice((destIx-1)*maxReadBufferSize,maxReadBufferSize).read();

            if (buf.length > 0) {
                chunkfile = new File(o.getParent()+"/ZipFiles", o.getName().replaceAll(".zip", "")+ "_" + fsize + "_" + kbsize+ String.format(".zip.%06d.part", destIx));
                Files.touch(chunkfile);

                chunk = Files.asByteSink(chunkfile);
                chunk.write(buf);

                md5 = Files.hash(chunkfile, Hashing.md5());
                writer.println(chunkfile.getName()+" "+md5.toString());

                destIx++;
            }

        } while (buf.length > 0);

        writer.close();
        return chunklist;
    }

    public void setView (ProgressBar progressBar, TextView textView) {
        this.progressBar = progressBar;
        this.textView = textView;
    }

    public void start() throws Exception {

        reportRoot = c.getExternalFilesDir("Reports").getPath() + "/" + today.format("%Y%m%d_%H%M%S")+"_"+ USERNAME;

        //create patient details file
        File entryFile = new File (c.getExternalFilesDir(null), PATIENT_TXT_FILENAME);
        MakeTextFile patient = new MakeTextFile(entryFile,entryList, false);
        patient.writeTextFile();

        //compress patient data file and images to a zip file
        File zipFile1 = new File (c.getExternalFilesDir(null), PATIENT_ZIP_FILENAME);
        Compress firstZip = new Compress(getFirstZipArray(),zipFile1.getPath());
        firstZip.zip();

        String [] reportfiles = fileList.toArray(new String[fileList.size()]);

        for (int cnt = 0; cnt< fileList.size(); cnt++) {
            File from = new File(reportfiles[cnt]);
            File to;
            if (from.getName().endsWith(".jpg")) {
                to = new File(reportRoot+"/Pictures/"+from.getName());
            } else {
                to = new File(reportRoot+"/"+from.getName());
            }

            Files.createParentDirs(to);

            Files.touch(to);
            Files.copy(from, to);
        }

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

        String nowname = today.format("%Y%m%d_%H%M%S")+"_"+ USERNAME+".zip";

        File zipFile2 = new File (c.getExternalFilesDir(null), nowname);
        Compress secondZip = new Compress(getSecondZipArray(),zipFile2.getPath());
        secondZip.zip();

        File listahan = splitFile(zipFile2);
        File AESFile2 = new File(c.getExternalFilesDir(null),"cipher_listahan");
        aes.encryptAES(listahan, AESFile2);

        nowname = today.format("%Y%m%d_%H%M%S")+"_"+ USERNAME+ "_" + fsize + "_" + kbsize+".zip";
        File zipFile3 = new File (c.getExternalFilesDir("ZipFiles"), nowname);
        Compress thirdZip = new Compress(getThirdZipArray(),zipFile3.getPath());
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
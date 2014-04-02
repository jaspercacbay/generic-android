package com.cajama.malarialite.newreport;

import android.util.Log;

import com.jamesmurty.utils.XMLBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by GMGA on 7/28/13.
 */
public class MakeTextFile {
    private File file;
    private ArrayList<String> contentArray;
    private boolean append;

    public MakeTextFile(File file, ArrayList<String> contentArray, boolean append){
        this.file = file;
        this.contentArray = contentArray;
        this.append = append;
    }

    private String combineData(){
        String content="";
        XMLBuilder builder = null;
        int index = 0;
        String[] tags;

        boolean isEntry = contentArray.size() != 2;

        if (isEntry) {
            tags = new String[]{
                    "date-created",
                    "time-created",
                    "latitude",
                    "longitude",
                    "priority",
                    "species",
                    "description",
                    "region",
                    "province",
                    "municipality",
                    "flags"
            };
        } else {
            tags = new String[] {
                    "user",
                    "pass"
            };
        }

        try {
            if (isEntry) builder = XMLBuilder.create("entry");
            else builder = XMLBuilder.create("credentials");
            Log.d("MakeTextFile", String.valueOf(contentArray.size()));
            for(String data : contentArray){
                builder.element(tags[index++]).text(data);
                Log.v("WRITE","CONTENT: " + data);
            }

            Properties outputProperties = new Properties();
            outputProperties.put(javax.xml.transform.OutputKeys.METHOD, "xml");
            outputProperties.put(javax.xml.transform.OutputKeys.INDENT, "yes");
            outputProperties.put("{http://xml.apache.org/xslt}indent-amount", "2");
            outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");

            content = builder.asString(outputProperties);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return content;
    }

    public void writeTextFile(){
        Log.v("write","text");
        try {
            OutputStream os = new FileOutputStream(file,append);

            os.write(combineData().getBytes());
            os.close();
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }
}

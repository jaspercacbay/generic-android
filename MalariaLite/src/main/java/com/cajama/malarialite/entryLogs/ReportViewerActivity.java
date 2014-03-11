package com.cajama.malarialite.entryLogs;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cajama.malarialite.R;
import com.cajama.malarialite.newreport.FullscreenPhotoActivity;
import com.cajama.malarialite.newreport.ImageAdapter;
import com.cajama.malarialite.newreport.myBitmap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Jasper on 3/10/14.
 */
public class ReportViewerActivity extends SherlockActivity {
    private static final String TAG = "ReportViewerActivity";
    private static final int PHOTO_REQUEST = 4214;
    ViewFlipper VF;
    ImageAdapter images;
    GridView gridViewImages;
    Uri fileUri;
    String[] step_subtitles;
    File folder;
    Toast t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_viewer);

        t = Toast.makeText(this, "Selected report not found! Please view on the server!", Toast.LENGTH_SHORT);

        Bundle bundle = getIntent().getExtras();
        String date = bundle.getString("date").replaceAll("/", "");
        String time = bundle.getString("time").replaceAll(":", "");
        String name = bundle.getString("name");

        File root = new File(getExternalFilesDir(null), "Reports");
        System.out.println(root.getPath());

        System.out.println(date +" " + time + " " + name);
        folder = new File(root, date + "_" + time + "_" + name);
        System.out.println(folder.getPath());
        if (!root.exists() || !folder.exists()) {
            System.out.println("walang ganitong report!");
            if (t.getView().isShown()) t.cancel();
            t.show();
            finish();
        }

        VF = (ViewFlipper) findViewById(R.id.reportViewer);
        getSupportActionBar().setSubtitle("Page 1 of " + VF.getChildCount());

        images = new ImageAdapter(this);
        File reportImages = new File(folder, "Pictures");
        if (reportImages.exists()) {
            System.out.println("images exist!");
            for (File image : reportImages.listFiles()) {
                Bitmap bmp = null;
                while (bmp == null) {
                    bmp = decodeSampledBitmapFromResource(image.getPath(), 100, 100);
                }
                myBitmap bmpp = new myBitmap();
                bmpp.path = image.getPath();
                bmpp.image = bmp;

                images.AddImage(bmpp);
                images.notifyDataSetChanged();
            }
        }
        else {
            System.out.println("no images!");
        }
        gridViewImages = (GridView) findViewById(R.id.grid_images);
        gridViewImages.setAdapter(images);
        gridViewImages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), FullscreenPhotoActivity.class);
                File imageFile = new File(images.getItem(position).path);
                fileUri = Uri.fromFile(imageFile);
                intent.putExtra("pos", position);
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileUri);
                intent.putExtra("view", "view");
                startActivityForResult(intent, PHOTO_REQUEST);
            }
        });

        step_subtitles = new String[]{
                "Slide Photos",
                "Summary"
        };
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int displayedchild = VF.getDisplayedChild();
        getSupportActionBar().setSubtitle(String.format("Page %d of %d - %s", displayedchild + 1, VF.getChildCount(), step_subtitles[displayedchild]));

        switch(displayedchild) {
            case 0:
                menu.findItem(R.id.action_prev).setTitle(R.string.cancel);
                menu.findItem(R.id.action_photo).setVisible(false);
                menu.findItem(R.id.action_next).setTitle(R.string.next);
                break;
            case 1:
                menu.findItem(R.id.action_prev).setTitle(R.string.back);
                menu.findItem(R.id.action_photo).setVisible(false);
                menu.findItem(R.id.action_next).setTitle(R.string.ok);
                break;
            default: break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.new_report, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println(VF.getDisplayedChild());
        switch (item.getItemId()) {
            case R.id.action_prev:
                if (VF.getDisplayedChild() == 0) {
                    finish();
                } else {
                    VF.showPrevious();
                }
                invalidateOptionsMenu();
                return true;
            case R.id.action_next:
                if(VF.getDisplayedChild() == VF.getChildCount()-1){
                    finish();
                }
                else {
                    generateSummary();
                    VF.showNext();
                }
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
            Log.d(TAG, "photo request");
            int pos = data.getIntExtra("pos", -1);

            if (pos != -1 ){
                File file = new File(images.getItem(pos).path);
                file.delete();

                images.remove(pos);
                images.notifyDataSetChanged();
            }
        } else Log.d(TAG, "wala sa cases");
    }

    @Override
    public void onBackPressed() {
        invalidateOptionsMenu();
        if (VF.getDisplayedChild() == 0) {
            finish();
        } else {
            VF.showPrevious();
        }
    }

    public static Bitmap decodeSampledBitmapFromResource(String filepath, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filepath, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    private void generateSummary() {
        ArrayList<Map<String,String>> list = getSummary();
        String[] from = {"label","value"};
        int[] to = {R.id.label, R.id.value};
        ListView lView = (ListView) findViewById(R.id.summary_report_viewer);
        //summaryAdapter adapter = new summaryAdapter(this, list);
        SimpleAdapter adapter = new SimpleAdapter(this,list,R.layout.summary_row, from, to);
        lView.setAdapter(adapter);
    }

    private ArrayList<Map<String,String>> getSummary() {
        ArrayList<Map<String,String>> list = new ArrayList<Map<String, String>>();
        File xml = new File(folder, "textData.xml");
        String[] label = new String[]{
                "Date Created",
                "Time Created",
                "Latitude",
                "Longitude",
                "Diagnosis",
                "Remarks",
                "Region",
                "Province",
                "Municipality",
                "Flags"
        };
        String[] tags = new String[]{
                "date-created",
                "time-created",
                "latitude",
                "longitude",
                "species",
                "description",
                "region",
                "province",
                "municipality",
                "flags"
        };

        if (!xml.exists()) return list;
        list.clear();

        Document doc = getDomElement(xml);
        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

        NodeList nl = doc.getElementsByTagName("entry");
        Node n = nl.item(0);
        Element eElement = (Element)n;

        for (int i=0; i<tags.length; i++) {
            list.add(putEntry(label[i], eElement.getElementsByTagName(tags[i]).item(0).getTextContent()));
        }

        return list;
    }

    private HashMap<String,String> putEntry(String label,String value){
        HashMap<String,String> line = new HashMap<String, String>();
        line.put("label",label);
        line.put("value",value);
        return line;
    }

    public Document getDomElement(File xml){
        Document doc = null;
        try {

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(xml);

        } catch (ParserConfigurationException e) {
            Log.e("Error: ", e.getMessage());
            return null;
        } catch (SAXException e) {
            Log.e("Error: ", e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e("Error: ", e.getMessage());
            return null;
        }
        // return DOM
        return doc;
    }
}

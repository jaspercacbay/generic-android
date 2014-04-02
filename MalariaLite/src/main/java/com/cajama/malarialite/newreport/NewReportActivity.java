package com.cajama.malarialite.newreport;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cajama.background.DataBaseHelper;
import com.cajama.background.SyncService;
import com.cajama.malarialite.R;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NewReportActivity extends SherlockActivity {
    AssembleData assembleData;
    ViewFlipper VF;
    GridView new_report_photos_layout;
    ImageAdapter images;
    private static final int CAMERA_REQUEST = 1888;
    private static final int PHOTO_REQUEST = 4214;
    private static final int GPS_RESULT = 477;
    private static final int SYNC_RESULT = 7962;
	private static final String TAG = "NewReportActivity";
	private Uri fileUri, imageUri;
    private String imageFilePath, required = "is a required field.";
    private Resources res;
    private String[] step_subtitles;
    ArrayList<String> entryList = new ArrayList<String>();
    ArrayList<String> accountList = new ArrayList<String>();
    ArrayList<Map<String,String>> entries = new ArrayList<Map<String, String>>();
    Toast userToast, passToast, requiredToast;
    boolean isCancelDialogOpen = false, isDeleteDialogOpen = false;
    GetLocation getLoc;
    ProgressDialog pd;
    Time today;
    Boolean submitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report_lite);

        pd = new ProgressDialog(this);
        pd.setMessage("Please wait...");
        pd.setTitle("Getting GPS Locations...");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        getLoc = new GetLocation(this);

        userToast = Toast.makeText(getApplicationContext(), "No existing user!", Toast.LENGTH_LONG);
        passToast = Toast.makeText(getApplicationContext(), "Unmatched username and password!", Toast.LENGTH_LONG);
        requiredToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);

        TextView textView = (TextView) findViewById(R.id.progressText);
        textView.setVisibility(View.INVISIBLE);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar_Horizontal);
        progressBar.setVisibility(View.INVISIBLE);



        final Spinner region = (Spinner) findViewById(R.id.region);
        final Spinner province = (Spinner) findViewById(R.id.province);
        region.setAdapter(new CustomAdapter(NewReportActivity.this, R.layout.spinner_region, getResources().getStringArray(R.array.region_array)));
        region.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String str = (String) adapterView.getItemAtPosition(i);
                int id = stringToResource(str, "array");
                System.out.println(id);
                province.setAdapter(new CustomAdapter(getApplicationContext(), R.layout.spinner_province, getResources().getStringArray(id)));
                province.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        final Spinner municipality = (Spinner) findViewById(R.id.municipality);
        province.setAdapter(new CustomAdapter(NewReportActivity.this, R.layout.spinner_province, getResources().getStringArray(R.array.Region_I_Ilocos_Region)));
        //province.setSelection(0);
        province.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String str = (String) adapterView.getItemAtPosition(i);
                if (str.equalsIgnoreCase("basilan")) str = region.getSelectedItem().toString().split(" ")[0] + "_" + str;
                int id = stringToResource(str, "array");
                municipality.setAdapter(new CustomAdapter(getApplicationContext(), R.layout.spinner_municipality, getResources().getStringArray(id)));
                municipality.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        municipality.setAdapter(new CustomAdapter(NewReportActivity.this, R.layout.spinner_municipality, getResources().getStringArray(R.array.Ilocos_Norte)));

        VF = (ViewFlipper) findViewById(R.id.viewFlipperLite);
        getSupportActionBar().setSubtitle("Step 1 of " + VF.getChildCount());

        images = new ImageAdapter(this);
        new_report_photos_layout = (GridView) findViewById(R.id.new_report_photos_layout);
        new_report_photos_layout.setEmptyView(findViewById(R.id.empty_list_view));
        new_report_photos_layout.setAdapter(images);

        new_report_photos_layout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            	Intent intent = new Intent(getApplicationContext(), FullscreenPhotoActivity.class);
                File imageFile = new File(images.getItem(position).path);
                fileUri = Uri.fromFile(imageFile);
                intent.putExtra("pos", position);
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(intent, PHOTO_REQUEST);
            }
        });

        res = getResources();
        step_subtitles = new String[]{
                res.getString(R.string.slide_photos),
                res.getString(R.string.diagnosis),
                res.getString(R.string.region),
                res.getString(R.string.summary),
                res.getString(R.string.submit)
        };

        File pics = new File(getExternalFilesDir(null), "Pictures");
        if (pics.exists()) {
            for (File f : pics.listFiles()) System.out.println(f.getName() + " " + f.delete());
            System.out.println(pics.delete());
        }
    }

    private int stringToResource(String str, String resource) {
        System.out.println("selected: " + str);
        str = str.replaceAll(" ", "_");
        str = str.replaceAll("-", "_");
        str = str.replaceAll("\\(", "");
        str = str.replaceAll("\\)", "");
        str = str.replaceAll("\\.", "");
        //str = str.replaceAll("-", "");
        System.out.println("formatted: " + str);
        return getResources().getIdentifier(str, resource, getPackageName());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int displayedchild = VF.getDisplayedChild();
        getSupportActionBar().setSubtitle(String.format("Step %d of %d - %s", displayedchild + 1, VF.getChildCount(), step_subtitles[displayedchild]));

        switch(displayedchild) {
            case 0: menu.findItem(R.id.action_prev).setTitle(R.string.cancel);
                    menu.findItem(R.id.action_photo).setVisible(true);
                    menu.findItem(R.id.action_next).setTitle(R.string.next);
                    break;
            case 1: menu.findItem(R.id.action_prev).setTitle(R.string.back);
                    menu.findItem(R.id.action_photo).setVisible(false);
                    menu.findItem(R.id.action_next).setTitle(R.string.next);
                    break;
            case 2: menu.findItem(R.id.action_prev).setTitle(R.string.back);
                    menu.findItem(R.id.action_photo).setVisible(false);
                    menu.findItem(R.id.action_next).setTitle(R.string.next);
                    break;
            case 3: menu.findItem(R.id.action_prev).setTitle(R.string.back);
                    menu.findItem(R.id.action_photo).setVisible(false);
                    menu.findItem(R.id.action_next).setTitle(R.string.next);
                    if (submitting) {
                        menu.findItem(R.id.action_prev).setEnabled(false);
                    }
                    break;
            case 4: menu.findItem(R.id.action_prev).setTitle(R.string.back);
                    menu.findItem(R.id.action_photo).setVisible(false);
                    menu.findItem(R.id.action_next).setTitle(R.string.submit);
                    if (submitting){
                        menu.findItem(R.id.action_next).setEnabled(false);
                    }
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
        removeToasts();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(VF.getWindowToken(), 0);
        System.out.println(VF.getDisplayedChild());
        switch (item.getItemId()) {
            case R.id.action_prev:
                if (VF.getDisplayedChild() == 0) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder
                            .setTitle(R.string.warning)
                            .setMessage(R.string.new_report_cancel_warning)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    for (int c=0; c<images.getCount(); c++) {
                                        File file = new File(images.getItem(c).path);
                                        file.delete();
                                    }
                                    isCancelDialogOpen = false;
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    isCancelDialogOpen = false;
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();

                    if (!isCancelDialogOpen) {
                        isCancelDialogOpen = true;
                        alertDialog.show();
                    }
                } else {
                    VF.showPrevious();
                }
                invalidateOptionsMenu();
                return true;
            case R.id.action_next:
                if(VF.getDisplayedChild() == 2){
                    if (getLoc.getLocation() != null) {
                        generateSummary();
                        //buildSummary();
                        VF.showNext();
                    }
                    else {
                        waitForResult(GPS_RESULT);
                    }
                }
                else if(VF.getDisplayedChild() != VF.getChildCount()-1) {
                    if (checkRequiredFields(VF.getDisplayedChild())) VF.showNext();
                    //return false;
                }
                else if(VF.getDisplayedChild() == VF.getChildCount()-1){
                    if (!submitting && checkRequiredFields(VF.getDisplayedChild()) && checkCredentials()) {
                        EditText user = (EditText) findViewById(R.id.username);
                        EditText pass = (EditText) findViewById(R.id.password);
                        user.setEnabled(false);
                        pass.setEnabled(false);
                        submitFinishedReport();
                    }
                }
                invalidateOptionsMenu();
                return true;
            case R.id.action_photo:
                System.out.println("asdfasdf");
            	//Intent cameraIntent = new Intent(this, Picture.class);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                //imageFilePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES) +  "/" + timeStamp + "_slide.jpg";
                imageUri = getOutputMediaFileUri();
                System.out.println("action photo: " + imageUri.getPath());

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void waitForResult(final int code) {
        pd.show();
        new Thread() {
            public void run() {
                try{
                    int timeElapsed=0;
                    while (true) {
                        timeElapsed+=3;
                        sleep(3000);
                        if (code == GPS_RESULT) {
                            if(getLoc.getLocation()!=null || timeElapsed >= 30) break;
                        }
                        else if (code == SYNC_RESULT) {
                            File temp = new File(getExternalFilesDir(null), "db.db");
                            if (temp.exists() || timeElapsed >= 30) break;
                        }
                    }
                } catch (Exception e) {
                    Log.e("tag", e.getMessage());
                }
                pd.dismiss();
                if (code == GPS_RESULT) if (getLoc.getLocation()!=null) VF.showNext();
                else if (code == SYNC_RESULT) {
                    File temp = new File(getExternalFilesDir(null), "db.db");
                    if (temp.exists()) VF.showNext();
                }
                                /*else {
                                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                                    alertDialogBuilder
                                            .setTitle("Reminder (Paalala)")
                                            .setMessage("Getting current location seems to be taking too long.\nPlease make sure that your internet connection is working.\nAlso, try to find an open area ")
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,int id) {
                                                    isCancelDialogOpen = false;
                                                }
                                            });

                                    if (!isCancelDialogOpen) {
                                        isCancelDialogOpen = true;
                                        AlertDialog alertDialog = alertDialogBuilder.create();
                                        alertDialog.show();
                                    }
                                }*/
            }
        }.start();
    }

    private boolean checkRequiredFields(int display) {
        switch (display) {
            case 1:
                EditText diagnosis = (EditText) findViewById(R.id.parasite);
                if (diagnosis.getText().toString()!=null &&  diagnosis.getText().toString().trim().length() == 0) {
                    requiredToast.setText("Diagnosis " + required);
                    requiredToast.show();
                    return false;
                }
                return true;
            case 4:
                EditText username = (EditText) findViewById(R.id.username);
                EditText password = (EditText) findViewById(R.id.password);
                File temp = new File(getExternalFilesDir(null), "db.db");
                if (username.getText().toString().trim().toLowerCase().length() == 0) {
                    requiredToast.setText("Username " + required);
                }
                else if (password.getText().toString().trim().length() == 0) {
                    requiredToast.setText("Password " + required);
                }
                else if (!temp.exists()) {
                    Intent intent = new Intent(this, SyncService.class);
                    startService(intent);
                    requiredToast.setText("Waiting to sync with server...");
                }
                else return true;
                requiredToast.show();
                return false;
            default:
                return true;
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
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

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(){
        return Uri.fromFile(getOutputMediaFile());
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/Android/data/com.cajama.malarialite/files/", "Pictures");
        System.out.println(mediaStorageDir.getPath());
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + timeStamp + "_slide.jpg");

        return mediaFile;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            String newimagepath;
        	//String newimagepath = ((Uri) data.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)).getPath();
            if (data == null) newimagepath = imageUri.getPath();
            else newimagepath = data.getData().getPath();
            Log.d(TAG, newimagepath);
            File f = new File(newimagepath);
            if(f.exists()) {
                Log.d(TAG, "newimagepath exists");

                Bitmap bmp = null;

                while(bmp == null) {
                    bmp = decodeSampledBitmapFromResource(newimagepath, 100,100);
                }
                myBitmap bmpp = new myBitmap();
                bmpp.image = bmp;
                bmpp.path = newimagepath;

                images.AddImage(bmpp);
                images.notifyDataSetChanged();
            }
        } else if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
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
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setTitle(R.string.warning)
                    .setMessage(R.string.new_report_cancel_warning)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            for (int c=0; c<images.getCount(); c++) {
                                File file = new File(images.getItem(c).path);
                                file.delete();
                            }

                            isDeleteDialogOpen = false;
                            getLoc.removeUpdates();
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            isDeleteDialogOpen = false;
                            dialog.cancel();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();

            if (!isDeleteDialogOpen) {
                isDeleteDialogOpen = true;
                alertDialog.show();
            }
        } else {
            VF.showPrevious();
        }
    }

    private String checkEmpty(String value){
        if (value.trim().length()==0) value = getString(R.string.no_input);
        return value;
    }

    private HashMap<String,String> putEntry(String label,String value){
        HashMap<String,String> line = new HashMap<String, String>();
        line.put("label",label);
        line.put("value",value);
        return line;
    }

    /*private ArrayList<Map<String,String>> buildSummary(){
        String fname, mname, lname, birthday, gender, diagnosisHuman, diagnosisNotes, photoCount, dateCreated, timeCreated, latitude,longitude;
        String caseMalaria,slideNumber, drugsGiven, examResult,age,address, region;
        //date & time
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        entries.clear();
        entryList.clear();
        //date
        dateCreated = today.format("%m/%d/%Y");
        entries.add(putEntry(getString(R.string.date_created), dateCreated));
        entryList.add(dateCreated);
        //time
        timeCreated = today.format("%H:%M:%S");
        entries.add(putEntry(getString(R.string.time_created),timeCreated));
        entryList.add(timeCreated);

        GetLocation getLoc = new GetLocation(this);

        //latitude
        latitude = getLoc.getLatitude();
        entries.add(putEntry(getString(R.string.latitude),latitude));
        entryList.add(latitude);
        //longitude
        longitude = getLoc.getLongitude();
        entries.add(putEntry(getString(R.string.longitude),longitude));
        entryList.add(longitude);

        //first name
        EditText editText1 = (EditText)findViewById(R.id.given_name_textfield);
        fname = editText1.getText().toString();
        fname = checkEmpty(fname);
        entries.add(putEntry(getString(R.string.given_name),fname));
        entryList.add(fname);

        //middle name
        EditText editText2=(EditText)findViewById(R.id.middle_name_textfield);
        mname=editText2.getText().toString();
        mname = checkEmpty(mname);
        entries.add(putEntry(getString(R.string.middle_name),mname));
        entryList.add(mname);

        //last name
        EditText editText3=(EditText)findViewById(R.id.last_name_textfield);
        lname=editText3.getText().toString();
        lname = checkEmpty(lname);
        entries.add(putEntry(getString(R.string.last_name),lname));
        entryList.add(lname);

        //birthday
        DateDisplayPicker dateDP=(DateDisplayPicker)findViewById(R.id.clientEditCreate_BirthDateDayPicker);
        birthday=dateDP.getText().toString();
        birthday = checkEmpty(birthday);
        entries.add(putEntry(getString(R.string.birthday),birthday));
        entryList.add(birthday);

        //age
        EditText editAge=(EditText)findViewById(R.id.age_textfield);
        age=editAge.getText().toString();
        age = checkEmpty(age);
        entries.add(putEntry(getString(R.string.age),age));
        entryList.add(age);

        //sex
        Spinner spinner1=(Spinner)findViewById(R.id.gender_spinner);
        gender=spinner1.getSelectedItem().toString();
        entries.add(putEntry(getString(R.string.sex),gender));
        entryList.add(gender);

        //address
        EditText editAddress=(EditText)findViewById(R.id.address);
        address=editAddress.getText().toString();
        address = checkEmpty(address);
        entries.add(putEntry(getString(R.string.address),address));
        entryList.add(address);

        //slide number
        EditText editTextSlide = (EditText) findViewById(R.id.slide_number);
        slideNumber = editTextSlide.getText().toString();
        slideNumber = checkEmpty(slideNumber);
        entries.add(putEntry(getString(R.string.slide_number),slideNumber));
        entryList.add(slideNumber);

        //region
        Spinner regionSpinner = (Spinner) findViewById(R.id.region_spinner);
        region = regionSpinner.getSelectedItem().toString();
        entries.add(putEntry(getString(R.string.region), region));
        entryList.add(region);

        //number of images
        GridView gridView1 = (GridView)findViewById(R.id.new_report_photos_layout);
        photoCount = Integer.toString(gridView1.getCount());
        //entries.add(putEntry(getString(R.string.num_photos),photoCount));

        //malaria case
        Spinner spinner3=(Spinner)findViewById(R.id.case_spinner);
        caseMalaria = spinner3.getSelectedItem().toString();
        entries.add(putEntry(getString(R.string.case_malaria),caseMalaria));
        entryList.add(caseMalaria);

        //malaria species
        Spinner spinner2=(Spinner)findViewById(R.id.species_spinner);
        diagnosisHuman=spinner2.getSelectedItem().toString();
        entries.add(putEntry(getString(R.string.diagnosis),diagnosisHuman));
        entryList.add(diagnosisHuman);

        //drugs given
        EditText editTextDrugs = (EditText)findViewById(R.id.drugs_given);
        drugsGiven = editTextDrugs.getText().toString();
        drugsGiven = checkEmpty(drugsGiven);
        entries.add(putEntry(getString(R.string.drugs_given),drugsGiven));
        entryList.add(drugsGiven);

        //exam result
        EditText editTextResult = (EditText)findViewById(R.id.exam_result);
        examResult = editTextResult.getText().toString();
        examResult = checkEmpty(examResult);
        entries.add(putEntry(getString(R.string.exam_result),examResult));
        entryList.add(examResult);

        //diagnostic notes
        EditText editText4=(EditText)findViewById(R.id.diagnostic_notes);
        diagnosisNotes =editText4.getText().toString();
        diagnosisNotes = checkEmpty(diagnosisNotes);
        entries.add(putEntry(getString(R.string.remarks),diagnosisNotes));
        entryList.add(diagnosisNotes);

        return entries;
    }*/

    private ArrayList<Map<String, String>> buildSummary() {
        String description, latitude, longitude, dateCreated, timeCreated, parasite;
        Location location = getLoc.getLocation();

        //date & time
        today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        entries.clear();
        entryList.clear();
        //date
        dateCreated = today.format("%Y/%m/%d");
        entries.add(putEntry(getString(R.string.date_created), dateCreated));
        entryList.add(dateCreated);
        //time
        timeCreated = today.format("%H:%M:%S");
        entries.add(putEntry(getString(R.string.time_created),timeCreated));
        entryList.add(timeCreated);

        //latitude
        if (location == null) {
            latitude = "";
            longitude = "";
        }
        else {
            latitude = String.valueOf(location.getLatitude());
            longitude = String.valueOf(location.getLongitude());
        }
        entries.add(putEntry(getString(R.string.latitude),latitude));
        entryList.add(latitude);

        entries.add(putEntry(getString(R.string.longitude),longitude));
        entryList.add(longitude);

        //CheckBox priority = (CheckBox) findViewById(R.id.priority);
        RadioGroup priority = (RadioGroup) findViewById(R.id.priorityRadioGroup);
        String prior = String.valueOf(((RadioButton) findViewById(priority.getCheckedRadioButtonId())).getText());
        entryList.add(prior);
        entries.add(putEntry("Priority", prior));

        //parasite
        EditText editText2 = (EditText) findViewById(R.id.parasite);
        parasite = checkEmpty(editText2.getText().toString().trim());
        entries.add(putEntry("Diagnosis", parasite));
        entryList.add(parasite);

        /*DiagnosisDataBaseHelper helper = new DiagnosisDataBaseHelper(this);
        try {
            helper.createDataBase();
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, String> hash = new HashMap<String, String>();
        hash.put("diagnosis", parasite);
        helper.insert(hash);

        hash = helper.getDiagnosis();
        if (hash != null) {
            for (Object str : hash.values().toArray()) {
                System.out.println(str.toString());
            }
        }*/

        //description
        EditText editText = (EditText) findViewById(R.id.description);
        description = checkEmpty(editText.getText().toString());
        entries.add(putEntry("Remarks", description));
        entryList.add(description);

        Spinner region = (Spinner) findViewById(R.id.region);
        String reg = checkEmpty(region.getSelectedItem().toString());
        entries.add(putEntry("Region", reg));
        entryList.add(reg);

        Spinner province = (Spinner) findViewById(R.id.province);
        String prov = checkEmpty(province.getSelectedItem().toString());
        entries.add(putEntry("Province", prov));
        entryList.add(prov);

        Spinner municipality = (Spinner) findViewById(R.id.municipality);
        String munic = checkEmpty(municipality.getSelectedItem().toString());
        entries.add(putEntry("Municipality", munic));
        entryList.add(munic);

        boolean testFlag = false;//PreferenceManager.getDefaultSharedPreferences(this).getBoolean("test_data_flag", false);
        entryList.add(String.valueOf(testFlag));

        return entries;
    }

    private void generateSummary() {
        ArrayList<Map<String,String>> list = buildSummary();
        String[] from = {"label","value"};
        int[] to = {R.id.label, R.id.value};
        ListView lView = (ListView) findViewById(R.id.summaryLabels);
        //summaryAdapter adapter = new summaryAdapter(this, list);
        SimpleAdapter adapter = new SimpleAdapter(this,list,R.layout.summary_row, from, to);
        lView.setAdapter(adapter);
    }

    private String getAccountData(){
        String USERNAME, PASSWORD;
        EditText editText1=(EditText )findViewById(R.id.username);
        EditText editText2=(EditText )findViewById(R.id.password);
        USERNAME           =editText1.getText().toString().trim();
        PASSWORD           =editText2.getText().toString().trim();
        USERNAME = USERNAME.toLowerCase();
        Log.v("write","USERNAME: " + USERNAME + " PASSWORD: " + PASSWORD);
        accountList.add(USERNAME);
        accountList.add(PASSWORD);
        Log.v("write","stuff: " + accountList.get(0) + accountList.get(1));

        return USERNAME;
    }

    private void submitFinishedReport() {

        submitting = true;
        Log.d(TAG, "submitting!");

        ArrayList<String> imageList = new ArrayList<String>();
        for (int i=0; i < images.getCount();i++ ) imageList.add(i,images.getItem(i).path);

        String USERNAME = getAccountData();

        assembleData = new AssembleData(getApplicationContext(),entryList,imageList,accountList,USERNAME, today);
        TextView textView = (TextView) findViewById(R.id.progressText);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar_Horizontal);
        assembleData.setView(progressBar, textView);
        textView.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        Thread assembleDataThread = new Thread(myThread);
        assembleDataThread.start();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("finish") !=  null && intent.getStringExtra("finish").equals("finish")) {
                getLoc.removeUpdates();
                finish();
            }
            else if (intent.getStringExtra("update") !=  null && intent.getStringExtra("update").equals("update")) {
                generateSummary();
                getLoc.removeUpdates();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getLoc.checkGps();
        registerReceiver(broadcastReceiver, new IntentFilter(AssembleData.BROADCAST_FINISH));
        registerReceiver(broadcastReceiver, new IntentFilter(GetLocation.BROADCAST_ACTION_LOCATION));
    }

    @Override
    public void onPause() {
        super.onPause();
        getLoc.removeUpdates();
        unregisterReceiver(broadcastReceiver);
    }

    private Runnable myThread = new Runnable(){

        @Override
        public void run()
        {
            try {
                assembleData.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Intent sendService = new Intent("com.cajama.background.FinalSendingService");
            sendService.putExtra("send", "send");
            sendBroadcast(sendService);
        }
    };

    private boolean checkCredentials() {
    	byte[] passBytes;
    	String USERNAME, PASSWORD;

        EditText editText1=(EditText )findViewById(R.id.username);
        EditText editText2=(EditText )findViewById(R.id.password);
        USERNAME           =editText1.getText().toString().trim();
        PASSWORD           =editText2.getText().toString().trim();

        USERNAME = USERNAME.toLowerCase();

        DataBaseHelper helper = new DataBaseHelper(this);
        helper.openDataBase();

        Cursor cursor = helper.getPair(USERNAME);
        
        try {
			passBytes = PASSWORD.getBytes("UTF-8");
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
            passBytes = sha.digest(passBytes);
            Log.d(TAG+" checkCredentials()", USERNAME);
            
        	if (cursor == null) {
        		//Toast.makeText(getApplicationContext(), "No existing user!", Toast.LENGTH_LONG).show();
                userToast.show();
        		return false;
        	}
        	
        	cursor.moveToFirst();
            byte[] test = cursor.getString(1).getBytes("UTF-8");
            MessageDigest s = MessageDigest.getInstance("SHA-1");
            test = s.digest(test);
            String f = byteArrayToHexString(test);

            Log.d(TAG, cursor.getString(1));
            Log.d(TAG, f);

            String temp = byteArrayToHexString(passBytes);
            Log.d(TAG, temp);

        	if (!cursor.getString(1).equals(temp)) {
        		//Toast.makeText(getApplicationContext(), "Unmatched username and password!", Toast.LENGTH_LONG).show();
                passToast.show();
        		return false;
        	}
        	
        	Log.d(TAG+" checkCredentials()", "login success!");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
            if (cursor != null) cursor.close();
            helper.close();
        }
        
    	return true;
    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result +=
                    Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
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

    private void removeToasts() {
        if (requiredToast.getView().isShown()) requiredToast.cancel();
        if (userToast.getView().isShown()) userToast.cancel();
        if (passToast.getView().isShown()) passToast.cancel();
    }
}

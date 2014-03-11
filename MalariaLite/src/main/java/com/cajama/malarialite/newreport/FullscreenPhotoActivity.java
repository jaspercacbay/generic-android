package com.cajama.malarialite.newreport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cajama.malarialite.R;

public class FullscreenPhotoActivity extends SherlockActivity {
    int pos;
    private String path;
    boolean isDeleteDialogOpen = false;
    private boolean viewOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_fullscreen_photo);
        Intent intent = getIntent();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();

        try {
            display.getSize(size);
        } catch (java.lang.NoSuchMethodError ignore) { // Older device
            size.x = display.getWidth();
            size.y = display.getHeight();
        }

        int width = size.x;
        int height = size.y;

        pos = intent.getIntExtra("pos", -1);
        path = null;
        if (((Uri) intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)) != null) {
            path = ((Uri) intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)).getPath();
        }

        if (path != null) {
            ImageView image = (ImageView) findViewById(R.id.fullscreen_imageView);
            image.setImageBitmap(decodeSampledBitmapFromResource(path, width, height));
        }

        if (intent.getStringExtra("view") != null) viewOnly = true;
        else viewOnly = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.fullscreen_photo, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (viewOnly) menu.findItem(R.id.action_delete_photo).setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_photo:
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder
                        .setTitle(R.string.warning)
                        .setMessage(R.string.photo_delete_warning)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent resultIntent = new Intent(getApplicationContext(), NewReportActivity.class);
                                resultIntent.putExtra("pos", pos);
                                //resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                setResult(RESULT_OK, resultIntent);
                                isDeleteDialogOpen = false;
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                isDeleteDialogOpen = false;
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();

                if (!isDeleteDialogOpen) {
                    isDeleteDialogOpen = true;
                    alertDialog.show();
                }

                return true;
            case android.R.id.home:
                Intent resultIntent = new Intent(getApplicationContext(), NewReportActivity.class);
                resultIntent.putExtra("pos", -1);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
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

    public static Bitmap decodeSampledBitmapFromResource(String filepath,
                                                         int reqWidth, int reqHeight) {

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
}
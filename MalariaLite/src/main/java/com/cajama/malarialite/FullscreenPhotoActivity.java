package com.cajama.malarialite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cajama.malarialite.newreport.NewReportActivity;

// to remove (copy exists in newreport module)

public class FullscreenPhotoActivity extends SherlockActivity {
    int pos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_fullscreen_photo);
        Intent intent = getIntent();

        pos = intent.getIntExtra("pos", -1);

        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();

        Bitmap bmp = BitmapFactory.decodeFile(intent.getStringExtra("path"), bmpFactoryOptions);

        ImageView image = (ImageView) findViewById(R.id.fullscreen_imageView);
        image.setImageBitmap(bmp);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.fullscreen_photo, menu);
        return true;
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
                            public void onClick(DialogInterface dialog,int id) {
                                Intent resultIntent = new Intent(getApplicationContext(), NewReportActivity.class);
                                resultIntent.putExtra("pos", pos);
                                resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();

                alertDialog.show();

                return true;
            case android.R.id.home:
                Intent resultIntent = new Intent(getApplicationContext(), NewReportActivity.class);
                resultIntent.putExtra("pos", -1);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                setResult(RESULT_OK, resultIntent);
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
}

package com.cajama.malarialite.newreport;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.cajama.malarialite.R;
import java.io.File;
import java.io.FileOutputStream;
public class Picture extends SherlockActivity {
    private SurfaceView preview=null;
    private SurfaceHolder previewHolder=null;
    private Camera camera=null;
    private boolean inPreview=false;
    private boolean cameraConfigured=false;
    private String path;
    private TextView countdown;
    final Handler handler = new Handler();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        preview=(SurfaceView)findViewById(R.id.preview);
        previewHolder=preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        Intent intent = getIntent();
        path = ((Uri) intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)).getPath();
        countdown = (TextView) findViewById(R.id.photocountdown);
        countdown.setVisibility(View.INVISIBLE);
    }
    @Override
    public void onResume() {
        super.onResume();
        getSupportActionBar().setBackgroundDrawable(null);
        if (camera == null) {
            camera=Camera.open();
        }
        setCameraDisplayOrientation(this, 0, camera);
        Camera.Parameters parameters=camera.getParameters();
        Camera.Size pictureSize= getBiggestPictureSize(parameters);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        parameters.setJpegQuality(100);
        parameters.setPictureFormat(ImageFormat.JPEG);
        camera.setParameters(parameters);
        initPreview(preview.getWidth(), preview.getHeight());
        startPreview();
        preview.setVisibility(View.VISIBLE);
    }
    @Override
    public void onPause() {
        if (inPreview) {
            camera.stopPreview();
        }
        camera.release();
        camera=null;
        inPreview=false;
        super.onPause();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.picture, menu);
        return(super.onCreateOptionsMenu(menu));
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.camera) {
            if (inPreview) {
                inPreview=false;
                countdown.setVisibility(View.VISIBLE);
                countdown.setText("3");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms
                        countdown.setText("2");
                    }
                }, 1000);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms
                        countdown.setText("1");
                    }
                }, 2000);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms
                        countdown.setVisibility(View.INVISIBLE);
                    }
                }, 3000);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        camera.autoFocus(new Camera.AutoFocusCallback(){
                            @Override
                            public void onAutoFocus(boolean arg0, Camera arg1) {
                                camera.takePicture(null, null, photoCallback);
                            }
                        });
                    }
                }, 3000);
            }
        }
        return(super.onOptionsItemSelected(item));
    }
    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result=size;
                }
                else {
                    int resultArea=result.width * result.height;
                    int newArea=size.width * size.height;
                    if (newArea > resultArea) {
                        result=size;
                    }
                }
            }
        }
        return(result);
    }
    private Camera.Size getBiggestPictureSize(Camera.Parameters parameters) {
        Camera.Size result=null;
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result=size;
            }
            else {
                int resultArea=result.width * result.height;
                int newArea=size.width * size.height;
                if (newArea > resultArea) {
                    result=size;
                }
            }
        }
        return(result);
    }
    public void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        Camera.Parameters parameters=camera.getParameters();
        if (Build.VERSION.SDK_INT >= 9) {
            android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            int rotation = activity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }
            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            parameters.setRotation(result);
            camera.setParameters(parameters);
            camera.setDisplayOrientation(result);
        } else {
            Configuration cfg = this.getResources().getConfiguration();
            if (cfg.orientation == Configuration.ORIENTATION_PORTRAIT) {
                camera.setDisplayOrientation(90);
            }
        }
    }
    private void initPreview(int width, int height) {
        if (camera != null && previewHolder.getSurface() != null) {
            try {
                camera.setPreviewDisplay(previewHolder);
                setCameraDisplayOrientation(this, 0, camera);
            }
            catch (Throwable t) {
                Log.e("PreviewDemo-surfaceCallback",
                        "Exception in setPreviewDisplay()", t);
                Toast.makeText(Picture.this, t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            Camera.Parameters parameters=camera.getParameters();
            Camera.Size size = getBestPreviewSize(width, height, parameters);
            Camera.Size pictureSize= getBiggestPictureSize(parameters);
            if (size != null && pictureSize != null) {
                parameters.setPreviewSize(size.width, size.height);
                parameters.setPictureSize(pictureSize.width, pictureSize.height);
                parameters.setJpegQuality(100);
                parameters.setPictureFormat(ImageFormat.JPEG);
                camera.setParameters(parameters);
                cameraConfigured=true;
            }
        }
    }
    private void startPreview() {
        if (cameraConfigured && camera != null) {
            setCameraDisplayOrientation(this, 0, camera);
            camera.startPreview();
            inPreview=true;
        }
    }
    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }
        public void surfaceChanged(SurfaceHolder holder, int format,
                                   int width, int height) {
            countdown.setVisibility(View.INVISIBLE);
            initPreview(width, height);
            startPreview();
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    };
    Camera.PictureCallback photoCallback=new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePhotoTask().execute(data);
            //camera.startPreview();
            //inPreview=true;
            Intent resultIntent = new Intent(getApplicationContext(), NewReportActivity.class);
            File imageFile = new File(path);
            Uri fileUri = Uri.fromFile(imageFile);
            resultIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileUri);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    };
    class SavePhotoTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... jpeg) {
            File photo= new File(path);
            Log.d("PictureDemo", path);
            if (photo.exists()) {
                Log.d("PictureDemo", "photo exists");
                photo.delete();
            }
            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());
                fos.write(jpeg[0], 0, jpeg[0].length);
                fos.close();
                Log.d("PictureDemo", String.valueOf(jpeg[0].length));
            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }
            return(null);
        }
    }
}
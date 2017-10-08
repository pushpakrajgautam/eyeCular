package com.enem.www.enem;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Landmark;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    private String eth = "Asian";
    private String gend = "Male";

    private SensorManager mSensorManager;
    private Sensor mProximity;
    private static final int SENSOR_SENSITIVITY = 4;

    private boolean done;
    private double optimalDist;
    public final static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034;
    File destination;

    private Vision vision;

    public void onLaunchCamera()
    {
        // create Intent to take a picture and return control to the calling application
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String name = format.format(date);
        destination = new File(Environment.getExternalStorageDirectory(), name + ".jpg");

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(destination));
        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        if (intent.resolveActivity(getPackageManager()) != null)
        {
            // Start the image capture intent to take photo
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK)
            {
                finalizeReoprt();
            }
            else
            { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Returns the Uri for a photo stored on disk given the fileName
    public void finalizeReoprt()
    {
        // Only continue if the SD Card is mounted
        final Bitmap myBitmap = BitmapFactory.decodeFile(destination.getAbsolutePath());
        ImageView myImage = (ImageView) findViewById(R.id.ivti);
        myImage.setImageBitmap(myBitmap);
        if (isExternalStorageAvailable())
        {
            AsyncTask.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Bitmap bitmap = Bitmap.createScaledBitmap(myBitmap,1600,1200,true);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 0 , bos);
                        byte[] bitmapdata = bos.toByteArray();
                        Image inputImage = new Image();
                        inputImage.encodeContent(bitmapdata);
                        Feature desiredFeature = new Feature();
                        desiredFeature.setType("FACE_DETECTION");
                        AnnotateImageRequest request = new AnnotateImageRequest();
                        request.setImage(inputImage);
                        request.setFeatures(Arrays.asList(desiredFeature));
                        BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
                        batchRequest.setRequests(Arrays.asList(request));
                        BatchAnnotateImagesResponse batchResponse = vision.images().annotate(batchRequest).execute();
                        List<AnnotateImageResponse> responses = batchResponse.getResponses();

                        double a = 0.0;
                        double b = 0.0;
                        double e = 0.0;
                        double f = 0.0;
                        double x = 0.0;
                        double y = 0.0;
                        double z = 0.0;
                        double w = 0.0;
                        for (AnnotateImageResponse res : responses)
                        {
                            // For full list of available annotations, see http://g.co/cloud/vision/docs
                            for (FaceAnnotation annotation : res.getFaceAnnotations())
                            {
                                for(Landmark lm : annotation.getLandmarks())
                                {
                                    if(lm.getType().equals("LEFT_EYE_LEFT_CORNER"))
                                    {
                                        a = lm.getPosition().getX();
                                    }
                                    else if(lm.getType().equals("LEFT_EYE_RIGHT_CORNER"))
                                    {
                                        b = lm.getPosition().getX();
                                    }
                                    else if(lm.getType().equals("LEFT_EYE_TOP_BOUNDARY"))
                                    {
                                        e = lm.getPosition().getY();
                                    }
                                    else if(lm.getType().equals("LEFT_EYE_BOTTOM_BONDARY"))
                                    {
                                        f = lm.getPosition().getY();
                                    }
                                    else if(lm.getType().equals("RIGHT_EYE_LEFT_CORNER"))
                                    {
                                        x = lm.getPosition().getX();
                                    }
                                    else if(lm.getType().equals("RIGHT_EYE_RIGHT_CORNER"))
                                    {
                                        y = lm.getPosition().getX();
                                    }
                                    else if(lm.getType().equals("RIGHT_EYE_TOP_BOUNDARY"))
                                    {
                                        z = lm.getPosition().getY();
                                    }
                                    else if(lm.getType().equals("RIGHT_EYE_BOTTOM_BONDARY"))
                                    {
                                        w = lm.getPosition().getY();
                                    }
                                }
                            }
                        }

                        final double leftx = (b-a)/4.12673887;
                        final double lefty = (e-f)/18.82730785;
                        final double rightx = (y-x)/3.79296421019;
                        final double righty = (z-w)/18.6873506313;

                        final double left = (59580.4806045 * 0.75)/(leftx * lefty * Math.PI*6.42) - 3.472;
                        final double right = (59580.4806045 * 0.75)/(rightx * righty * Math.PI*6.42) - 3.472;

                        final double powerLeft = ((left/0.0782) - 5)/10;
                        final double powerRight = ((right/0.0782) - 5)/10;
                        System.out.println("How can you be twenty-four?");

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(MainActivity.this,"Right: " + powerRight + " Left: " + powerLeft,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    // Returns true if external storage for photos is available
    private boolean isExternalStorageAvailable()
    {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE))
            {

            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},42);
            }
        }

        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(), new AndroidJsonFactory(), null);

        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyATGi-ZfZEtEVqmw-XGc7zVygrbbz-SANw"));
        vision = visionBuilder.build();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        Button submit = (Button) findViewById(R.id.sub);
        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                eth = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
                eth = "Asian";
            }
        });
        spinner = (Spinner) findViewById(R.id.spinner2);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                gend = (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
                gend = "Male";
            }
        });
        submit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                done = true;
                onLaunchCamera();
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY)
            {
                if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY)
                {
                    optimalDist = event.values[0];
                    Toast.makeText(getApplicationContext(), optimalDist + "", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    optimalDist = event.values[0];
                    Toast.makeText(getApplicationContext(), optimalDist + "", Toast.LENGTH_SHORT).show();
                }
            }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case 42:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                }
                else
                {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}

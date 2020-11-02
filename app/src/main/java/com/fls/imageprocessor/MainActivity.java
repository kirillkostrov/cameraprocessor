package com.fls.imageprocessor;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "FLSImageProcessor";
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private String cameraId;
    CameraCaptureSession cameraCaptureSession;
    TextureView cameraPreview;
    private EditText valueX;
    private EditText valueY;
    private EditText valueZ;


//    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        valueX = findViewById(R.id.inputNumber1);
        valueX.setText(getIntPreference("X").toString());
        valueX.addTextChangedListener(onChangeListener);

        valueY = findViewById(R.id.inputNumber2);
        valueY.setText(getIntPreference("Y").toString());
        valueY.addTextChangedListener(onChangeListener);

        valueZ = findViewById(R.id.inputNumber3);
        valueZ.setText(getIntPreference("Z").toString());
        valueZ.addTextChangedListener(onChangeListener);

        cameraPreview = findViewById(R.id.textureView);
        cameraPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                checkPermission();
                cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                startCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
    }


    private void startCamera() {
        try {
            cameraId = getCameraName();
            if (cameraId == null) {
                Toast.makeText(this, "No camera found.", Toast.LENGTH_LONG).show();
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, cameraStateCallback, null);
            } else {
                Toast.makeText(this, "No camera permission.", Toast.LENGTH_LONG).show();
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 10) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            }
        }
    }

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            startCameraPreview();
            Log.i(TAG, "camera opened: " + cameraDevice.getId());
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
            Log.i(TAG, "camera disconnected: " + cameraDevice.getId());
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.i(TAG, "camera failed: " + camera.getId() + " error: " + error);
        }
    };

    private void startCameraPreview() {

        SurfaceTexture texture = cameraPreview.getSurfaceTexture();
        texture.setDefaultBufferSize(1920,1080);
        Surface surface = new Surface(texture);

        try {
            final CaptureRequest.Builder builder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            builder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            try {
                                cameraCaptureSession.setRepeatingRequest(builder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    private String getCameraName() throws CameraAccessException {
        /* we will try to get first back-facing camera, if none we return first available */
        String[] cameras = cameraManager.getCameraIdList();

        for (String cameraID : cameras) {
            if (cameraManager.getCameraCharacteristics(cameraID).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK)
                return cameraID;
        }

        return cameras.length > 0 ? cameras[0] : null;
    }

    private void checkPermission() {
        String[] deniedPermission = PermissionUtil.getDeniedPermissions(this, new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        });
        if (deniedPermission.length > 0) {
            PermissionUtil.requestPermissions(this, deniedPermission, 10);
        }
    }

    public void onProcessClick(View view) {
        Bitmap resultBitmap = cameraPreview.getBitmap();

        int x = 0;
        int y = 0;
        int z = 0;

        try {
            x = Integer.parseInt(valueX.getText().toString());
            y = Integer.parseInt(valueY.getText().toString());
            z = Integer.parseInt(valueZ.getText().toString());

        } catch (NumberFormatException e) {
            showWarnDialog("All three parameters Z, Y and Z must be entered");
            return;
        }

        Logic.Run(resultBitmap, x, y, z, new LogicResultCallback() {
            @Override
            public void OnResult(LogicResult result) {
                String output = String.format(getString(R.string.resut_toast_text), result.result, result.probability);
                Toast.makeText(MainActivity.this, output, Toast.LENGTH_SHORT).show();
            };
        });
    }
    private void showWarnDialog(String content) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.warn);
        builder.setIcon(R.drawable.ic_warn);
        builder.setMessage(content);
        builder.setNegativeButton(R.string.confirm, null);
        builder.show();
    }

    private TextWatcher onChangeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            saveAll();
        }
    };

    private void saveAll() {
        Integer valX = tryParseInt(valueX.getText().toString());
        if (valX != null) setIntPreference("X", valX);
        Integer valY = tryParseInt(valueY.getText().toString());
        if (valY != null) setIntPreference("Y", valY);
        Integer valZ = tryParseInt(valueZ.getText().toString());
        if (valZ != null) setIntPreference("Z", valZ);
    }

    private Integer getIntPreference(String name) {
        SharedPreferences prefs = getSharedPreferences(TAG, 0);
        return prefs.getInt(name, 0);
    }

    private void setIntPreference(String name, int value) {
        SharedPreferences.Editor editor = getSharedPreferences(TAG, 0).edit();
        editor.putInt(name, value);
        editor.commit();
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
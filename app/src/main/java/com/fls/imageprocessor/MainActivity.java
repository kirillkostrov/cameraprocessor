package com.fls.imageprocessor;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FLSImageProcessor";
    private static final int CAMERA_REQUEST = 1111;
    ImageView resultView;

    private Bitmap capturedBitmap;
    String currentPhotoPath;
    private EditText valueX;
    private EditText valueY;
    private EditText valueZ;

    private ProgressDialog progressDialog;
    private Handler taskHandler;
    private Runnable processTask;

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

        resultView = findViewById(R.id.textureView);

        checkPermission();
    }

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            try {
                capturedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(new File(currentPhotoPath)));
                processImage(capturedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 10) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
    }

    private void checkPermission() {
        String[] deniedPermission = PermissionUtil.getDeniedPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        });
        if (deniedPermission.length > 0) {
            PermissionUtil.requestPermissions(this, deniedPermission, 10);
        }
    }

    public void onProcessClick(View view) {
        dispatchTakePictureIntent();
    }

    private void processImage(Bitmap resultBitmap) {

        int x = 0;
        int y = 0;
        int z = 0;

        try {
            x = Integer.parseInt(valueX.getText().toString());
            y = Integer.parseInt(valueY.getText().toString());
            z = Integer.parseInt(valueZ.getText().toString());

        } catch (NumberFormatException e) {
            showWarnDialog();
            return;
        }

        showProgressDialog();

        taskHandler = new Handler(Looper.myLooper());

        int finalX = x;
        int finalY = y;
        int finalZ = z;

        processTask = () -> Logic.Run(resultBitmap, finalX, finalY, finalZ, result -> {
            String output = String.format(getString(R.string.resut_toast_text), result.result, result.probability);
            if (result.bitmap != null) {
                resultView.setImageBitmap(result.bitmap);
            }
            else {
                resultView.setImageResource(R.drawable.ic_noimage_foreground);
            }
            hideProgressDialog();
            Toast.makeText(MainActivity.this, output, Toast.LENGTH_LONG).show();
        });

        //TODO: Don't forget to remove delay after debug
        taskHandler.postDelayed(processTask, 0);
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.processing_dialog_title));
        progressDialog.setMessage(getString(R.string.processing_dialog_message));

        progressDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (taskHandler != null && processTask != null) {
                    taskHandler.removeCallbacks(processTask);
                }
            }
        });
        progressDialog.show();
    }

    private void hideProgressDialog() {
        progressDialog.dismiss();
    }

    private void showWarnDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.warn);
        builder.setIcon(R.drawable.ic_warn);
        builder.setMessage(R.string.parameters_validation_failed);
        builder.setNegativeButton(R.string.confirm, null);
        builder.show();
    }

    private final TextWatcher onChangeListener = new TextWatcher() {
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
        editor.apply();
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
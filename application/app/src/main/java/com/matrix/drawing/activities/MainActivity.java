package com.matrix.drawing.activities;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.matrix.drawing.R;
import com.matrix.drawing.data.ImageUploadInfo;
import com.matrix.drawing.views.PaintView;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MainActivity extends AppCompatActivity {

    private PaintView paint;
    private RangeSlider rangeSlider;
    private Uri uri;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        paint = findViewById(R.id.paint_view);
        rangeSlider = findViewById(R.id.rangebar);
        ImageButton undo = findViewById(R.id.btn_undo);
        ImageButton save = findViewById(R.id.btn_save);
        ImageButton redo = findViewById(R.id.btn_redo);
        ImageButton stroke = findViewById(R.id.btn_brush);
        ImageButton color = findViewById(R.id.btn_color);
        ImageButton emoji = findViewById(R.id.btn_emoji);
        ImageButton eraser = findViewById(R.id.btn_eraser);


        undo.setOnClickListener(view -> paint.undo());
        redo.setOnClickListener(view -> paint.redo());

        eraser.setOnClickListener(view -> {
            paint.eraser();
        });

        emoji.setOnClickListener(view -> {
            Toast.makeText(getApplicationContext(), "Under Progress!", Toast.LENGTH_LONG).show();
        });

        save.setOnClickListener(view -> {
            Bitmap bmp = paint.save();
            OutputStream imageOutStream = null;
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "drawing.png");
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            try {
                imageOutStream = getContentResolver().openOutputStream(uri);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, imageOutStream);
                imageOutStream.close();
                Toast.makeText(getApplicationContext(), "Saved to gallery!", Toast.LENGTH_LONG).show();

                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Save to Cloud?")
                        .setMessage("Do you want to save the image to cloud?")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            uploadFile(uri);
                        })
                        .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        })
                        .show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        color.setOnClickListener(view -> {
            final ColorPicker colorPicker = new ColorPicker(MainActivity.this);
            colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                @Override
                public void setOnFastChooseColorListener(int position, int color1) {
                    paint.setColor(color1);
                }

                @Override
                public void onCancel() {
                    colorPicker.dismissDialog();
                }
            })
                    .setColumns(5)
                    .setDefaultColorButton(Color.parseColor("#000000"))
                    .show();
        });

        stroke.setOnClickListener(view -> {
            paint.restoreColor();
            if (rangeSlider.getVisibility() == View.VISIBLE)
                rangeSlider.setVisibility(View.GONE);
            else
                rangeSlider.setVisibility(View.VISIBLE);
        });

        rangeSlider.setValueFrom(0.0f);
        rangeSlider.setValueTo(100.0f);

        rangeSlider.addOnChangeListener((slider, value, fromUser) -> paint.setStrokeWidth((int) value));

        ViewTreeObserver vto = paint.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                paint.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = paint.getMeasuredWidth();
                int height = paint.getMeasuredHeight();
                paint.init(height, width);
            }
        });
    }

    private void uploadFile(Uri uri) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading");
        progressDialog.show();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        StorageReference riversRef = storage.getReference().child("images").child("Paint_" + System.currentTimeMillis() / 1000 + ".png");

        riversRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                    riversRef.getDownloadUrl().addOnSuccessListener(downloadPhotoUrl -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("Paint_" + System.currentTimeMillis() / 1000 + ".png", downloadPhotoUrl.toString());

                        db.collection("image-link").document("all-images")
                                .set(map, SetOptions.merge())
                                .addOnSuccessListener(documentReference ->
                                        Toast.makeText(MainActivity.this, "Added to database.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(MainActivity.this, "Failed to add to database. \n" + e, Toast.LENGTH_SHORT).show());
                    });
                })
                .addOnFailureListener(exception -> {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                    progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getApplicationContext(), "logged out", Toast.LENGTH_LONG).show();
                Intent i = new Intent(MainActivity.this,
                        LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
                return true;
            case R.id.allImages:
                Intent intent = new Intent(MainActivity.this,
                        DisplayImages.class);
                startActivity(intent);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
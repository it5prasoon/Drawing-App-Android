package com.matrix.drawing.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.matrix.drawing.R;
import com.matrix.drawing.RecyclerViewAdapter;
import com.matrix.drawing.data.ImageUploadInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DisplayImages extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.Adapter adapter ;
    ProgressDialog progressDialog;
    List<String> list = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_images);
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.allImg);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Images From Firebase.");
        progressDialog.show();

        DocumentReference docRef = db.collection("image-link").document("all-images");

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot != null) {
                Map<String, Object> map;
                map = documentSnapshot.getData();
                assert map != null;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String imageUploadInfo = (String) entry.getValue();
                    list.add(imageUploadInfo);
                    Log.e("Lists", list.toString());
                    adapter = new RecyclerViewAdapter(getApplicationContext(), list);
                    recyclerView.setAdapter(adapter);
                    progressDialog.dismiss();
                }
            } else {
                Log.d(ContentValues.TAG, "No such document");
            }
        });


    }
}
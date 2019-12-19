package com.example.webservisimage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Color;


import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    private FirebaseFunctions mFunctions;;
    private StorageReference mStorageRef;
    private Uri selectedImage;
    ImageView img;
    ImageView resultImg;
    EditText editTextRate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

    }

    private void init() {
        img = findViewById(R.id.imageView2);
        resultImg=findViewById(R.id.imageView1);
        mFunctions = FirebaseFunctions.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        editTextRate=findViewById(R.id.edittext_rate);
    }

    public void Compress(View view){
        if (selectedImage!=null){
            if (!TextUtils.isEmpty(editTextRate.getText())){
                Toast.makeText(this, "Resim Sunucuda Compress edilicek", Toast.LENGTH_LONG).show();
                final int x=100-((Integer.parseInt(editTextRate.getText().toString()))%100);
                addFirebaseFile(selectedImage,1,x);
            }else Toast.makeText(this, "Sıkıştırma oranı gir", Toast.LENGTH_SHORT).show();
        }else Toast.makeText(this, "Bir resim seçilmedi", Toast.LENGTH_SHORT).show();

    }

    public void Segmantation(View view){
        if (selectedImage!=null){
                addFirebaseFile(selectedImage,2,0);
        }else Toast.makeText(this, "Bir resim seçilmedi", Toast.LENGTH_SHORT).show();
    }

    public void ImageClick(View view){
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]
                    {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        } else ShowDialog();
    }

    private void ShowDialog() {
        AlertDialog.Builder myDialog = new AlertDialog.Builder(MainActivity.this);
        myDialog.setTitle("Resim Yükle");
        myDialog.setMessage("Nasıl yükleme yapmak istersiniz");
        myDialog.setPositiveButton("GALERİ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,2);
            }
        });
        myDialog.setNegativeButton("KAMERA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE,"Yeni Foto");
                values.put(MediaStore.Images.Media.DESCRIPTION,"Kamera");
                selectedImage=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,selectedImage);
                startActivityForResult(cameraIntent, 3);
            }
        });
        myDialog.show();
    }

    private void addFirebaseFile(Uri selectedImage, final int operation ,final int compRate)  {
        UUID uuıd = UUID.randomUUID();
        final String imageName = "images/"+uuıd+".jpg";
        StorageReference storageReference = mStorageRef.child(imageName);
        storageReference. putFile(selectedImage)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            if (operation==1){
                                ImageCompress(imageName,compRate).addOnCompleteListener(new OnCompleteListener<String>() {
                                    @Override
                                    public void onComplete(@NonNull Task<String> task) {
                                        if (task.isSuccessful()){
                                            Picasso.get().load(task.getResult()).into(resultImg);
                                            System.out.println("beyter 5 "+task.getResult());
                                        }
                                    }
                                });
                            }
                            else if (operation==2){
                                ImageSegmantation(imageName,String.valueOf(imageName)).addOnCompleteListener(new OnCompleteListener<String>() {
                                    @Override
                                    public void onComplete(@NonNull Task<String> task) {
                                        if (task.isSuccessful()){
                                            System.out.println("beyter 2"+ task.getResult());
                                        }
                                    }
                                });
                            }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("beyter "+e.getMessage());
            }
        });
    }


    private Task<String> ImageCompress(String pathName, int compRate) {
        Map<String, Object> data = new HashMap<>();
        data.put("compRate",compRate);
        data.put("path_name",pathName);
        return mFunctions.getHttpsCallable("ImageCompress").call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        return String.valueOf(result.get("result")) ;
                    }
                });
    }

    private Task<String> ImageSegmantation(String pathName,String uri){
        Map<String, Object> data = new HashMap<>();
        data.put("dowloandUrl", uri);
        data.put("path_name",pathName);
        return mFunctions .getHttpsCallable("ImageSegmantation").call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
            @Override
            public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                return String.valueOf(result.get("result")) ;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==1 && grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
            ShowDialog();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK ){
            if (requestCode == 2 && data != null ) {
                selectedImage = data.getData();
                img.setBackgroundColor(Color.rgb(255,255,255));
                img.setImageURI(selectedImage);
            }else if(requestCode==3){
                img.setImageURI(selectedImage);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}

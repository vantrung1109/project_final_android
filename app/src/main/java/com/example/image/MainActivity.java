package com.example.image;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_IMAGE_PICK = 1;
    private ArrayList<Image> arrayList;
    private RecyclerView.Adapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arrayList = new ArrayList<>();
        FirebaseApp.initializeApp(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = findViewById(R.id.recycler);
        adapter = new ImageAdapter(this, arrayList); // Khởi tạo biến adapter
        recyclerView.setAdapter(adapter);
        FloatingActionButton fabAddImage = findViewById(R.id.fab_add_image);
        fabAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK);
            }
        });
//        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
//
//            @Override
//            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
//                return false;
//            }
//
//            @Override
//            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
//                int position = viewHolder.getAdapterPosition();
//                arrayList.remove(position);
//                adapter.notifyItemRemoved(position);
//            }
//
//            @Override
//            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
//                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
//                    // Cast the ViewHolder to your custom ViewHolder to access your delete button
//                    ImageAdapter.ImageViewHolder imageViewHolder = (ImageAdapter.ImageViewHolder) viewHolder;
//                    imageViewHolder.deleteButton.setVisibility(View.VISIBLE);
//                }
//                super.onSelectedChanged(viewHolder, actionState);
//            }
//
//            @Override
//            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
//                // Cast the ViewHolder to your custom ViewHolder to access your delete button
//                ImageAdapter.ImageViewHolder imageViewHolder = (ImageAdapter.ImageViewHolder) viewHolder;
//                imageViewHolder.deleteButton.setVisibility(View.GONE);
//                super.clearView(recyclerView, viewHolder);
//            }
//        };
//
//        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
//        itemTouchHelper.attachToRecyclerView(recyclerView);
        FirebaseStorage.getInstance().getReference().child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                ArrayList<Image> arrayList = new ArrayList<>();
                ImageAdapter adapter = new ImageAdapter(MainActivity.this, arrayList);
                adapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(Image image) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(image.getUrl()));
                        intent.setDataAndType(Uri.parse(image.getUrl()), "image/*");
                        startActivity(intent);
                    }
                });
                recyclerView.setAdapter(adapter);
                listResult.getItems().forEach(new Consumer<StorageReference>() {
                    @Override
                    public void accept(StorageReference storageReference) {
                        Image image = new Image();
                        image.setName(storageReference.getName());
                        storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                String url = "https://" + task.getResult().getEncodedAuthority() + task.getResult().getEncodedPath() + "?alt=media&token=" + task.getResult().getQueryParameters("token").get(0);
                                image.setUrl(url);
                                arrayList.add(image);
                                adapter.notifyDataSetChanged();
                            }
                        });

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Failed to retrieve images", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // Tải hình ảnh lên Firebase
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("images/" + imageUri.getLastPathSegment());

            UploadTask uploadTask = imageRef.putFile(imageUri);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(MainActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageUrl = uri.toString();
                            // Tạo một đối tượng Image mới với URL mới và thêm nó vào danh sách
                            Image image = new Image(imageUrl, "New image");
                            // Thêm vào danh sách hiện có
                            arrayList.add(image);
                            // Thông báo cho adapter rằng dữ liệu đã thay đổi
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    }
}
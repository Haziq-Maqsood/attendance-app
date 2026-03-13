package com.example.hmattendance.adapter;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hmattendance.R;
import com.example.hmattendance.models.Student;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private static final String TAG = "StudentAdapter";
    private List<Student> studentList;
    private OnItemClickListener listener;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnItemClickListener {
        void onItemClick(Student student);
    }

    public StudentAdapter(List<Student> studentList, OnItemClickListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student currentStudent = studentList.get(position);

        holder.ivStudentImage.setImageResource(R.drawable.user); // Set placeholder immediately

        String imagePath = currentStudent.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            holder.ivStudentImage.setTag(imagePath); // Set tag for recycling check

            executorService.execute(() -> {
                File imgFile = new File(imagePath);
                Bitmap loadedBitmap = null;
                if (imgFile.exists()) {
                    Log.d(TAG, "Attempting to load and scale image from path: " + imagePath);
                    try {
                        // Get the dimensions of the ImageView.
                        // For RecyclerView items, a fixed size is usually best.
                        // Let's assume a target width/height for the CircleImageView, e.g., 100dp.
                        // Convert dp to pixels for BitmapFactory.Options
                        int targetWidth = dpToPx(holder.itemView.getContext(), 100); // Example target width in pixels
                        int targetHeight = dpToPx(holder.itemView.getContext(), 100); // Example target height in pixels

                        loadedBitmap = decodeSampledBitmapFromFile(imgFile.getAbsolutePath(), targetWidth, targetHeight);

                        if (loadedBitmap == null) {
                            Log.e(TAG, "BitmapFactory.decodeFile or scaling returned null for path: " + imagePath + ". File might be corrupted or decoding failed.");
                        } else {
                            Log.d(TAG, "Image loaded and scaled successfully for path: " + imagePath + ", size: " + loadedBitmap.getWidth() + "x" + loadedBitmap.getHeight());
                        }
                    } catch (OutOfMemoryError e) {
                        Log.e(TAG, "Out of memory while decoding or scaling bitmap for path: " + imagePath + ": " + e.getMessage());
                        loadedBitmap = null;
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error decoding or scaling bitmap for path: " + imagePath + ": " + e.getMessage());
                        loadedBitmap = null;
                    }
                } else {
                    Log.w(TAG, "Image file does not exist at path: " + imagePath);
                }

                Bitmap finalLoadedBitmap = loadedBitmap;
                mainHandler.post(() -> {
                    if (holder.ivStudentImage.getTag() != null && holder.ivStudentImage.getTag().equals(imagePath)) {
                        if (finalLoadedBitmap != null) {
                            holder.ivStudentImage.setImageBitmap(finalLoadedBitmap);
                        } else {
                            holder.ivStudentImage.setImageResource(R.drawable.user); // Fallback to default
                            // Consider removing this Toast if it appears too often or is disruptive
                            // Toast.makeText(holder.itemView.getContext(), "Failed to load image for " + currentStudent.getName(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Skipping image update for " + currentStudent.getName() + " (recycled view)");
                    }
                });
            });
        } else {
            Log.d(TAG, "No image path available for student: " + currentStudent.getName());
            holder.ivStudentImage.setImageResource(R.drawable.user); // No path, show placeholder
        }

        // Set Student Details (rest of your code)
        holder.tvStudentId.setText(holder.itemView.getContext().getString(R.string.student_id_placeholder, currentStudent.getId()));
        holder.tvStudentName.setText(holder.itemView.getContext().getString(R.string.format_name, currentStudent.getName()));
        holder.tvFatherName.setText(holder.itemView.getContext().getString(R.string.format_father_name, currentStudent.getFatherName()));
        holder.tvAge.setText(holder.itemView.getContext().getString(R.string.format_age, currentStudent.getAge()));
        holder.tvAdmissionDate.setText(holder.itemView.getContext().getString(R.string.format_admission_date, currentStudent.getAdmissionDate()));
        holder.belt.setText(currentStudent.getBelt());

        if (currentStudent.getBelt() != null) {
            switch (currentStudent.getBelt()) {
                case "White Belt":
                    holder.belt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_text_color));
                    break;
                case "Yellow Belt":
                    holder.belt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow));
                    break;
                case "Green Belt":
                    holder.belt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
                    break;
                case "Blue Belt":
                    holder.belt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
                    break;
                case "Red Belt":
                    holder.belt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
                    break;
                case "Black Belt":
                    holder.belt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
                    break;
                default:
                    holder.belt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_text_color));
                    break;
            }
        } else {
            holder.belt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_text_color));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentStudent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void setStudentList(List<Student> newStudentList) {
        this.studentList = newStudentList;
        notifyDataSetChanged();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivStudentImage;
        TextView tvStudentId;
        TextView tvStudentName;
        TextView tvFatherName;
        TextView tvAge;
        TextView tvAdmissionDate;
        TextView belt;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStudentImage = itemView.findViewById(R.id.iv_student_item_image);
            tvStudentId = itemView.findViewById(R.id.tv_student_item_id);
            tvStudentName = itemView.findViewById(R.id.tv_student_item_name);
            tvFatherName = itemView.findViewById(R.id.tv_student_item_father_name);
            tvAge = itemView.findViewById(R.id.tv_student_item_age);
            tvAdmissionDate = itemView.findViewById(R.id.tv_student_item_admission_date);
            belt = itemView.findViewById(R.id.tv_student_item_belt);
        }
    }

    // --- New helper methods for image scaling ---

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromFile(String path,
                                                     int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        Log.d(TAG, "Calculated inSampleSize: " + options.inSampleSize + " for original: " + options.outWidth + "x" + options.outHeight + " to req: " + reqWidth + "x" + reqHeight);


        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        try {
            return BitmapFactory.decodeFile(path, options);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OOM while decoding sampled bitmap: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error decoding sampled bitmap: " + e.getMessage());
            return null;
        }
    }

    // Helper to convert dp to pixels
    private int dpToPx(android.content.Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
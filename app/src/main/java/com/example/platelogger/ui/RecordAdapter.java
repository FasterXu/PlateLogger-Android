package com.example.platelogger.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.platelogger.R;
import com.example.platelogger.PlateImageActivity;
import com.example.platelogger.data.PlateRecord;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.Holder> {
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private List<PlateRecord> records = new ArrayList<>();

    public void submit(List<PlateRecord> value) {
        records = value;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plate_record, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PlateRecord record = records.get(position);
        holder.code.setText(record.plateNumber);
        PlateBadgeStyler.apply(holder.code, record.plateType);
        holder.type.setText(record.plateType);
        holder.time.setText(dateFormat.format(new Date(record.capturedAt)) + "  ·  " +
                String.format(Locale.CHINA, "置信度 %.1f%%", record.confidence * 100f));
        if (record.hasLocation()) {
            holder.location.setText(String.format(Locale.CHINA, "位置 %.6f, %.6f（%s）",
                    record.latitude, record.longitude,
                    record.locationProvider == null ? "系统定位" : record.locationProvider));
        } else {
            holder.location.setText("位置未获取（未授权或暂时无定位）");
        }

        holder.image.setImageDrawable(null);
        holder.image.setOnClickListener(null);
        holder.image.setClickable(false);
        holder.image.setFocusable(false);
        File imageFile = new File(record.imagePath);
        if (imageFile.isFile()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            holder.image.setImageBitmap(bitmap);
            holder.image.setClickable(true);
            holder.image.setFocusable(true);
            holder.image.setContentDescription("点击查看 " + record.plateNumber + " 完整截图");
            holder.image.setOnClickListener(view -> PlateImageActivity.open(
                    view.getContext(), imageFile.getAbsolutePath(), record.plateNumber));
        } else {
            holder.image.setImageResource(R.drawable.ic_camera);
            holder.image.setContentDescription("车牌截图不存在");
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView code;
        final TextView type;
        final TextView time;
        final TextView location;

        Holder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.plate_image);
            code = itemView.findViewById(R.id.plate_code);
            type = itemView.findViewById(R.id.plate_type);
            time = itemView.findViewById(R.id.plate_time);
            location = itemView.findViewById(R.id.plate_location);
        }
    }
}

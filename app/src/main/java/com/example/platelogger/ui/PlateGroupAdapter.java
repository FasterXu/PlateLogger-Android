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
import com.example.platelogger.data.PlateGroup;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PlateGroupAdapter extends RecyclerView.Adapter<PlateGroupAdapter.Holder> {
    public interface OnPlateClickListener {
        void onPlateClick(String plateNumber);
    }

    private final OnPlateClickListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private List<PlateGroup> groups = new ArrayList<>();

    public PlateGroupAdapter(OnPlateClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<PlateGroup> value) {
        groups = value;
        notifyDataSetChanged();
    }

    public PlateGroup getItem(int position) {
        return groups.get(position);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plate_group, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PlateGroup group = groups.get(position);
        holder.code.setText(group.plateNumber);
        PlateBadgeStyler.apply(holder.code, group.latestPlateType);
        holder.type.setText(group.latestPlateType);
        if (group.note == null || group.note.isBlank()) {
            holder.note.setText("");
            holder.note.setVisibility(View.GONE);
        } else {
            holder.note.setText("备注：" + group.note);
            holder.note.setVisibility(View.VISIBLE);
        }
        holder.count.setText(group.recordCount + " 次记录");
        holder.lastSeen.setText("最近：" + dateFormat.format(new Date(group.lastSeenAt)));
        holder.image.setImageDrawable(null);
        holder.image.setOnClickListener(null);
        holder.image.setClickable(false);
        holder.image.setFocusable(false);
        File file = new File(group.latestImagePath == null ? "" : group.latestImagePath);
        if (file.isFile()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            holder.image.setImageBitmap(bitmap);
            holder.image.setClickable(true);
            holder.image.setFocusable(true);
            holder.image.setContentDescription("点击查看 " + group.plateNumber + " 完整截图");
            holder.image.setOnClickListener(view -> PlateImageActivity.open(
                    view.getContext(), file.getAbsolutePath(), group.plateNumber));
        } else {
            holder.image.setImageResource(R.drawable.ic_camera);
            holder.image.setContentDescription("车牌截图不存在");
        }
        holder.itemView.setOnClickListener(v -> listener.onPlateClick(group.plateNumber));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView code;
        final TextView type;
        final TextView note;
        final TextView count;
        final TextView lastSeen;

        Holder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.group_image);
            code = itemView.findViewById(R.id.group_code);
            type = itemView.findViewById(R.id.group_type);
            note = itemView.findViewById(R.id.group_note);
            count = itemView.findViewById(R.id.group_count);
            lastSeen = itemView.findViewById(R.id.group_last_seen);
        }
    }
}

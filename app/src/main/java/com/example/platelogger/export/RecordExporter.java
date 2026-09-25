package com.example.platelogger.export;

import android.content.ContentResolver;
import android.net.Uri;

import com.example.platelogger.data.PlateRecord;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class RecordExporter {
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private RecordExporter() { }

    public static void exportZip(ContentResolver resolver, Uri destination,
                                 List<PlateRecord> records,
                                 Map<String, String> notes) throws IOException {
        OutputStream stream = resolver.openOutputStream(destination, "w");
        if (stream == null) throw new IOException("无法打开导出文件");
        try (ZipOutputStream zip = new ZipOutputStream(stream)) {
            zip.putNextEntry(new ZipEntry("车牌记录.csv"));
            zip.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            zip.write("序号,车牌号,备注,车牌类型,出现时间,纬度,经度,定位来源,置信度,截图文件\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            for (PlateRecord record : records) {
                File image = new File(record.imagePath);
                String imageName = image.exists() ? "images/" + safeImageName(record, image) : "";
                String row = record.id + "," + csv(record.plateNumber) + "," +
                        csv(notes.get(record.plateNumber)) + "," +
                        csv(record.plateType) + "," +
                        csv(DATE_FORMAT.format(new Date(record.capturedAt))) + "," +
                        value(record.latitude) + "," + value(record.longitude) + "," +
                        csv(record.locationProvider) + "," +
                        String.format(Locale.US, "%.3f", record.confidence) + "," +
                        csv(imageName) + "\r\n";
                zip.write(row.getBytes(StandardCharsets.UTF_8));
            }
            zip.closeEntry();

            byte[] buffer = new byte[16 * 1024];
            for (PlateRecord record : records) {
                File image = new File(record.imagePath);
                if (!image.isFile()) continue;
                zip.putNextEntry(new ZipEntry("images/" + safeImageName(record, image)));
                try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(image))) {
                    int count;
                    while ((count = input.read(buffer)) != -1) zip.write(buffer, 0, count);
                }
                zip.closeEntry();
            }
        }
    }

    private static String safeImageName(PlateRecord record, File image) {
        return record.id + "_" + record.plateNumber.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]", "_") + ".jpg";
    }

    private static String value(Double value) {
        return value == null ? "" : String.format(Locale.US, "%.7f", value);
    }

    private static String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

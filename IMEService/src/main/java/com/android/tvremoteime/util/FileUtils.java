package com.android.tvremoteime.util;

import android.text.TextUtils;

import java.io.File;

/**
 * File utility methods.
 */
public class FileUtils {

    /**
     * Check if a file is a media file based on extension.
     */
    public static boolean isMediaFile(String fileName) {
        String ext = getFileExt(fileName);
        switch (ext) {
            case ".avi":
            case ".mp4":
            case ".m4v":
            case ".mkv":
            case ".mov":
            case ".mpeg":
            case ".mpg":
            case ".mpe":
            case ".rm":
            case ".rmvb":
            case ".3gp":
            case ".wmv":
            case ".asf":
            case ".asx":
            case ".dat":
            case ".vob":
            case ".m3u8":
            case ".flv":
            case ".ts":
            case ".webm":
            case ".mp3":
            case ".wav":
            case ".flac":
            case ".aac":
            case ".ogg":
            case ".wma":
                return true;
            default:
                return false;
        }
    }

    /**
     * Get file extension (lowercase, including dot).
     */
    public static String getFileExt(String fileName) {
        if (TextUtils.isEmpty(fileName)) return "";
        int p = fileName.lastIndexOf('.');
        if (p != -1) {
            return fileName.substring(p).toLowerCase();
        }
        return "";
    }

    /**
     * Get file name without extension.
     */
    public static String getFileNameWithoutExt(String filePath) {
        if (TextUtils.isEmpty(filePath)) return "";
        String fileName = filePath;
        int p = fileName.lastIndexOf(File.separatorChar);
        if (p != -1) {
            fileName = fileName.substring(p + 1);
        }
        p = fileName.indexOf('.');
        if (p != -1) {
            fileName = fileName.substring(0, p);
        }
        return fileName;
    }

    /**
     * Get file name from path.
     */
    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath)) return "";
        String fileName = filePath;
        int p = fileName.lastIndexOf(File.separatorChar);
        if (p != -1) {
            fileName = fileName.substring(p + 1);
        }
        return fileName;
    }

    /**
     * Convert file size to human readable format.
     */
    public static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f M" : "%.1f M", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f K" : "%.1f K", f);
        } else {
            return String.format("%d B", size);
        }
    }

    /**
     * Delete file or directory recursively.
     */
    public static void deleteDirFiles(File file) {
        if (file == null || !file.exists()) return;
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    if (f.isDirectory()) deleteDirFiles(f);
                    f.delete();
                } catch (SecurityException e) {
                    // Ignore
                }
            }
        }
    }
}

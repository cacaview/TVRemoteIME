package com.android.tvremoteime.server;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Simple security manager for TVRemoteIME HTTP server.
 * Provides path traversal protection for local network use.
 */
public class SecurityManager {
    private static final String TAG = "SecurityManager";

    private static SecurityManager instance;

    public static synchronized SecurityManager getInstance() {
        if (instance == null) {
            instance = new SecurityManager();
        }
        return instance;
    }

    private SecurityManager() {
    }

    /**
     * Validate and sanitize file path to prevent path traversal attacks
     * @param requestedPath The path requested by the client
     * @return Sanitized path or null if invalid
     */
    public String sanitizePath(String requestedPath) {
        if (requestedPath == null) {
            return null;
        }

        // Decode URL encoding
        try {
            requestedPath = java.net.URLDecoder.decode(requestedPath, "UTF-8");
        } catch (Exception e) {
            return null;
        }

        // Remove null bytes
        if (requestedPath.contains("\0")) {
            return null;
        }

        // Normalize path separators
        requestedPath = requestedPath.replace('\\', '/');

        // Check for path traversal patterns
        if (requestedPath.contains("../") ||
            requestedPath.contains("..\\") ||
            requestedPath.contains("/..") ||
            requestedPath.contains("\\..") ||
            requestedPath.equals("..")) {
            Log.w(TAG, "Path traversal attempt detected: " + requestedPath);
            return null;
        }

        // Remove leading slashes for relative path
        while (requestedPath.startsWith("/")) {
            requestedPath = requestedPath.substring(1);
        }

        return requestedPath;
    }

    /**
     * Validate that the resolved file is within the allowed base directory
     */
    public boolean isPathWithinBase(File file, File baseDir) {
        try {
            String canonicalFile = file.getCanonicalPath();
            String canonicalBase = baseDir.getCanonicalPath();
            return canonicalFile.startsWith(canonicalBase);
        } catch (IOException e) {
            Log.e(TAG, "Failed to validate path", e);
            return false;
        }
    }

    /**
     * Get safe file within external storage
     */
    public File getSafeFile(String requestedPath) {
        String sanitized = sanitizePath(requestedPath);
        if (sanitized == null) {
            return null;
        }

        File baseDir = Environment.getExternalStorageDirectory();
        File requestedFile = new File(baseDir, sanitized);

        if (!isPathWithinBase(requestedFile, baseDir)) {
            Log.w(TAG, "Path escape attempt: " + requestedPath);
            return null;
        }

        return requestedFile;
    }
}

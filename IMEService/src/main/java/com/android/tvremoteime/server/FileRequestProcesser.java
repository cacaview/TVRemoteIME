package com.android.tvremoteime.server;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import com.android.tvremoteime.util.FileUtils;

/**
 * Created by kingt on 2018/1/11.
 */

public class FileRequestProcesser  implements RequestProcesser {
    private Context context;
    private SecurityManager securityManager;

    public FileRequestProcesser(Context context){
        this.context = context;
        this.securityManager = SecurityManager.getInstance();
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String fileName) {
        if(session.getMethod() == NanoHTTPD.Method.GET){
            return fileName.startsWith("/file/dir/")
                    || fileName.startsWith("/file/download/");
        }
        else if(session.getMethod() == NanoHTTPD.Method.POST){
            switch (fileName) {
                case "/file/copy":
                case "/file/cut":
                case "/file/delete":
                case "/file/upload":
                    return true;
            }
        }
        return false;
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String fileName, Map<String, String> params, Map<String, String> files) {
        if(session.getMethod() == NanoHTTPD.Method.GET){
            if(fileName.startsWith("/file/dir/")) {
                return responseDirData(fileName.substring("/file/dir/".length()));
            }else if(fileName.startsWith("/file/download/")){
                return downloadFileData(fileName.substring("/file/download/".length()));
            }
        }
        else if(session.getMethod() == NanoHTTPD.Method.POST) {
            String paths = params.get("paths");
            switch (fileName) {
                case "/file/copy":
                    if (!TextUtils.isEmpty(paths)) {
                        batchCopyFile(params.get("targetPath"), paths);
                    }
                    return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok");
                case "/file/cut":
                    if (!TextUtils.isEmpty(paths)) {
                        batchCutFile(params.get("targetPath"),paths);
                    }
                    return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok");
                case "/file/delete":
                    if (!TextUtils.isEmpty(paths)) {
                        batchDeleteFile(paths);
                    }
                    return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok");
                case "/file/upload":
                    return uploadFile(params, files);
            }
        }
        return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND, "Error 404, file not found.");
    }

    private NanoHTTPD.Response responseDirData(String dirName) {
        // Validate path to prevent path traversal
        File path = securityManager.getSafeFile(dirName);
        if (path == null) {
            return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.FORBIDDEN, "Invalid path");
        }

        String root = Environment.getExternalStorageDirectory().getPath();
        JSONArray dirs = new JSONArray();
        JSONArray files = new JSONArray();
        try {
            File[] subfiles = path.listFiles();
            Arrays.sort(subfiles, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
                }
            });

            for (File file : subfiles) {
                if(file.isHidden()) continue;
                JSONObject item = new JSONObject();
                item.put("name", file.getName());
                item.put("path", file.getPath().substring(root.length()));
                item.put("fullPath", file.getPath());
                if (file.isDirectory()) {
                    //item.put("total", 0);
                    dirs.put(item);
                }else {
                    item.put("size", file.length());
                    item.put("isMedia", FileUtils.isMediaFile(file.getName()));
                    files.put(item);
                }
            }

            JSONObject data = new JSONObject();
            if(!TextUtils.isEmpty(dirName) && dirName != "/") data.put("parent", path.getParent().substring(root.length()));
            data.put("dirs", dirs);
            data.put("files", files);
            return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, data.toString());
        }catch (JSONException ex){
            return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,  "SERVER INTERNAL ERROR: JSONException: " + ex.getMessage());
        }
    }

    private NanoHTTPD.Response downloadFileData(String fileName){
        // Validate path to prevent path traversal
        File file = securityManager.getSafeFile(fileName);
        if (file == null) {
            return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.FORBIDDEN, "Invalid path");
        }
        if(!file.exists()){
            return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND, "Error 404, file not found.");
        }
        try{
            InputStream inputStream = new FileInputStream(file);
            return RemoteServer.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
                    NanoHTTPD.getMimeTypeForFile(file.getName()) + "; charset=utf-8", inputStream, (long)inputStream.available());
        } catch (Exception e) {
            return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND, "Error 404, file not found.");
        }
    }

    private NanoHTTPD.Response uploadFile(Map<String, String> params, Map<String, String> files){
        String uploadFileName  = params.get("file");
        String uploadPathName  = params.get("path");
        String localFilename = files.get("file");
        boolean r = false;
        if(!TextUtils.isEmpty(uploadFileName)) {
            if (!TextUtils.isEmpty(localFilename)) {
                // Validate path to prevent path traversal
                File saveFilename = securityManager.getSafeFile(uploadPathName);
                if (saveFilename == null) {
                    return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{\"success\":false,\"error\":\"Invalid path\"}");
                }
                File localFile = new File(localFilename);
                saveFilename = new File(saveFilename, localFile.getName());
                r = localFile.renameTo(saveFilename);
            }
        }
        return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,  "{\"success\":" + (r ? "true": "false") + "}");
    }

    private void batchDeleteFile(String paths){
        String[] pathData = paths.split("\\|");
        for(String p : pathData){
            if(!TextUtils.isEmpty(p)) {
                // Validate path to prevent path traversal
                File path = securityManager.getSafeFile(p);
                if (path != null) {
                    RemoteServerFileManager.deleteFile(path);
                }
            }
        }
    }
    private void batchCopyFile(String targetPath, String paths){
        // Validate target path
        File targetPathFile = securityManager.getSafeFile(targetPath);
        if(targetPathFile == null || !targetPathFile.exists()) return;

        String[] pathData = paths.split("\\|");
        for(String p : pathData){
            if(!TextUtils.isEmpty(p)) {
                // Validate source path
                File source = securityManager.getSafeFile(p);
                if (source != null) {
                    RemoteServerFileManager.copyFile(source, targetPathFile);
                }
            }
        }
    }
    private void batchCutFile(String targetPath, String paths){
        // Validate target path
        File targetPathFile = securityManager.getSafeFile(targetPath);
        if(targetPathFile == null || !targetPathFile.exists()) return;

        String[] pathData = paths.split("\\|");
        for(String p : pathData){
            if(!TextUtils.isEmpty(p)) {
                // Validate source path
                File source = securityManager.getSafeFile(p);
                if (source != null) {
                    RemoteServerFileManager.cutFile(source, targetPathFile);
                }
            }
        }
    }
}

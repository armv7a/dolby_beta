package com.raincat.dolby_beta.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <pre>
 *     author : RainCat
 *     e-mail : nining377@gmail.com
 *     time   : 2021/04/16
 *     desc   : 文件操作帮助
 *     version: 1.0
 * </pre>
 */

public class FileHelper {
    /**
     * 写入内容到一个文件
     */
    static void writeFileFromSD(String path, List<String> content) {
        BufferedWriter out = null;
        try {
            File file = new File(path);
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "utf-8"));
            for (String s : content) {
                out.write(s);
                out.write("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解压一个文件
     */
    public static boolean unzipFile(String zipFileString, String outPathString, String fileParentName, String fileName) {
        try {
            File outPath = new File(outPathString);
            if (!outPath.exists()) {
                outPath.mkdirs();
            }

            ZipFile zipFile = new ZipFile(zipFileString);
            InputStream is;
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            ZipEntry entry;
            while (e.hasMoreElements()) {
                entry = e.nextElement();
                if (entry.getName().contains(fileParentName) && entry.getName().contains(fileName) && !entry.isDirectory()) {
                    is = zipFile.getInputStream(entry);
                    File dstFile = new File(outPathString + "/" + fileName);
                    FileOutputStream fos = new FileOutputStream(dstFile);
                    int len;
                    byte[] buffer = new byte[8192];
                    while ((len = is.read(buffer, 0, buffer.length)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 解压整个zip
     */
    public static boolean unzipFiles(String zipFileString, String outPathString) {
        try {
            File outPath = new File(outPathString);
            if (!outPath.exists()) {
                outPath.mkdirs();
            }

            ZipFile zipFile = new ZipFile(zipFileString);
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            ZipEntry entry;
            String szName = "";
            while (e.hasMoreElements()) {
                entry = e.nextElement();
                if (entry.isDirectory()) {
                    szName = entry.getName();
                    szName = szName.substring(0, szName.length() - 1);
                    File folder = new File(outPathString + File.separator + szName);
                    folder.mkdirs();
                } else {
                    InputStream is = zipFile.getInputStream(entry);
                    File dstFile = new File(outPathString + "/" + entry.getName());
                    FileOutputStream fos = new FileOutputStream(dstFile);
                    int len;
                    byte[] buffer = new byte[8192];
                    while ((len = is.read(buffer, 0, buffer.length)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}

package com.sjbb.core.utils;

import com.sjbb.core.constant.CommonConst;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class FileUtils {


    /**
     * 默认放在本项目资源路径下
     *
     * @param mFile         上传文件
     * @param subFolderPath 保存文件夹相对路径
     * @return 文件资源路径
     */
    public static String saveMultipartFileInStatic(MultipartFile mFile, String subFolderPath, String fileName) {
        String folderPath = getAbsolutePathByProject(subFolderPath);
        return folderPath + saveMultipartFile(mFile, folderPath, fileName);
    }

    /**
     * 上传文件保存
     *
     * @param mFile      上传文件
     * @param folderPath 保存文件夹路径
     * @return 文件名
     */
    public static String saveMultipartFile(MultipartFile mFile, String folderPath, String fileName) {
        if (StringUtils.isBlank(fileName)) {
            String[] names = mFile.getOriginalFilename().split("\\.");
            fileName = UUID.randomUUID().toString().replaceAll("-", "") + "." + names[names.length - 1];
        }
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = mFile.getInputStream();
            File dir = new File(folderPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filePath = folderPath + fileName;
            fos = new FileOutputStream(filePath);
            int len = 0;
            byte[] buf = new byte[1024 * 1024];
            while ((len = is.read(buf)) > -1) {
                fos.write(buf, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fileName;
    }

    /**
     * 获取项目的绝对路径(D:\git\tl\web\tl-api\target\classes) + "static" + subFolderPath
     *
     * @param subFolderPath 相对路径
     * @return 绝对路径
     */
    public static String getAbsolutePathByProject(String subFolderPath) {
        String classesPath = null;
        try {
            classesPath = new String(FileUtils.class.getResource("/").getPath().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classesPath + CommonConst.STATIC_PATH + subFolderPath;
    }
}

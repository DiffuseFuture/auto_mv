package com.ohyesai.next.component;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public interface FileComponent {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");

    String upload(InputStream inputStream, String objectName);

    String upload(InputStream inputStream, String objectName, String mimeType);

    String upload(byte[] bytes, String objectName);

    String upload(String path, String objectName);

    String upload(File file, String objectName);

    String uploadPub(File file, String objectName);

    String shareUrl(@Nullable String objectName);

    String uploadByUrl(String url, String objectName);

    /**
     * 获取公开文件url地址;
     * 复制原始文件到公开桶 生成新url
     * 该方法用于一些特殊三方api。我们传入带签名url导致无法识别
     *
     * @param objectName
     * @return
     */
    String shareUrlPubByCopy(String objectName);

    String shareUrlPub(String objectName);

    void delete(String objectName);

    /**
     * 生成minio文件名
     *
     * @param extName 扩展名(如果存在会拼接到末尾)，支持解析文件名;   jpg   xx.jpg
     */
    default String genObjectName(String prefix, String extName) {
        StringJoiner sj = new StringJoiner("/");
        sj.add(prefix);
        sj.add(LocalDate.now().format(formatter));
        if (StrUtil.isNotBlank(extName)) {
            // 有扩展名 则拼接
            int index = extName.lastIndexOf(".");
            if (index != -1) {
                extName = extName.substring(index + 1);
            }
            sj.add(IdUtil.fastSimpleUUID() + "." + extName);
        } else {
            // 没有扩展名直接返回
            sj.add(IdUtil.fastSimpleUUID());
        }
        return sj.toString();
    }

    /**
     * 扩展名转媒体类型
     *
     * @param extName
     * @return
     */
    default String mimeType(String extName) {
        try {
            return Files.probeContentType(Path.of(extName));
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    InputStream download(String objectName);

    void download(String objectName, File outFile);

    void copy(String sourceObjectName, String targetObjectName);
}

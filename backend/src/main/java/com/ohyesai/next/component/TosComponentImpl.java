package com.ohyesai.next.component;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ohyesai.next.common.consts.FileDirConst;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.common.properties.TosProperties;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.comm.HttpMethod;
import com.volcengine.tos.model.bucket.DoesBucketExistInput;
import com.volcengine.tos.model.object.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;

@Slf4j
@Component
public class TosComponentImpl implements FileComponent {

    // 使用应用名称当桶名
    private final String bucketName;

    private final String publicBucketName;

    private final TOSV2 tos;

    private final TosProperties tosProperties;

    public TosComponentImpl(TOSV2 tos, TosProperties tosProperties) {
        this.tos = tos;
        this.tosProperties = tosProperties;
        this.bucketName = tosProperties.bucket();
        this.publicBucketName = this.bucketName + "-public";
    }


    @Override
    public String upload(InputStream inputStream, String objectName) {
        return upload(inputStream, objectName, mimeType(objectName));
    }

    @Override
    public String upload(InputStream inputStream, String objectName, String mimeType) {
        log.info("开始上传 {}", objectName);
        // 清理缓存; 如果一个文件刚上传 那本地不应该有他的缓存
        cleanCache(objectName);

        try {
            // 校验桶
            DoesBucketExistInput doesBucketExistInput = new DoesBucketExistInput()
                    .setBucket(bucketName);
            if (!tos.doesBucketExist(doesBucketExistInput)) {
                tos.createBucket(bucketName);
            }

            // 原数据
            ObjectMetaRequestOptions options = new ObjectMetaRequestOptions()
                    .setContentType(mimeType);
            PutObjectInput putObjectInput = new PutObjectInput()
                    .setBucket(bucketName)
                    .setKey(objectName)
                    .setOptions(options)
                    .setContent(inputStream);
            // 上传
            PutObjectOutput _ = tos.putObject(putObjectInput);
            log.info("上传结束 {}", objectName);
            return objectName;
        } catch (Exception e) {
            throw new BusinessException(e);
        }

    }

    @Override
    public String upload(byte[] bytes, String objectName) {
        return upload(new ByteArrayInputStream(bytes), objectName);
    }

    @Override
    public String upload(String path, String objectName) {
        return upload(new File(path), bucketName, objectName, mimeType(objectName));
    }

    @Override
    public String upload(File file, String objectName) {
        return upload(file, bucketName, objectName, mimeType(objectName));
    }

    private String upload(File file, String bucketName, String objectName, String mimeType) {
        log.info("开始上传 {}", objectName);
        // 清理缓存; 如果一个文件刚上传 那本地不应该有他的缓存
        cleanCache(objectName);

        try (InputStream inputStream = new FileInputStream(file)) {
            // 校验桶
            DoesBucketExistInput doesBucketExistInput = new DoesBucketExistInput()
                    .setBucket(bucketName);
            if (!tos.doesBucketExist(doesBucketExistInput)) {
                tos.createBucket(bucketName);
            }

            // 原数据
            ObjectMetaRequestOptions options = new ObjectMetaRequestOptions()
                    .setContentType(mimeType);
            PutObjectInput putObjectInput = new PutObjectInput()
                    .setBucket(bucketName)
                    .setKey(objectName)
                    .setOptions(options)
                    .setContent(inputStream)
                    .setContentLength(file.length());

            PutObjectOutput _ = tos.putObject(putObjectInput);
            log.info("上传结束 {}", objectName);
            return objectName;
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    @Override
    public String uploadByUrl(String url, String objectName) {
        log.info("uploadByUrl 开始打开流 {} {}", url, objectName);
        // 清理缓存
        cleanCache(objectName);

        HttpResponse response = HttpRequest.get(url).execute();
        InputStream inputStream = response.bodyStream();
        try (response; inputStream) {
            log.info("uploadByUrl 获取流成功，即将进入流式上传 {} {}", url, objectName);
            return upload(inputStream, objectName);
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    public String uploadPub(File file, String objectName) {
        return upload(file, publicBucketName, objectName, mimeType(objectName));
    }

    @Override
    public String shareUrl(String objectName) {
        if (StrUtil.isBlank(objectName)) {
            return null;
        }
        PreSignedURLInput input = new PreSignedURLInput()
                .setBucket(bucketName)
                .setKey(objectName)
                .setHttpMethod(HttpMethod.GET).setExpires(7 * 24 * 3600);
        PreSignedURLOutput output = tos.preSignedURL(input);
        return output.getSignedUrl();
    }


    @Override
    public String shareUrlPubByCopy(String objectName) {
        // 复制原文件到公开桶 放到临时文件目录
        String targetObjectName = "temp/" + objectName;

        CopyObjectV2Input input = new CopyObjectV2Input()
                .setBucket(publicBucketName)
                .setKey(targetObjectName)
                .setSrcBucket(bucketName)
                .setSrcKey(objectName);

        CopyObjectV2Output output = tos.copyObject(input);

        // 公开桶直接返回
        return "https://" + publicBucketName + "." + tosProperties.endpoint() + "/" + targetObjectName;
    }

    @Override
    public String shareUrlPub(String objectName) {
        return "https://" + publicBucketName + "." + tosProperties.endpoint() + "/" + objectName;
    }

    @Override
    public void delete(String objectName) {
        if (StrUtil.isBlank(objectName)) {
            return;
        }
        DeleteObjectInput input = new DeleteObjectInput()
                .setBucket(bucketName)
                .setKey(objectName);
        DeleteObjectOutput output = tos.deleteObject(input);
    }

    @Override
    public InputStream download(String objectName) {
        // 获取缓存文件
        File cacheFile = getCache(objectName);
        if (cacheFile == null) {
            // 不存在则缓存文件
            GetObjectV2Input input = new GetObjectV2Input()
                    .setBucket(bucketName)
                    .setKey(objectName);

            InputStream inputStream = tos.getObject(input).getContent();
            cacheFile = putCache(objectName, inputStream);
        }

        try {
            return Files.newInputStream(cacheFile.toPath());
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    public void download(String objectName, File outFile) {
        // 创建父级目录
        var _ = outFile.getParentFile().mkdirs();
        // 获取缓存文件
        File cacheFile = getCache(objectName);
        if (cacheFile == null) {
            // 不存在则缓存
            GetObjectToFileInput fileInput = new GetObjectToFileInput()
                    .setFile(outFile)
                    .setBucket(bucketName)
                    .setKey(objectName);
            GetObjectToFileOutput _ = tos.getObjectToFile(fileInput);
            // 缓存文件
            putCache(objectName, outFile);
            return;
        }
        // 存在直接返回
        FileUtil.copy(cacheFile, outFile, true);
    }

    /**
     * 获取文件缓存，如果文件在本地存在则返回缓存文件
     *
     * @param fileId
     * @return
     */
    private File getCache(String fileId) {
        File file = new File(FileDirConst.LOCAL_CACHE_DIR, fileId);
        if (!file.exists()) {
            return null;
        }
        log.info("tos 缓存命中 {}", fileId);
        return file;
    }

    private void putCache(String fileId, File file) {
        File cacheFile = new File(FileDirConst.LOCAL_CACHE_DIR, fileId);
        boolean _ = cacheFile.getParentFile().mkdirs();
        FileUtil.copy(file, cacheFile, true);
    }

    private File putCache(String fileId, InputStream inputStream) {
        File cacheFile = new File(FileDirConst.LOCAL_CACHE_DIR, fileId);
        boolean _ = cacheFile.getParentFile().mkdirs();
        FileUtil.writeFromStream(inputStream, cacheFile);
        return cacheFile;
    }

    /**
     * 清理缓存
     * 每次上传文件时  清除对应的本地缓存
     *
     * @param fileId
     */
    private void cleanCache(String fileId) {
        File file = new File(FileDirConst.LOCAL_CACHE_DIR, fileId);
        if (file.exists()) {
            log.info("清理缓存 {}", fileId);
            FileUtil.del(file);
        }
    }

    @Override
    public void copy(String sourceObjectName, String targetObjectName) {
        CopyObjectV2Input input = new CopyObjectV2Input()
                .setBucket(bucketName)
                .setKey(targetObjectName)
                .setSrcBucket(bucketName)
                .setSrcKey(sourceObjectName);
        CopyObjectV2Output output = tos.copyObject(input);
    }


}

package com.communityfreshmarket.upload;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.config.AppProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 图片放在本项目自己的 MinIO 桶里，公开读。手机经本站 /files/ 打开。 */
@Service
public class MinioImageStorage {

  private static final Logger log = LoggerFactory.getLogger(MinioImageStorage.class);

  private final AppProperties appProperties;
  private MinioClient client;

  public MinioImageStorage(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @PostConstruct
  public void prepare() {
    if (!configured()) {
      log.info("minio not configured; image upload disabled until MINIO_* and UPLOAD_PUBLIC_BASE are set");
      return;
    }
    try {
      MinioClient created = client();
      String bucket = bucket();
      boolean exists = created.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!exists) {
        created.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      }
      created.setBucketPolicy(
          SetBucketPolicyArgs.builder().bucket(bucket).config(publicReadPolicy(bucket)).build());
      log.info("minio bucket ready: {}", bucket);
    } catch (Exception ex) {
      log.warn("minio bucket prepare failed: {}", ex.getMessage());
    }
  }

  public String publicUrl(String objectName) {
    String base = appProperties.getUploadPublicBase();
    if (!StringUtils.hasText(base)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "图片地址未配置");
    }
    String prefix = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    return prefix + "/" + objectName;
  }

  public void put(String objectName, byte[] body, String contentType) {
    try {
      client()
          .putObject(
              PutObjectArgs.builder()
                  .bucket(bucket())
                  .object(objectName)
                  .stream(new ByteArrayInputStream(body), body.length, -1)
                  .contentType(contentType)
                  .build());
    } catch (BizException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("minio put failed: {}", ex.getMessage());
      throw new BizException(ErrorCodes.BAD_REQUEST, "上传失败，请重试");
    }
  }

  public void remove(String objectName) {
    if (!configured() || !StringUtils.hasText(objectName)) {
      return;
    }
    try {
      client()
          .removeObject(RemoveObjectArgs.builder().bucket(bucket()).object(objectName).build());
    } catch (Exception ex) {
      log.warn("minio remove failed: {}", objectName);
    }
  }

  private boolean configured() {
    return StringUtils.hasText(appProperties.getMinioEndpoint())
        && StringUtils.hasText(appProperties.getMinioAccessKey())
        && StringUtils.hasText(appProperties.getMinioSecretKey())
        && StringUtils.hasText(appProperties.getMinioBucket())
        && StringUtils.hasText(appProperties.getUploadPublicBase());
  }

  private MinioClient client() {
    if (!configured()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "图片存储未配置");
    }
    if (client == null) {
      client =
          MinioClient.builder()
              .endpoint(appProperties.getMinioEndpoint().trim())
              .credentials(
                  appProperties.getMinioAccessKey().trim(),
                  appProperties.getMinioSecretKey().trim())
              .build();
    }
    return client;
  }

  private String bucket() {
    return appProperties.getMinioBucket().trim();
  }

  private static String publicReadPolicy(String bucket) {
    return """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Principal": {"AWS": ["*"]},
              "Action": ["s3:GetObject"],
              "Resource": ["arn:aws:s3:::%s/*"]
            }
          ]
        }
        """
        .formatted(bucket);
  }
}

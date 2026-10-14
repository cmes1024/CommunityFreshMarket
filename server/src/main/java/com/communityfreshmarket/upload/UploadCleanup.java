package com.communityfreshmarket.upload;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.config.AppProperties;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.mapper.StoreMapper;
import java.util.Collection;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 本服务上传的图片不再被商品图、实景图或首页头图引用时，从 MinIO 删除。 */
@Service
public class UploadCleanup {

  private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9._-]+");

  private final AppProperties appProperties;
  private final ProductMapper productMapper;
  private final StoreMapper storeMapper;
  private final MinioImageStorage imageStorage;

  public UploadCleanup(
      AppProperties appProperties,
      ProductMapper productMapper,
      StoreMapper storeMapper,
      MinioImageStorage imageStorage) {
    this.appProperties = appProperties;
    this.productMapper = productMapper;
    this.storeMapper = storeMapper;
    this.imageStorage = imageStorage;
  }

  public void release(Collection<String> urls) {
    if (urls == null || urls.isEmpty()) {
      return;
    }
    for (String url : urls) {
      releaseOne(url);
    }
  }

  private void releaseOne(String url) {
    String filename = ownedFilename(url);
    if (filename == null || stillReferenced(url.trim())) {
      return;
    }
    imageStorage.remove(filename);
  }

  private boolean stillReferenced(String url) {
    Long products =
        productMapper.selectCount(
            new LambdaQueryWrapper<Product>()
                .and(
                    w ->
                        w.eq(Product::getImageUrl, url)
                            .or()
                            .eq(Product::getLivePhotoUrl, url)
                            .or()
                            .like(Product::getImageUrls, url)));
    if (products != null && products > 0) {
      return true;
    }
    Long stores =
        storeMapper.selectCount(new LambdaQueryWrapper<Store>().eq(Store::getHeroImageUrl, url));
    return stores != null && stores > 0;
  }

  /** 只认本服务公开地址下的单层文件名，外链不删。 */
  private String ownedFilename(String url) {
    if (!StringUtils.hasText(url)) {
      return null;
    }
    String base = appProperties.getUploadPublicBase();
    if (!StringUtils.hasText(base)) {
      return null;
    }
    String prefix = base.endsWith("/") ? base : base + "/";
    String value = url.trim();
    if (!value.startsWith(prefix)) {
      return null;
    }
    String name = value.substring(prefix.length());
    if (name.isEmpty() || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || !FILE_NAME.matcher(name).matches()) {
      return null;
    }
    return name;
  }
}

package com.communityfreshmarket.upload;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

  private static final Set<String> ALLOWED =
      Set.of("jpg", "jpeg", "png", "webp", "gif");

  private final AppProperties appProperties;

  public UploadController(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @PostMapping("/image")
  public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择图片");
    }
    String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    String ext = extension(original);
    if (!ALLOWED.contains(ext)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "仅支持 jpg/png/webp/gif");
    }
    if (file.getSize() > 5 * 1024 * 1024L) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "图片不能超过 5MB");
    }

    String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
    Path dir = Path.of(appProperties.getUploadDir());
    try {
      Files.createDirectories(dir);
      Path target = dir.resolve(filename);
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "上传失败，请重试");
    }

    String url = appProperties.getUploadPublicBase() + "/" + filename;
    return ApiResponse.ok(Map.of("url", url));
  }

  private static String extension(String name) {
    int i = name.lastIndexOf('.');
    if (i < 0 || i == name.length() - 1) {
      return "";
    }
    return name.substring(i + 1).toLowerCase(Locale.ROOT);
  }
}

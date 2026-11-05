package com.communityfreshmarket.upload;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import java.io.IOException;
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

  private final MinioImageStorage imageStorage;

  public UploadController(MinioImageStorage imageStorage) {
    this.imageStorage = imageStorage;
  }

  @PostMapping("/image")
  public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择图片");
    }
    if (file.getSize() > 5 * 1024 * 1024L) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "图片不能超过 5MB");
    }
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "上传失败，请重试");
    }
    String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    String ext = extension(original);
    if (!ALLOWED.contains(ext)) {
      ext = extensionFromContentType(file.getContentType());
    }
    if (!ALLOWED.contains(ext)) {
      // 微信 chooseAvatar 给的是无扩展名临时文件，Content-Type 也可能不是图片类型
      ext = extensionFromMagic(bytes);
    }
    if (!ALLOWED.contains(ext)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "仅支持 jpg/png/webp/gif");
    }

    String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
    imageStorage.put(filename, bytes, contentType(ext));
    return ApiResponse.ok(Map.of("url", imageStorage.publicUrl(filename)));
  }

  /** 文件名和 MIME 都认不出时按文件头判断，兜住微信头像这类临时文件。 */
  private static String extensionFromMagic(byte[] bytes) {
    if (bytes == null || bytes.length < 4) {
      return "";
    }
    if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
      return "jpg";
    }
    if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
      return "png";
    }
    if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
      return "gif";
    }
    if (bytes.length >= 12
        && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
        && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
      return "webp";
    }
    return "";
  }

  private static String contentType(String ext) {
    return switch (ext) {
      case "png" -> "image/png";
      case "webp" -> "image/webp";
      case "gif" -> "image/gif";
      default -> "image/jpeg";
    };
  }

  private static String extensionFromContentType(String contentType) {
    if (contentType == null) {
      return "";
    }
    String type = contentType.toLowerCase(Locale.ROOT);
    int semi = type.indexOf(';');
    if (semi >= 0) {
      type = type.substring(0, semi).trim();
    }
    return switch (type) {
      case "image/jpeg", "image/jpg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      case "image/gif" -> "gif";
      default -> "";
    };
  }

  private static String extension(String name) {
    int i = name.lastIndexOf('.');
    if (i < 0 || i == name.length() - 1) {
      return "";
    }
    return name.substring(i + 1).toLowerCase(Locale.ROOT);
  }
}

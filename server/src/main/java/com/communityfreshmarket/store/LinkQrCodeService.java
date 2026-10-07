package com.communityfreshmarket.store;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 本地生成普通链接二维码 PNG（非微信小程序太阳码，无数量额度）。 */
@Service
public class LinkQrCodeService {

  private static final Logger log = LoggerFactory.getLogger(LinkQrCodeService.class);

  public String pngBase64(String content) {
    if (!StringUtils.hasText(content)) {
      return null;
    }
    try {
      Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
      hints.put(EncodeHintType.MARGIN, 1);
      BitMatrix matrix = new QRCodeWriter().encode(content.trim(), BarcodeFormat.QR_CODE, 430, 430, hints);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return Base64.getEncoder().encodeToString(out.toByteArray());
    } catch (Exception ex) {
      log.warn("link qr encode error: {}", ex.getMessage());
      return null;
    }
  }
}

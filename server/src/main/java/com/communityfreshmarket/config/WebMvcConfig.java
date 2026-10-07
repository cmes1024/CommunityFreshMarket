package com.communityfreshmarket.config;

import com.communityfreshmarket.auth.AuthInterceptor;
import com.communityfreshmarket.auth.MerchantAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AuthInterceptor authInterceptor;
  private final MerchantAuthInterceptor merchantAuthInterceptor;

  public WebMvcConfig(
      AuthInterceptor authInterceptor, MerchantAuthInterceptor merchantAuthInterceptor) {
    this.authInterceptor = authInterceptor;
    this.merchantAuthInterceptor = merchantAuthInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
        .addPathPatterns("/api/v1/**")
        .excludePathPatterns(
            "/api/v1/health",
            "/api/v1/auth/wechat",
            "/api/v1/meta/**");
    registry.addInterceptor(merchantAuthInterceptor)
        .addPathPatterns("/api/v1/merchant/**");
  }
}

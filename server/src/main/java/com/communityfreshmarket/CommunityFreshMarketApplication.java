package com.communityfreshmarket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.communityfreshmarket.mapper")
public class CommunityFreshMarketApplication {

  public static void main(String[] args) {
    SpringApplication.run(CommunityFreshMarketApplication.class, args);
  }
}

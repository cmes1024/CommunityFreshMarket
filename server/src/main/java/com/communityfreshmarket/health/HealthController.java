package com.communityfreshmarket.health;

import com.communityfreshmarket.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

  private final JdbcTemplate jdbcTemplate;

  public HealthController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/health")
  public ApiResponse<Map<String, Object>> health() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "UP");
    body.put("db", checkDb());
    return ApiResponse.ok(body);
  }

  private String checkDb() {
    try {
      Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      return one != null && one == 1 ? "UP" : "DOWN";
    } catch (Exception ex) {
      return "DOWN";
    }
  }
}

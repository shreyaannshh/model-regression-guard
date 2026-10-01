package com.shreyansh.regressionguard.domain;
import tools.jackson.databind.json.JsonMapper;

final class Json {
  static final JsonMapper MAPPER = JsonMapper.builder().build();
  
  private Json() {
  }
}

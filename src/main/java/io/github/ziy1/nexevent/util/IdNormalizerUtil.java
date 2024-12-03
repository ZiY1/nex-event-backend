package io.github.ziy1.nexevent.util;

public class IdNormalizerUtil {
  public static String normalize(String id) {
    return id != null ? id.toLowerCase() : null;
  }
}

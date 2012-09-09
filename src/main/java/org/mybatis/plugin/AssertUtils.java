package org.mybatis.plugin;

final class AssertUtils {
  static String ensureNotBlank(final String input) {
    if (input == null || input.trim().isEmpty()) {
      throw new IllegalArgumentException("Input must not be blank");
    }
    return input;
  }

  static <T> T ensureNotNull(final T item) {
    if (item == null) {
      throw new IllegalArgumentException("Input must not be null");
    }
    return item;
  }
}

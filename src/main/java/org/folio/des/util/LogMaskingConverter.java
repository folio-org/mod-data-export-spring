package org.folio.des.util;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(name="LogMaskingConverter", category = "Converter")
@ConverterKeys({"spi"})
@Log4j2
public class LogMaskingConverter extends LogEventPatternConverter {
  private static final Pattern SERVER_ADDRESS_PATTERN = Pattern.compile("ftp[s]?://[a-zA-Z0-9.]+/");
  private static final String SERVER_ADDRESS_PATTERN_REPLACEMENT = "**********";
  public static final Pattern PASSWORD_PATTERN = Pattern.compile("password: [a-zA-Z0-9@#$?&%!~]+");
  public static final String PASSWORD_PATTERN_REPLACEMENT = "password: **********";
  public static final Pattern PASSWORD_SECOND_PATTERN = Pattern.compile("\"password\":\"[a-zA-Z0-9@#$?&%!~]+\"");
  public static final String PASSWORD_SECOND_PATTERN_REPLACEMENT = "\"password\":\"**********\"";
  public static final Pattern USERNAME_PATTERN = Pattern.compile("username: [a-zA-Z0-9]+");
  public static final String USERNAME_PATTERN_REPLACEMENT = "username: **********";
  public static final Pattern USERNAME_SECOND_PATTERN = Pattern.compile("\"username\":\"[a-zA-Z0-9]+\"");
  public static final String USERNAME_SECOND_PATTERN_REPLACEMENT = "\"username\":\"**********\"";


  protected LogMaskingConverter(String name, String style) {
    super(name, style);
  }

  public static LogMaskingConverter newInstance(String[] options) {
    return new LogMaskingConverter("spi", Thread.currentThread().getName());
  }

  @Override
  public void format(LogEvent event, StringBuilder toAppendTo) {
    String messageString = toAppendTo.toString();
    toAppendTo.delete(0, toAppendTo.length());
    String maskedMessage;
    try {
      maskedMessage = mask(messageString);
    } catch (Exception e) {
      log.error("Failed While Masking");
      maskedMessage = messageString;
    }
    toAppendTo.append(maskedMessage);
  }

  private String mask(String message) {
    Matcher matcher;
    StringBuffer buffer = new StringBuffer();

    matcher = SERVER_ADDRESS_PATTERN.matcher(message);
    maskMatcher(matcher, buffer, SERVER_ADDRESS_PATTERN_REPLACEMENT);
    message=buffer.toString();
    buffer.setLength(0);

    matcher = PASSWORD_PATTERN.matcher(message);
    maskMatcher(matcher, buffer, PASSWORD_PATTERN_REPLACEMENT);
    message=buffer.toString();
    buffer.setLength(0);

    matcher = PASSWORD_SECOND_PATTERN.matcher(message);
    maskMatcher(matcher, buffer, PASSWORD_SECOND_PATTERN_REPLACEMENT);
    message=buffer.toString();
    buffer.setLength(0);

    matcher = USERNAME_PATTERN.matcher(message);
    maskMatcher(matcher, buffer, USERNAME_PATTERN_REPLACEMENT);
    message=buffer.toString();
    buffer.setLength(0);

    matcher = USERNAME_SECOND_PATTERN.matcher(message);
    maskMatcher(matcher, buffer, USERNAME_SECOND_PATTERN_REPLACEMENT);

    return buffer.toString();
  }

  private void maskMatcher(Matcher matcher, StringBuffer buffer, String maskStr)
  {
    while (matcher.find()) {
      matcher.appendReplacement(buffer,maskStr);
    }
    matcher.appendTail(buffer);
  }
}

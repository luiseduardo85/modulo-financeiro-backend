package com.financeiro.partner.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record Document(String value) {
  private static final Pattern CPF_CANONICAL = Pattern.compile("[0-9]{11}");
  private static final Pattern CPF_FORMATTED =
      Pattern.compile("[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}");
  private static final Pattern CNPJ_CANONICAL = Pattern.compile("[A-Z0-9]{12}[0-9]{2}");
  private static final Pattern CNPJ_FORMATTED =
      Pattern.compile("[A-Z0-9]{2}\\.[A-Z0-9]{3}\\.[A-Z0-9]{3}/[A-Z0-9]{4}-[0-9]{2}");
  private static final int[] CNPJ_FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
  private static final int[] CNPJ_SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

  public Document {
    if (value == null
        || !isCanonical(value)
        || hasRepeatedNumericDigits(value)
        || !hasValidCheckDigits(value)) {
      throw new InvalidPartnerDocumentException();
    }
  }

  public static Document of(String raw) {
    if (raw == null) {
      throw new InvalidPartnerDocumentException();
    }

    String input = raw.strip().toUpperCase(Locale.ROOT);
    if (input.isBlank() || !isAcceptedInput(input)) {
      throw new InvalidPartnerDocumentException();
    }

    String canonical = input.replace(".", "").replace("/", "").replace("-", "");
    return new Document(canonical);
  }

  public DocumentType type() {
    return value.length() == 11 ? DocumentType.CPF : DocumentType.CNPJ;
  }

  private static boolean isAcceptedInput(String input) {
    return isCanonical(input)
        || CPF_FORMATTED.matcher(input).matches()
        || CNPJ_FORMATTED.matcher(input).matches();
  }

  private static boolean isCanonical(String value) {
    return CPF_CANONICAL.matcher(value).matches() || CNPJ_CANONICAL.matcher(value).matches();
  }

  private static boolean hasRepeatedNumericDigits(String value) {
    return value.chars().allMatch(character -> character == value.charAt(0));
  }

  private static boolean hasValidCheckDigits(String value) {
    if (value.length() == 11) {
      return cpfDigit(value, 9) == numericValue(value.charAt(9))
          && cpfDigit(value, 10) == numericValue(value.charAt(10));
    }
    return cnpjDigit(value, 12, CNPJ_FIRST_WEIGHTS) == numericValue(value.charAt(12))
        && cnpjDigit(value, 13, CNPJ_SECOND_WEIGHTS) == numericValue(value.charAt(13));
  }

  private static int cpfDigit(String value, int length) {
    int sum = 0;
    for (int index = 0; index < length; index++) {
      sum += numericValue(value.charAt(index)) * (length + 1 - index);
    }
    return moduloElevenDigit(sum);
  }

  private static int cnpjDigit(String value, int length, int[] weights) {
    int sum = 0;
    for (int index = 0; index < length; index++) {
      sum += cnpjCharacterValue(value.charAt(index)) * weights[index];
    }
    return moduloElevenDigit(sum);
  }

  private static int moduloElevenDigit(int sum) {
    int remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  }

  private static int numericValue(char character) {
    return character - '0';
  }

  private static int cnpjCharacterValue(char character) {
    return character - '0';
  }
}

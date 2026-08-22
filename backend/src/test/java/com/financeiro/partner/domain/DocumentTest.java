package com.financeiro.partner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DocumentTest {
  @Test
  void acceptsAndNormalizesValidCpf() {
    assertThat(Document.of(" 529.982.247-25 ").value()).isEqualTo("52998224725");
    assertThat(Document.of("52998224725").type()).isEqualTo(DocumentType.CPF);
  }

  @Test
  void acceptsLegacyNumericCnpj() {
    assertThat(Document.of("04.252.011/0001-10").value()).isEqualTo("04252011000110");
    assertThat(Document.of("04252011000110").type()).isEqualTo(DocumentType.CNPJ);
  }

  @Test
  void acceptsFormattedAndCanonicalAlphanumericCnpj() {
    assertThat(Document.of(" 00.000.000/E08G-12 ").value()).isEqualTo("00000000E08G12");
    assertThat(Document.of("00000000E08G12").type()).isEqualTo(DocumentType.CNPJ);
  }

  @Test
  void acceptsLowercaseCnpjAndCanonicalizesItToUppercase() {
    assertThat(Document.of("00.000.000/e08g-12").value()).isEqualTo("00000000E08G12");
    assertThat(Document.of("00000000e08g12").value()).isEqualTo("00000000E08G12");
  }

  @Test
  void rejectsInvalidCpfCheckDigits() {
    assertInvalid("52998224715");
    assertInvalid("52998224724");
  }

  @Test
  void rejectsInvalidLegacyCnpjCheckDigits() {
    assertInvalid("04252011000010");
    assertInvalid("04252011000111");
  }

  @Test
  void rejectsAlphanumericCnpjWithWrongFirstCheckDigit() {
    assertInvalid("00000000E08G22");
  }

  @Test
  void rejectsAlphanumericCnpjWithCorrectFirstAndWrongSecondCheckDigit() {
    assertInvalid("00000000E08G11");
  }

  @Test
  void rejectsAlphanumericCnpjWithInvalidCanonicalLength() {
    assertInvalid("00000000E08G1");
    assertInvalid("00000000E08G120");
  }

  @Test
  void rejectsMalformedPunctuationAndLettersInCheckDigitPositions() {
    for (String value :
        new String[] {
          "529 982 247 25", "529.982247-25", "00.000.000-E08G/12",
          "00.000.000/E08G/12", "00000000E08G1A", "00.000.000/E08G-1A"
        }) {
      assertInvalid(value);
    }
  }

  @Test
  void rejectsNullBlankRepeatedBadLengthAndUnsupportedCharacters() {
    for (String value :
        new String[] {
          null, "", "   ", "11111111111", "00000000000000", "123", "529x98224725", "00000000E08@12"
        }) {
      assertInvalid(value);
    }
  }

  private void assertInvalid(String value) {
    assertThatThrownBy(() -> Document.of(value))
        .isInstanceOf(InvalidPartnerDocumentException.class);
  }
}

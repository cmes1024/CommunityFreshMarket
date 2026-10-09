package com.communityfreshmarket.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.communityfreshmarket.common.BizException;
import java.util.List;
import org.junit.jupiter.api.Test;

class RemarkPhrasesTest {

  @Test
  void keepsOrderAndDropsBlanksAndDuplicates() {
    List<String> phrases = RemarkPhrases.normalize(List.of(" 放门口 ", "", "放门口", "少葱"));
    assertEquals(List.of("放门口", "少葱"), phrases);
    assertEquals("放门口\n少葱", RemarkPhrases.store(phrases));
  }

  @Test
  void rejectsNinthPhraseAndLongPhrase() {
    assertThrows(
        BizException.class,
        () -> RemarkPhrases.normalize(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9")));
    assertThrows(BizException.class, () -> RemarkPhrases.normalize(List.of("一二三四五六七八九")));
  }

  @Test
  void parseTruncatesCorruptStoredText() {
    List<String> phrases = RemarkPhrases.parse("放门口\n\n放门口\n一二三四五六七八九\n少葱");
    assertEquals(List.of("放门口", "一二三四五六七八", "少葱"), phrases);
    assertTrue(RemarkPhrases.parse("  ").isEmpty());
  }
}

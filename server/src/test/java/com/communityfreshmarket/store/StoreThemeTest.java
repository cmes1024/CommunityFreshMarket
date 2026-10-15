package com.communityfreshmarket.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.communityfreshmarket.common.BizException;
import org.junit.jupiter.api.Test;

class StoreThemeTest {

  @Test
  void acceptsIndustryPresetsAndClearsPrimaryUnlessCustom() {
    assertEquals("BREAKFAST", StoreTheme.preset(" breakfast "));
    assertEquals("PRODUCE", StoreTheme.preset("teal"));
    assertNull(StoreTheme.primary("PRODUCE", "#c2410c"));
    assertEquals("#c2410c", StoreTheme.primary("CUSTOM", "#C2410C"));
  }

  @Test
  void rejectsUnknownPresetAndCustomWithoutHex() {
    assertThrows(BizException.class, () -> StoreTheme.preset("PINK"));
    assertThrows(BizException.class, () -> StoreTheme.primary("CUSTOM", "red"));
  }

  @Test
  void readFallsBackWhenStoredValueIsCorrupt() {
    assertEquals("PRODUCE", StoreTheme.readPreset(" "));
    assertEquals("PRODUCE", StoreTheme.readPreset("PINK"));
    assertEquals("MARKET", StoreTheme.readPreset("ink"));
    assertNull(StoreTheme.readPrimary("CUSTOM", "red"));
    assertNull(StoreTheme.readPrimary("MARKET", "#2757a5"));
  }
}

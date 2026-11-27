package com.radaeli.ruilib.ui;
import org.junit.Test;
import static org.junit.Assert.*;
public class WorkshopThemeTest {
 @Test public void legacyConstructorRetainsDefaultPainter(){assertSame(UiSkin.DEFAULT,new UiTheme(UiTheme.DARK.palette(),UiTheme.DARK.tokens()).skin());}
 @Test public void workshopUsesNativeMetricsWithoutChangingOldThemes(){
  assertEquals(1,UiTheme.WORKSHOP.tokens().typography().smallScale(),0);
  assertEquals(0,UiTheme.WORKSHOP.tokens().motion().transitionDurationMillis());
  assertEquals(3,UiTheme.WORKSHOP.skin().metrics().frameInset());
  assertFalse(UiTheme.LIGHT.skin().pixelated());assertTrue(UiTheme.WORKSHOP.skin().pixelated());
 }
}

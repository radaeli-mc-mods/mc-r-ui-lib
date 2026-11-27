package com.radaeli.ruilib.ui;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.Objects;
/** Stateful presentation without an implicit toggle; the owner supplies selection. */
public final class IconButton extends UiComponent {
 public enum Symbol { LOCK_OPEN, LOCK_CLOSED }
 private Component accessibleName; private UiIcon icon; private final Runnable action;private boolean pressed,selected; private Symbol legacySymbol;
 public IconButton(Component name,Component glyph,Runnable action){this(name,UiIcon.glyph(glyph),action);}
 public IconButton(Component name,Symbol symbol,Runnable action){this(name,Objects.requireNonNull(symbol)==Symbol.LOCK_OPEN?UiIcons.LOCK_OPEN:UiIcons.LOCK_CLOSED,action);legacySymbol=symbol;}
 public IconButton(Component name,UiIcon icon,Runnable action){setAccessibleName(name);this.icon=Objects.requireNonNull(icon);this.action=Objects.requireNonNull(action);}
 public Component accessibleName(){return accessibleName;}
 public IconButton setIcon(UiIcon icon){this.icon=Objects.requireNonNull(icon);legacySymbol=null;return this;}
 public IconButton setAccessibleName(Component name){if(Objects.requireNonNull(name).getString().isBlank())throw new IllegalArgumentException("IconButton requires a name");accessibleName=name;tooltip(name);return this;}
 public IconButton setSelected(boolean selected){this.selected=selected;return this;}public boolean isSelected(){return selected;}
 @Override protected UiSize measureIntrinsic(Font f,UiTheme t,int w,int h){int size=Math.max(t.tokens().buttonMinimumHeight(),icon.width(f,t)+t.tokens().buttonPadding().left()+t.tokens().buttonPadding().right());return new UiSize(size,Math.max(t.tokens().buttonMinimumHeight(),icon.height(f,t)+6)).constrain(w,h);}
 @Override public void render(UiContext c){boolean hover=isEnabled()&&bounds().contains(c.mouseX(),c.mouseY());
  if(!c.theme().skin().button(c,bounds(),new UiVisualState(isEnabled(),hover?1:0,pressed?1:0,selected?1:0,isFocused()?1:0))) {
   if(selected)UiPaint.chip(c,bounds(),true,hover,isEnabled(),isFocused());else UiPaint.button(c,bounds(),animate(c,"hover",hover?1:0),animate(c,"press",pressed?1:0),isEnabled(),animate(c,"focus",isFocused()?1:0));
  }
  if(legacySymbol!=null&&!c.theme().skin().pixelated()){
   int x=bounds().centerX()-5,y=bounds().centerY()-5;
   int color=isEnabled()?c.theme().palette().text():c.theme().palette().mutedText();
   UiPaint.outline(c,new Bounds(x,y+4,10,7),color);UiPaint.outline(c,new Bounds(x+2,y,6,6),color);
   if(legacySymbol==Symbol.LOCK_OPEN)UiPaint.rect(c,new Bounds(x+6,y+2,2,4),c.theme().palette().button());
   UiPaint.rect(c,new Bounds(x+4,y+7,2,2),color);return;
  }
  int d=c.theme().skin().pixelated()&&pressed&&isEnabled()?1:0;
  c.graphics().enableScissor(bounds().x(),bounds().y(),bounds().right(),bounds().bottom());
  try{icon.render(c,bounds().centerX()-icon.width(c.font(),c.theme())/2+d,bounds().centerY()-icon.height(c.font(),c.theme())/2+d,c.theme().skin().buttonText(c.theme(),isEnabled(),selected));}finally{c.graphics().disableScissor();}
 }
 @Override public boolean mouseClicked(double x,double y,int b){if(!isEnabled()||b!=0||!bounds().contains(x,y))return false;pressed=true;setFocused(true);return true;}
 @Override public boolean mouseReleased(double x,double y,int b){if(!pressed||b!=0)return false;pressed=false;if(isEnabled()&&bounds().contains(x,y))action.run();return true;}
 @Override public boolean keyPressed(int k,int s,int m){if(!isEnabled()||!isFocused()||(k!=257&&k!=32&&k!=335))return false;action.run();return true;}
 @Override public boolean isFocusable(){return true;}
}

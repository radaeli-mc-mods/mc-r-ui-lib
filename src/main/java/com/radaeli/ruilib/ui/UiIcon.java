package com.radaeli.ruilib.ui;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.radaeli.ruilib.platform.UiPlatform;
/** An unscaled icon; render coordinates are logical GUI pixels. */
public interface UiIcon {
 int width(Font f,UiTheme theme); int height(Font f,UiTheme theme);
 void render(UiContext c,int x,int y,int color);
 static UiIcon glyph(Component label){java.util.Objects.requireNonNull(label);return new UiIcon(){
  public int width(Font f,UiTheme t){return UiText.width(f,label,t);}
  public int height(Font f,UiTheme t){return UiText.height(f,t);}
  public void render(UiContext c,int x,int y,int color){UiPaint.text(c,label,x,y,color);}
 };}
 static UiIcon texture(ResourceLocation texture,int u,int v,int width,int height,int textureWidth,int textureHeight){
  java.util.Objects.requireNonNull(texture);
  if(u<0||v<0||width<1||height<1||u+width>textureWidth||v+height>textureHeight)throw new IllegalArgumentException("Icon outside texture");
  return new UiIcon(){public int width(Font f,UiTheme t){return width;}public int height(Font f,UiTheme t){return height;}
   public void render(UiContext c,int x,int y,int color){UiPlatform.icon(c.graphics(),texture,x,y,u,v,width,height,textureWidth,textureHeight,UiPaint.withAlpha(color,c.opacity()));}
  };
 }
}

package com.radaeli.ruilib.ui;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.Objects;
/** Native styled text with explicit overflow; legacy defaults remain unbounded. */
public final class Label extends UiComponent {
 private Component text; private final Integer color; private UiTextStyle style=UiTextStyle.BODY;
 private UiText.Overflow overflow=UiText.Overflow.VISIBLE; private int maxLines=1; private boolean truncated;
 public Label(Component text){this(text,null);}
 public Label(Component text,int color){this(text,Integer.valueOf(color));}
 private Label(Component text,Integer color){this.text=Objects.requireNonNull(text);this.color=color;}
 public void setText(Component text){this.text=Objects.requireNonNull(text);}
 public Label setStyle(UiTextStyle style){this.style=Objects.requireNonNull(style);return this;}
 public Label setOverflow(UiText.Overflow overflow,int maxLines){if(maxLines<1)throw new IllegalArgumentException("maxLines must be positive");this.overflow=Objects.requireNonNull(overflow);this.maxLines=maxLines;return this;}
 @Override public Tooltip tooltip(){return super.tooltip()!=null?super.tooltip():truncated?Tooltip.of(text):null;}
 @Override protected UiSize measureIntrinsic(Font f,UiTheme t,int w,int h){
  int lines=overflow==UiText.Overflow.WRAP?Math.min(maxLines,f.split(text,Math.max(1,(int)(w/t.tokens().typography().scale(style)))).size()):1;
  return new UiSize(UiText.width(f,text,t,style),Math.max(1,lines)*UiText.height(f,t,style)).constrain(w,h);
 }
 @Override public void render(UiContext c){
  int col=color==null?c.theme().palette().text():color;int lineHeight=UiText.height(c.font(),c.theme(),style);
  int fontWidth=Math.max(0,(int)(bounds().width()/c.theme().tokens().typography().scale(style)));
  truncated=c.font().width(text)>fontWidth;
  if(overflow==UiText.Overflow.VISIBLE){UiPaint.text(c,text,bounds().x(),bounds().y(),col,style);return;}
  c.graphics().enableScissor(bounds().x(),bounds().y(),bounds().right(),bounds().bottom());
  try {
   if(overflow==UiText.Overflow.WRAP){var lines=c.font().split(text,Math.max(1,fontWidth));truncated=lines.size()>maxLines;
    for(int i=0;i<Math.min(maxLines,lines.size());i++)UiPaint.text(c,lines.get(i),bounds().x(),bounds().y()+i*lineHeight,col,style);
   }else if(overflow==UiText.Overflow.ELLIPSIS)UiPaint.text(c,UiText.ellipsize(c.font(),text,fontWidth),bounds().x(),bounds().y(),col,style);
   else UiPaint.text(c,text,bounds().x(),bounds().y(),col,style);
  }finally{c.graphics().disableScissor();}
 }
}

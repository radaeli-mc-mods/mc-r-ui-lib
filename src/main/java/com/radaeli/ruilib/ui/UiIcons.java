package com.radaeli.ruilib.ui;
import net.minecraft.client.gui.Font;
/** Original pixel symbols shared by consumers and playground examples. */
public enum UiIcons implements UiIcon {
CLOSE("10001","01010","00100","01010","10001"),
CONFIRM("0000001","0000010","1000100","0101000","0010000"),
PLUS("00100","00100","11111","00100","00100"),
MINUS("11111"),
LEFT("001","010","100","010","001"),
RIGHT("100","010","001","010","100"),
UP("00100","01010","10001"),
DOWN("10001","01010","00100"),
QUEUE("1111100","0000000","1111100","0000000","1111100","0000010","0000111"),
PLAY("10000","11000","11100","11110","11100","11000","10000"),
PAUSE("11011","11011","11011","11011","11011","11011","11011"),
PREVIOUS("10001","10011","10111","11111","10111","10011","10001"),
NEXT("10001","11001","11101","11111","11101","11001","10001"),
SHUFFLE("1000011","0100010","0010100","0001000","0010100","0100010","1000011"),
SAVE("1111111","1010101","1010101","1000001","1011101","1011101","1111111"),
HISTORY("0111100","1100010","1001001","1001101","1000001","0100010","0011100"),
SETTINGS("0010100","0111110","1100011","0101010","1100011","0111110","0010100"),
SEARCH("0111000","1000100","1000100","0111000","0000100","0000010","0000001"),
LOCK_CLOSED("01110","01010","01010","11111","11011","11011","11111"),
LOCK_OPEN("00111","00101","00100","11111","11011","11011","11111");
 private final String[] pixels;UiIcons(String... pixels){this.pixels=pixels;}
 public int width(Font f,UiTheme t){return pixels[0].length();}public int height(Font f,UiTheme t){return pixels.length;}
 public void render(UiContext c,int x,int y,int color){for(int row=0;row<pixels.length;row++)for(int col=0;col<pixels[row].length();col++)if(pixels[row].charAt(col)=='1')UiPaint.rect(c,new Bounds(x+col,y+row,1,1),color);}
}

package com.yjw.vultr;
public final class IndicatorState {
 public static final long FRAME_MS=50;
 public static String style(String value){return "rack".equals(value)?"rack":"pulse";}
 public static String label(String value){return style(value).equals("rack")?"微型机架":"呼吸圆点";}
 public static boolean animate(boolean running,boolean motion,boolean foreground,boolean visible,boolean systemEnabled,boolean interactive){return running&&motion&&foreground&&visible&&systemEnabled&&interactive;}
 private IndicatorState(){}
}

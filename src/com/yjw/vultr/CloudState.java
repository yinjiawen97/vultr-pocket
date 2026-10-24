package com.yjw.vultr;
public final class CloudState {
 public static final long INTERVAL=Refresher.CLOUD;
 public static boolean fresh(long checked,long now){return checked>0&&now>=checked&&now-checked<=INTERVAL;}
 public static boolean healthy(int health,int admin,int subscription){return health==200&&admin==200&&subscription==200;}
 public static String stage(int code){if(code==200)return "通过";if(code==0)return "尚未检测";if(code==-1)return "连接失败，服务状态未知";if(code==-2)return "响应格式不符合要求";if(code==401||code==403)return "权限验证失败 · HTTP "+code;if(code==404)return "地址不存在或尚未发布 · HTTP 404";return "服务返回 HTTP "+code;}
 public static String summary(boolean configured,long checked,long now,int health,int admin,int subscription){if(!configured)return "未配置固定入口";if(checked==0)return "等待首次检测";if(!fresh(checked,now))return "记录已过期 · 等待刷新";return healthy(health,admin,subscription)?"固定订阅检测通过":"检测未全部通过 · 查看详情";}
 private CloudState(){}
}

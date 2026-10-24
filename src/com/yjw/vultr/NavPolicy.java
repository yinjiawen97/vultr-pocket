package com.yjw.vultr;
public final class NavPolicy {
 public static final String[] TABS={"总览","服务器","订阅","费用","设置"};
 public static String section(String value){if(value==null)return "总览";switch(value){case "首页":case "总览":return "总览";case "部署":case "管理":case "部署记录":case "服务器详情":case "库存与方案":case "服务器":return "服务器";case "固定订阅":case "固定入口设置":case "合并订阅":case "订阅":return "订阅";case "费用":case "设置":return value;default:return "总览";}}
 public static boolean owns(long started,long current){return started==current;}
 private NavPolicy(){}
}

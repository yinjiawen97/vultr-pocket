package com.yjw.vultr;
import java.util.*;
import java.io.IOException;
/** Transaction over the existing complete fixed subscription, never a single-node substitute. */
public final class RoutingUpdate {
 public interface Endpoint {Map<String,Object> read()throws Exception;void write(Map<String,Object> config)throws Exception;}
 public interface Backup {void save(Map<String,Object> config)throws Exception;}
 public interface Validator {void check(Map<String,Object> config)throws Exception;}
 public static void apply(Endpoint endpoint,Backup backup,Validator validator,Map<String,Object> next)throws Exception{
  validator.check(next);Map<String,Object> before=endpoint.read();Configs.nodes(before);backup.save(before);
  try{endpoint.write(next);if(!next.equals(endpoint.read()))throw new IOException("发布回读内容不一致");}
  catch(Exception failed){try{Map<String,Object> observed=null;try{observed=endpoint.read();}catch(Exception ignored){}if(!before.equals(observed)){try{endpoint.write(before);}catch(Exception ignored){}if(!before.equals(endpoint.read()))throw new IOException();}}catch(Exception rollback){throw new IOException("更新失败，自动恢复未确认；已保存加密备份，请恢复后复查");}throw new IOException("更新未通过，已核对原固定订阅保持或恢复成功");}
 }
}

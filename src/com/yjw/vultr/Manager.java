package com.yjw.vultr;
import android.content.Context;
import android.util.Base64;
import org.json.*;
import java.util.*;
import java.io.*;
import java.security.SecureRandom;
final class Manager {
 static final java.util.concurrent.ExecutorService IO=java.util.concurrent.Executors.newSingleThreadExecutor();
 // Read-only refreshes run here in parallel; any write to data goes through commit()/synchronized(this). Never lock from the UI thread.
 static final java.util.concurrent.ExecutorService NET=java.util.concurrent.Executors.newFixedThreadPool(3,r->{Thread t=new Thread(r,"vultr-net");t.setDaemon(true);return t;});
 interface Change{void run()throws Exception;}
 private static Manager shared;
 static synchronized Manager get(Context c)throws Exception{if(shared==null)shared=new Manager(c.getApplicationContext());return shared;}
 final Context context; final Vault vault; final JSONObject data;
 Manager(Context c)throws Exception{context=c;vault=new Vault(c);data=vault.read();}
 Api api(){return new Api(data.optString("key"));}
 synchronized void save()throws Exception{vault.write(data);}
 synchronized void commit(Change change)throws Exception{change.run();save();}
 JSONArray refreshInstances()throws Exception{JSONArray all=api().list("/instances","instances");synchronized(this){syncInstances(all);JSONArray safe=ServerScreen.safe(all,this);data.put("home_snapshot",safe).put("home_checked",System.currentTimeMillis());save();return safe;}}
 JSONArray records()throws Exception{return data.getJSONArray("records");}
 static String id(JSONObject item)throws Exception{return UUID.fromString(item.getString("id")).toString();}
 static String hex(){byte[] bytes=new byte[16];new SecureRandom().nextBytes(bytes);StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format("%02x",x&255));return b.toString();}
 String asset(String name)throws Exception{return Api.read(context.getAssets().open(name),1024*1024);}
 String config(JSONObject record,String ip)throws Exception{String template=asset("config.yaml");String expanded=template.replace("REPLACE_PASSWORD",record.getString("password")).replace("REPLACE_SERVER_IP",ip);return Configs.yaml(Configs.routing(Configs.parse(template),Configs.parse(expanded)));}
 String verificationConfig(JSONObject record,String ip)throws Exception{return Configs.verification(record.optString("verification_config"),asset("config-legacy-v1.yaml").replace("REPLACE_PASSWORD",record.getString("password")),ip);}
 String bootstrap(JSONObject record)throws Exception{
  record.put("verification_config",config(record,"REPLACE_SERVER_IP")).put("routing_version",2);
  record.put("ssh_username",ServerLogin.deployUser(data.optString("ssh_username"))).put("ssh_password",ServerLogin.newPassword());
  JSONObject p=new JSONObject().put("ssh_username",record.getString("ssh_username")).put("ssh_password",record.getString("ssh_password")).put("password",record.getString("password")).put("token",record.getString("token")).put("yaml",config(record,"REPLACE_SERVER_IP"));
  String sh=asset("bootstrap.sh").replace("__PAYLOAD__",Base64.encodeToString(p.toString().getBytes("UTF-8"),2));return Base64.encodeToString(sh.getBytes("UTF-8"),2);
 }
 static String now(){return java.time.Instant.now().toString();}
 void remember(JSONObject r,JSONObject instance)throws Exception{remember(r,instance,true);}
 void remember(JSONObject r,JSONObject instance,boolean detail)throws Exception{
  ServerLogin.remember(data,instance,System.currentTimeMillis(),detail);
  for(String field:new String[]{"date_created","region","plan","os","status","power_status","server_status","user_scheme","username","vcpu_count","ram","disk","allowed_bandwidth"})if(instance.has(field)&&!instance.isNull(field))r.put(field,instance.get(field));
  r.put("ip",instance.optString("main_ip")).put("cloud",instance.optString("status")+" / "+instance.optString("power_status")).put("checked_at",now());
 }
 void syncInstances(JSONArray instances)throws Exception{
  for(int i=0;i<records().length();i++){JSONObject r=records().getJSONObject(i);for(int j=0;j<instances.length();j++){JSONObject x=instances.getJSONObject(j);if(!r.optString("id").isEmpty()&&r.optString("id").equals(x.optString("id")))remember(r,x,false);}}
  for(int i=0;i<instances.length();i++)ServerLogin.remember(data,instances.getJSONObject(i),System.currentTimeMillis(),false);
  ServerPower.reconcile(data.optJSONObject("power_pending"),instances);save();
 }
 JSONObject refreshInfo(JSONObject r)throws Exception{
  if(r.optString("id").isEmpty())throw new IOException("请求尚无实例 ID，请用检查安装继续对账。");
  JSONObject x=api().call("GET","/instances/"+id(r),null).getJSONObject("instance");synchronized(this){remember(r,x);save();}return r;
 }
 JSONObject existing(boolean another)throws Exception{
  JSONArray a=records();for(int i=a.length()-1;i>=0;i--){JSONObject r=a.getJSONObject(i);if(Core.uncertain(r.optString("phase")))return r;}
  if(!another)for(int i=a.length()-1;i>=0;i--){JSONObject r=a.getJSONObject(i);if(Core.inactive(r.optString("phase"))||r.optString("id").isEmpty())continue;
   try{api().call("GET","/instances/"+id(r),null);return r;}catch(Api.Error e){if(e.code!=404)throw e;r.put("phase","deleted");save();}
  }return null;
 }
 ServerPower.Result power(String target,String action)throws Exception{
  Api client=api();return ServerPower.execute((method,path,body)->client.call(method,path,body),target,action,new ServerPower.Pending(){
   public void begin(String id,String mode)throws Exception{JSONObject all=data.optJSONObject("power_pending");if(all==null){all=new JSONObject();data.put("power_pending",all);}Object previous=all.opt(id);all.put(id,new JSONObject().put("action",mode).put("sent",System.currentTimeMillis()));try{save();}catch(Exception e){if(previous==null)all.remove(id);else all.put(id,previous);throw e;}}
   public void clear(String id)throws Exception{JSONObject all=data.optJSONObject("power_pending");if(all==null)return;Object previous=all.remove(id);try{save();}catch(Exception e){if(previous!=null)all.put(id,previous);throw e;}}
  });
 }
 JSONObject plan()throws Exception{return plan(data.optString("region","sjc"),data.optString("plan","vc2-1c-1gb"),data.optString("cap","5"));}
 JSONObject plan(String region,String plan,String cap)throws Exception{
  if(!region.matches("[a-z]{3}"))throw new IOException("地区应为 sjc 等三位小写字母。");
  JSONArray plans=api().list("/plans","plans");JSONObject chosen=null;
  for(int i=0;i<plans.length();i++)if(plans.getJSONObject(i).getString("id").equals(plan))chosen=plans.getJSONObject(i);
  if(chosen==null)throw new IOException("未找到此套餐，请检查设置。");
  if(!Inventory.supported(chosen))throw new IOException("此套餐不支持当前自动部署：需要按需 x64 云服务器、IPv4 和至少 1 GB 内存。");
  if(!Inventory.located(chosen,region))throw new IOException("此套餐不在所选地区销售。");
  if(!Core.budget(Inventory.price(chosen,region),cap))throw new IOException("套餐月费超过 $"+cap+" 上限，未创建。");
  JSONArray stock=api().call("GET","/regions/"+region+"/availability",null).getJSONArray("available_plans");boolean available=false;for(int i=0;i<stock.length();i++)available|=stock.getString(i).equals(plan);
  if(!available)throw new IOException("该地区当前没有此套餐库存，未创建。");
  JSONArray systems=api().list("/os","os");int os=-1;for(int i=0;i<systems.length();i++){JSONObject s=systems.getJSONObject(i);if(s.optString("name").equals("Ubuntu 24.04 LTS x64")&&s.optString("arch").equals("x64"))os=s.getInt("id");}
  if(os<0)throw new IOException("找不到 Ubuntu 24.04 x64，未创建。");
  return new JSONObject().put("region",region).put("plan",plan).put("os_id",os).put("price",Inventory.price(chosen,region).toPlainString()).put("cap",cap);
 }
 JSONObject create(JSONObject confirmed)throws Exception{
  if(existing(true)!=null)throw new IOException("已有结果不确定的请求，请先继续核对。");
  JSONObject fresh=plan(confirmed.getString("region"),confirmed.getString("plan"),confirmed.getString("cap"));if(!fresh.toString().equals(confirmed.toString()))throw new IOException("方案或价格已变化，请重新预览确认。");
  String tag="vp-"+UUID.randomUUID().toString();JSONObject r=new JSONObject().put("tag",tag).put("label","clash-"+fresh.getString("region")+"-"+System.currentTimeMillis()).put("password",hex()).put("token",hex()).put("phase","create-pending").put("requested_at",now()).put("mode","new");
  String script=bootstrap(r);
  records().put(r);save(); // Durable marker BEFORE the only POST; never replay an uncertain request.
  JSONObject payload=new JSONObject().put("region",fresh.getString("region")).put("plan",fresh.getString("plan")).put("os_id",fresh.getInt("os_id")).put("label",r.getString("label")).put("hostname","clash-node").put("tags",new JSONArray().put(tag)).put("backups","disabled").put("enable_ipv6",false).put("disable_public_ipv4",false).put("activation_email",false).put("ddos_protection",false).put("user_data",script);
  try{JSONObject item=api().call("POST","/instances",payload).getJSONObject("instance");r.put("id",id(item)).put("phase","created");remember(r,item);save();return r;}
  catch(Api.Error e){if(e.code==400||e.code==401||e.code==403||e.code==404||e.code==422){r.put("phase","create-rejected");save();}throw e;}
 }
 JSONObject check(JSONObject r)throws Exception{
  if(r.optString("id").isEmpty()){
   JSONArray all=api().list("/instances","instances");JSONObject match=null;
   for(int i=0;i<all.length();i++){JSONObject x=all.getJSONObject(i);JSONArray tags=x.optJSONArray("tags");if(tags!=null)for(int j=0;j<tags.length();j++)if(tags.getString(j).equals(r.optString("tag"))){if(match!=null)throw new IOException("发现多个匹配实例，请到控制台核对。");match=x;}}
   if(match==null)throw new IOException("尚未找到该请求对应的实例；为防重复收费不会重发，请稍后继续核对或查看控制台。");r.put("id",id(match)).put("phase","created");save();
  }
  JSONObject instance;
  try{instance=api().call("GET","/instances/"+id(r),null).getJSONObject("instance");}catch(Api.Error e){if(e.code==404){r.put("phase","deleted");save();}throw e;}
  String ip=instance.optString("main_ip");remember(r,instance);save();
  if(!ip.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")||ip.equals("0.0.0.0"))return r;
  try{
   String url="http://"+ip+"/sub/"+r.getString("token");String body=Api.subscription(url);
   if(body.equals(verificationConfig(r,ip))){r.put("url",url).put("phase","subscription-ready");if(!r.has("ready_verified_at"))r.put("ready_verified_at",now());r.remove("note");ServerLogin.initialized(data,r,System.currentTimeMillis());save();new Book(this).sync();CloudPublisher.latest(this,r);}else r.put("note","订阅内容不符，尚未通过核验。");
  }catch(IOException e){r.put("note","订阅未就绪或手机网络不可达；稍后继续检查。不能据此判断 IP 被封。");}
  save();return r;
 }
 JSONObject managed(String instanceId)throws Exception{for(int i=records().length()-1;i>=0;i--){JSONObject r=records().getJSONObject(i);if(r.optString("id").equals(instanceId)&&!Core.inactive(r.optString("phase")))return r;}return null;}
 void delete(JSONObject instance)throws Exception{
  String target=id(instance);api().call("DELETE","/instances/"+target,null);
  try{api().call("GET","/instances/"+target,null);throw new IOException("删除已提交，尚未确认。请刷新列表核对，未确认前仍可能计费。");}catch(Api.Error e){if(e.code!=404)throw e;}
  for(int i=0;i<records().length();i++){JSONObject r=records().getJSONObject(i);if(r.optString("id").equals(target))r.put("phase","deleted");}ServerLogin.forget(data,target);save();
 }
 JSONObject reinstall(JSONObject item)throws Exception{
  if(existing(true)!=null)throw new IOException("有未完成请求，请先继续核对。");
  String target=id(item);JSONObject latest=api().call("GET","/instances/"+target,null).getJSONObject("instance");
  if(!latest.optString("os").equals("Ubuntu 24.04 LTS x64"))throw new IOException("手机版仅支持重装 Ubuntu 24.04 LTS x64 实例。");
  JSONObject r=new JSONObject().put("id",target).put("label",latest.optString("label")).put("password",hex()).put("token",hex()).put("phase","reinstall-preparing").put("requested_at",now()).put("mode","reinstall");remember(r,latest);String script=bootstrap(r);records().put(r);save();
  api().call("PATCH","/instances/"+target,new JSONObject().put("user_data",script));r.put("phase","reinstall-pending");save();
  api().call("POST","/instances/"+target+"/reinstall",new JSONObject().put("hostname","clash-node"));r.put("phase","created");r.remove("checked_at");ServerLogin.forget(data,target);save();return r;
 }
}

package com.yjw.vultr;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import org.json.JSONObject;
final class SubscriptionActions {
 static Intent intent(String url,String target){SubscriptionLinks.clientName(target);return new Intent(Intent.ACTION_VIEW,Uri.parse(SubscriptionLinks.flclash(url))).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);}
 static boolean accepts(MainActivity a,String url,String target){android.content.pm.ResolveInfo found=a.getPackageManager().resolveActivity(intent(url,target),android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);return found!=null&&found.activityInfo!=null&&found.activityInfo.exported&&target.equals(found.activityInfo.packageName);}
 static void missing(MainActivity a,JSONObject r){new AlertDialog.Builder(a).setTitle("未找到可接收导入的 FlClash").setMessage("支持 GitHub 官方版和 SumCheung 版。请安装支持订阅导入的客户端，或复制订阅地址手动添加。").setNegativeButton("取消",null).setPositiveButton("复制订阅地址",(d,w)->a.copySubscription(r)).show();}
 static void launch(MainActivity a,JSONObject r,String target){try{a.startActivity(intent(r.getString("url"),target));}catch(ActivityNotFoundException e){missing(a,r);}catch(Exception e){a.message("无法打开所选客户端，请核验订阅或复制地址手动添加。");}}
 static void flclash(MainActivity a,JSONObject r){
  try{if(Core.inactive(r.optString("phase")))throw new IllegalArgumentException("实例记录已失效，请先检查服务器。");String url=SubscriptionLinks.valid(r.getString("url"));String[] packages=SubscriptionLinks.clientPackages();String[] targets=SubscriptionLinks.clients(accepts(a,url,packages[0]),accepts(a,url,packages[1]));
   if(targets.length==0){missing(a,r);return;}if(targets.length==1){launch(a,r,targets[0]);return;}
   String[] labels=new String[targets.length];for(int i=0;i<targets.length;i++)labels[i]=SubscriptionLinks.clientName(targets[i])+"\n"+targets[i];new AlertDialog.Builder(a).setTitle("选择要导入的 FlClash").setItems(labels,(d,index)->launch(a,r,targets[index])).setNegativeButton("取消",null).show();
  }catch(Exception e){a.message("无法打开导入，请先核验订阅，或复制订阅地址手动添加。");}
 }
 static void desktop(MainActivity a,JSONObject r){new AlertDialog.Builder(a).setTitle("用于电脑上的 Clash Verge").setItems(new String[]{"复制订阅地址（电脑端粘贴）","分享订阅地址到电脑","复制 Clash Verge 专用导入链接"},(dialog,index)->{
  try{String url=SubscriptionLinks.valid(r.getString("url"));if(index==0)a.copySubscription(r);else if(index==1){Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,url).putExtra(Intent.EXTRA_SUBJECT,r.optString("label","Vultr 订阅"));a.startActivity(Intent.createChooser(share,"选择你自己的跨设备传输工具"));}else copy(a,SubscriptionLinks.verge(url,r.optString("label","Vultr 订阅")),"Clash Verge 导入链接已复制，在电脑端打开使用。");}
  catch(Exception e){a.message("无法生成分享内容，请先核验订阅地址。");}
 }).setNegativeButton("取消",null).show();}
 static void copy(MainActivity a,String text,String feedback){android.content.ClipData clip=android.content.ClipData.newPlainText("私密订阅链接",text);android.os.PersistableBundle extras=new android.os.PersistableBundle();extras.putBoolean("android.content.extra.IS_SENSITIVE",true);clip.getDescription().setExtras(extras);((android.content.ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);android.widget.Toast.makeText(a,feedback,android.widget.Toast.LENGTH_LONG).show();}
}

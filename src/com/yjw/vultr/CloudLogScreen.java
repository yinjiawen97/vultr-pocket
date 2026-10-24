package com.yjw.vultr;
import android.app.AlertDialog;
import android.content.*;
import android.os.PersistableBundle;
import android.widget.*;
import org.json.*;
final class CloudLogScreen {
 final MainActivity a;CloudLogScreen(MainActivity a){this.a=a;}
 void open(){a.enterDetail("Cloudflare 日志","订阅",()->render());}
 void render(){a.body.removeAllViews();JSONObject log=a.manager.data.optJSONObject("cloud_diagnostics");JSONArray rounds=log==null?null:log.optJSONArray("rounds");int count=rounds==null?0:rounds.length();LinearLayout intro=a.card("本机诊断日志",count+" / 100 轮 · 页面显示最近 20 轮 · 时间为 UTC");intro.addView(a.label("仅在本机加密保存。复制包含保留的全部记录和最近一次失败，不含密钥、私密订阅路径、响应内容或原始异常。VPN 存在不代表请求实际经由 VPN。",14,a.MUTED));a.pair(intro,"复制日志",()->{ClipData clip=ClipData.newPlainText("Cloudflare 诊断日志",CloudDiagnostics.report(a.manager.data.optJSONObject("cloud_diagnostics")));PersistableBundle flags=new PersistableBundle();flags.putBoolean("android.content.extra.IS_SENSITIVE",true);clip.getDescription().setExtras(flags);((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);a.toast("已复制诊断日志");},"清空日志",()->new AlertDialog.Builder(a).setTitle("清空本机诊断日志？").setMessage("只删除诊断记录和最近一次失败，不影响订阅、凭据或抽检统计。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->a.task("清空诊断日志…",()->{Object old=a.manager.data.remove("cloud_diagnostics");try{a.manager.save();}catch(Exception e){if(old!=null)a.manager.data.put("cloud_diagnostics",old);throw e;}a.ui(()->{render();a.toast("诊断日志已清空");});})).show());
  if(log!=null&&log.optJSONObject("last_failure")!=null)text(a.card("最近一次失败","后续检测成功仍保留此记录"),CloudDiagnostics.roundText(log.optJSONObject("last_failure")));
  if(count==0)a.card("尚无详细日志","更新前的抽检没有记录具体错误；下次手动、首页或后台检测时会自动记录。");
  if(rounds!=null)for(int i=count-1;i>=Math.max(0,count-20);i--){JSONObject x=rounds.optJSONObject(i);if(x!=null)text(a.card("检测记录", ""),CloudDiagnostics.roundText(x));}
 }
 void text(LinearLayout card,String value){TextView text=a.label(value,14,a.MUTED);text.setTextIsSelectable(true);card.addView(text);}
}

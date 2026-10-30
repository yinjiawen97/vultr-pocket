package com.yjw.vultr;
import org.json.*;
import android.widget.*;
import android.graphics.Typeface;
import java.math.BigDecimal;
final class AccountSummary {
 static boolean fresh(JSONObject value,long now){return AccountState.fresh(value,now);}
 static void fetch(Manager m)throws Exception{JSONObject account=m.api().call("GET","/account",null).getJSONObject("account");store(m,account);try{spend(m);}catch(Exception billing){/* Keep the last per-server spend; its own update time is shown beside it. */}}
 static void store(Manager m,JSONObject account)throws Exception{JSONObject safe=AccountState.from(account,System.currentTimeMillis());m.commit(()->m.data.put("account_summary",safe));}
 // Invoices never change, so their items are kept by id; only invoices issued after the oldest current server are read.
 static void spend(Manager m)throws Exception{
  JSONArray servers=m.data.optJSONArray("home_snapshot");if(servers==null||servers.length()==0)return;String oldest=null;for(int i=0;i<servers.length();i++){String c=servers.getJSONObject(i).optString("date_created");if(!c.isEmpty()&&(oldest==null||Core.overlaps(oldest,c)))oldest=c;}if(oldest==null)return;
  Api api=m.api();JSONArray items=new JSONArray(),pending=api.list("/billing/pending-charges","pending_charges");for(int i=0;i<pending.length();i++)items.put(slim(pending.getJSONObject(i)));
  JSONObject known=m.data.optJSONObject("invoice_items"),kept=new JSONObject();JSONArray invoices=api.list("/billing/invoices","billing_invoices");for(int n=0;n<invoices.length();n++){JSONObject inv=invoices.getJSONObject(n);String id=inv.optString("id");if(!id.matches("[0-9]+")||!Core.overlaps(inv.optString("date"),oldest))continue;JSONArray rows=known==null?null:known.optJSONArray(id);if(rows==null){rows=new JSONArray();JSONArray raw=api.list("/billing/invoices/"+id+"/items","invoice_items");for(int i=0;i<raw.length();i++)rows.put(slim(raw.getJSONObject(i)));}kept.put(id,rows);for(int i=0;i<rows.length();i++)items.put(rows.get(i));}
  JSONObject by=new JSONObject();for(int i=0;i<servers.length();i++){JSONObject s=servers.getJSONObject(i);by.put(s.optString("id"),Spend.total(items,s).toPlainString());}
  JSONObject out=new JSONObject().put("checked",System.currentTimeMillis()).put("by",by);m.commit(()->m.data.put("invoice_items",kept).put("server_spend",out));
 }
 static JSONObject slim(JSONObject r)throws Exception{return new JSONObject().put("product",r.optString("product")).put("description",r.optString("description")).put("end_date",r.optString("end_date")).put("total",r.opt("total"));}
 // Each current server: time since it was created and what Vultr has billed for it so far (invoiced + pending).
 static void servers(MainActivity a,LinearLayout c){JSONArray all=a.manager.data.optJSONArray("home_snapshot");if(all==null||all.length()==0)return;JSONObject spend=a.manager.data.optJSONObject("server_spend"),by=spend==null?null:spend.optJSONObject("by");java.time.Instant now=java.time.Instant.now();
  for(int i=0;i<Math.min(3,all.length());i++){JSONObject x=all.optJSONObject(i);if(x==null)continue;String cost=by!=null&&by.has(x.optString("id"))?Core.cash(Core.money(by.optString(x.optString("id")))):"—";a.fact(c,x.optString("label","未命名服务器"),"已运行 "+Spend.uptime(x.optString("date_created"),now)+" · 已花费 "+cost);}
  if(spend!=null)c.addView(a.tnum(a.label("花费按 Vultr 账单（含未出账）· "+Core.shortTime(spend.optLong("checked"))+" 更新",12,a.MUTED)));}
 static void card(MainActivity a,String error,boolean showLink){
  JSONObject s=a.manager.data.optJSONObject("account_summary");BigDecimal credit=null,pending=null,available=null,shortfall=null;
  try{if(s!=null){credit=AccountState.credit(s);pending=Core.money(s.get("pending"));available=AccountState.available(s);shortfall=AccountState.shortfall(s);}}catch(Exception invalid){available=null;}
  boolean known=available!=null,insufficient=known&&shortfall.signum()>0;LinearLayout c=a.card("","");TextView head=a.label("剩余可用 · USD",13,a.MUTED);head.setTypeface(MainActivity.MEDIUM);c.addView(head);TextView amount=a.tnum(a.label(known?Core.cash(available):"—",32,insufficient?a.DANGER:a.INK));amount.setTypeface(MainActivity.MEDIUM);amount.setLetterSpacing(-0.02f);c.addView(amount);
  if(insufficient)a.pill(c,"余额不足 · 待补 "+Core.cash(shortfall),a.DANGER);
  String stamp=known?(fresh(s,System.currentTimeMillis())?"":"缓存 · ")+Core.shortTime(s.optLong("checked"))+" 更新":"尚未获取完整金额 · 点击顶部刷新";
  servers(a,c);
  if(showLink){c.addView(a.tnum(a.label(known?"结余 "+Core.cash(credit)+"  ·  未出账 "+Core.cash(pending):"结余 —  ·  未出账 —",13,a.MUTED)));c.addView(a.tnum(a.label(error.isEmpty()?stamp:error,12,error.isEmpty()?a.MUTED:a.AMBER)));a.row(c,"查看完整费用","",()->a.page("费用"));return;}
  a.fact(c,"账户结余（扣费前）",known?Core.cash(credit):"尚未获取");a.fact(c,"未出账费用",known?Core.cash(pending):"尚未获取");
  String note="通常次月 1 日出账（UTC），实际扣款依付款设置。上方已扣除未出账费用，出账时不重复计算。";if(!error.isEmpty())note+="\n"+error;note+="\n"+stamp;c.addView(a.label(note,12,a.MUTED));
 }
}

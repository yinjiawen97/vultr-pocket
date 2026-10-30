package com.yjw.vultr;
import org.json.*;
import android.widget.*;
import android.graphics.Typeface;
import java.math.BigDecimal;
final class AccountSummary {
 static boolean fresh(JSONObject value,long now){return AccountState.fresh(value,now);}
 static void fetch(Manager m)throws Exception{JSONObject account=m.api().call("GET","/account",null).getJSONObject("account");store(m,account);}
 static void store(Manager m,JSONObject account)throws Exception{JSONObject safe=AccountState.from(account,System.currentTimeMillis());m.commit(()->m.data.put("account_summary",safe));}
 static void card(MainActivity a,String error,boolean showLink){
  JSONObject s=a.manager.data.optJSONObject("account_summary");BigDecimal credit=null,pending=null,available=null,shortfall=null;
  try{if(s!=null){credit=AccountState.credit(s);pending=Core.money(s.get("pending"));available=AccountState.available(s);shortfall=AccountState.shortfall(s);}}catch(Exception invalid){available=null;}
  boolean known=available!=null,insufficient=known&&shortfall.signum()>0;LinearLayout c=a.card("","");TextView head=a.label("剩余可用 · USD",13,a.MUTED);head.setTypeface(MainActivity.MEDIUM);c.addView(head);TextView amount=a.tnum(a.label(known?Core.cash(available):"—",32,insufficient?a.DANGER:a.INK));amount.setTypeface(MainActivity.MEDIUM);amount.setLetterSpacing(-0.02f);c.addView(amount);
  if(insufficient)a.pill(c,"余额不足 · 待补 "+Core.cash(shortfall),a.DANGER);
  String stamp=known?(fresh(s,System.currentTimeMillis())?"":"缓存 · ")+Core.shortTime(s.optLong("checked"))+" 更新":"尚未获取完整金额 · 点击顶部刷新";
  if(showLink){c.addView(a.tnum(a.label(known?"结余 "+Core.cash(credit)+"  ·  未出账 "+Core.cash(pending):"结余 —  ·  未出账 —",13,a.MUTED)));c.addView(a.tnum(a.label(error.isEmpty()?stamp:error,12,error.isEmpty()?a.MUTED:a.AMBER)));a.row(c,"查看完整费用","",()->a.page("费用"));return;}
  a.fact(c,"账户结余（扣费前）",known?Core.cash(credit):"尚未获取");a.fact(c,"未出账费用",known?Core.cash(pending):"尚未获取");
  String note="通常次月 1 日出账（UTC），实际扣款依付款设置。上方已扣除未出账费用，出账时不重复计算。";if(!error.isEmpty())note+="\n"+error;note+="\n"+stamp;c.addView(a.label(note,12,a.MUTED));
 }
}

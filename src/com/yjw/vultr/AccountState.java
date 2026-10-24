package com.yjw.vultr;
import org.json.*;
import java.math.BigDecimal;
public final class AccountState {
 public static JSONObject from(JSONObject account,long checked)throws Exception{return new JSONObject().put("balance",Core.money(account.get("balance")).toPlainString()).put("pending",Core.money(account.get("pending_charges")).toPlainString()).put("checked",checked);}
 // Keep the API ledger unchanged in storage; convert only for the wallet display.
 public static BigDecimal credit(JSONObject value)throws Exception{return Core.money(value.get("balance")).negate();}
 public static BigDecimal remaining(JSONObject value)throws Exception{return credit(value).subtract(Core.money(value.get("pending")));}
 public static BigDecimal available(JSONObject value)throws Exception{return remaining(value).max(BigDecimal.ZERO);}
 public static BigDecimal shortfall(JSONObject value)throws Exception{return remaining(value).negate().max(BigDecimal.ZERO);}
 public static boolean fresh(JSONObject value,long now){long t=value==null?0:value.optLong("checked");return t>0&&now>=t&&now-t<=Refresher.ACCOUNT;}
 private AccountState(){}
}

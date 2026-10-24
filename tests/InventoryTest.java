package com.yjw.vultr;
import org.json.*;
public class InventoryTest {
 static int n=0;static void check(boolean v){n++;if(!v)throw new AssertionError("inventory case "+n);}
 public static void main(String[] args)throws Exception{
  JSONObject p=new JSONObject("{\"id\":\"vc2-1c-1gb\",\"type\":\"vc2\",\"ram\":1024,\"vcpu_count\":1,\"disk\":25,\"bandwidth\":1024,\"monthly_cost\":5,\"deploy_ondemand\":true,\"locations\":[\"sjc\",\"sao\"],\"location_cost\":{\"sao\":{\"monthly_cost\":7.5}}}");
  check(Inventory.price(p,"sjc").toPlainString().equals("5"));check(Inventory.price(p,"sao").toPlainString().equals("7.5"));check(!Core.budget(Inventory.price(p,"sao"),"5"));
  check(Inventory.located(p,"sjc"));check(!Inventory.located(p,"nrt"));check(Inventory.supported(p));
  p.put("ram",512);check(!Inventory.supported(p));p.put("ram",1024).put("id","vc2-test-v6");check(!Inventory.supported(p));
  p.put("id","test").put("type","vcg");check(!Inventory.supported(p));p.put("type","vc2").put("deploy_ondemand",false);check(!Inventory.supported(p));
  check(Inventory.available(new JSONObject("{\"available_plans\":[]}")).isEmpty());
  check(Inventory.available(new JSONObject("{\"available_plans\":[\"vc2-1c-1gb\"],\"available_vpc_only_plans\":[\"vpc-only\"]}")).contains("vc2-1c-1gb"));
  check(!Inventory.available(new JSONObject("{\"available_plans\":[],\"available_vpc_only_plans\":[\"vpc-only\"]}")).contains("vpc-only"));
  boolean missing=false;try{Inventory.available(new JSONObject());}catch(JSONException e){missing=true;}check(missing);
  p.getJSONObject("location_cost").getJSONObject("sao").put("monthly_cost","invalid");boolean invalid=false;try{Inventory.price(p,"sao");}catch(IllegalArgumentException e){invalid=true;}check(invalid);
  System.out.println("PASS: "+n+" inventory checks");
 }
}

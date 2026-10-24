package com.yjw.vultr;
import android.widget.*;
import android.view.*;
import java.util.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import org.json.*;
final class StockScreen {
 final MainActivity a; final Manager m;
 JSONArray regions;Spinner region,family;EditText budget;CheckBox onlyStock,onlyBudget,another;LinearLayout results;int generation=0;
 final String[] types={"vc2","vhf","vhp","voc","vx1"};
 StockScreen(MainActivity a){this.a=a;m=a.manager;}
 void open(){a.enterDetail("库存与方案","服务器",()->loadRegions());}
 void loadRegions(){a.task("读取 Vultr 地区列表…",()->{JSONArray list=m.api().list("/regions","regions");a.ui(()->{regions=list;render();});});}
 Spinner spinner(LinearLayout parent,List<String> labels){Spinner s=new Spinner(a);ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,labels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(adapter);parent.addView(s,new LinearLayout.LayoutParams(-1,a.dp(54)));return s;}
 CheckBox toggle(LinearLayout parent,String text,boolean value){CheckBox c=new CheckBox(a);c.setText(text);c.setTextColor(a.INK);c.setTextSize(16);c.setMinHeight(a.dp(48));c.setChecked(value);parent.addView(c);return c;}
 void render()throws Exception{
  a.body.removeAllViews();LinearLayout c=a.card("选择你的服务器","先查库存，再选套餐。所有查询均免费，不会自动创建。");
  List<String> names=new ArrayList<>();int selected=0;
  for(int i=0;i<regions.length();i++){JSONObject r=regions.getJSONObject(i);String id=r.getString("id");if(id.equals(m.data.optString("region","sjc")))selected=i;String country=new Locale("",r.optString("country")).getDisplayCountry(Locale.SIMPLIFIED_CHINESE);names.add(country+" · "+r.optString("city")+"  ("+id+")");}
  if(names.isEmpty()){c.addView(a.label("没有返回可用地区，请稍后重试。",14,a.MUTED));return;}
  c.addView(a.label("1  选择地区",13,a.MUTED));region=spinner(c,names);region.setSelection(selected);
  c.addView(a.label("2  选择服务器系列",13,a.MUTED));family=spinner(c,Arrays.asList("经济型 Cloud Compute · vc2","高频计算 · vhf","高性能计算 · vhp","优化型计算 · voc","通用计算 · vx1"));
  budget=a.field(c,"基础月费上限（美元）",m.data.optString("cap","5"),false);budget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
  onlyStock=toggle(c,"只显示当前有库存的套餐",true);onlyBudget=toggle(c,"只显示月费上限内的套餐",false);another=toggle(c,"另建一台（保留原服务器并额外计费）",false);
  a.button(c,"查询实时库存",MainActivity.Tone.PRIMARY,()->query());
  a.helpCard("库存说明","只显示适合当前 Ubuntu 24.04 自动部署的按需云服务器（IPv4、至少 1 GB 内存），不含 GPU、裸金属或抢占式套餐。\nVultr 提供可部署套餐清单，不提供剩余台数；库存与价格都可能变化，创建前会再次核验。税费及超额流量另计。");
  results=new LinearLayout(a);results.setOrientation(1);a.body.addView(results);
  AdapterView.OnItemSelectedListener clear=new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){invalidate();}public void onNothingSelected(AdapterView<?> p){invalidate();}};
  region.setOnItemSelectedListener(clear);family.setOnItemSelectedListener(clear);onlyStock.setOnCheckedChangeListener((b,v)->invalidate());onlyBudget.setOnCheckedChangeListener((b,v)->invalidate());
  budget.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){invalidate();}public void afterTextChanged(android.text.Editable e){}});
 }
 void invalidate(){generation++;if(results!=null){results.removeAllViews();results.addView(a.label("选择条件后，点击“查询实时库存”。",14,a.MUTED));}}
 void query(){try{
  final String rid=regions.getJSONObject(region.getSelectedItemPosition()).getString("id"),title=region.getSelectedItem().toString(),type=types[family.getSelectedItemPosition()],cap=budget.getText().toString().trim();final boolean stockOnly=onlyStock.isChecked(),within=onlyBudget.isChecked();Core.budget("0",cap);final int ticket=++generation;
  ((android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(budget.getWindowToken(),0);
  results.removeAllViews();results.addView(a.label("正在查询 "+title+"…",14,a.MUTED));
  a.task("查询地区实时库存与价格…",()->{
   try{
    JSONArray plans=m.api().list("/plans","plans");Set<String> available=Inventory.available(m.api().call("GET","/regions/"+rid+"/availability",null));List<JSONObject> list=new ArrayList<>();int sold=0,inStock=0;
    for(int i=0;i<plans.length();i++){JSONObject p=plans.getJSONObject(i);if(!Inventory.supported(p)||!Inventory.located(p,rid)||!p.optString("type").equals(type))continue;sold++;boolean has=available.contains(p.getString("id"));if(has)inStock++;BigDecimal price=Inventory.price(p,rid);if((!stockOnly||has)&&(!within||Core.budget(price,cap)))list.add(new JSONObject(p.toString()).put("display_price",price.toPlainString()).put("in_stock",has));}
    list.sort((x,y)->{int order=Boolean.compare(y.optBoolean("in_stock"),x.optBoolean("in_stock"));return order!=0?order:Core.money(x.optString("display_price")).compareTo(Core.money(y.optString("display_price")));});
    final int count=sold,stock=inStock;final String stamp=new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date());
    a.ui(()->{if(ticket!=generation)return;results.removeAllViews();results.addView(a.label(title+"\n"+stamp+" 查询 · "+count+" 种在售 / "+stock+" 种有库存\n符合筛选："+list.size()+" 种",16,a.INK));if(list.isEmpty())results.addView(a.label("没有符合条件的套餐，可更换地区、系列或筛选条件。",14,a.MUTED));
     for(JSONObject p:list){LinearLayout row=new LinearLayout(a);row.setOrientation(1);row.setPadding(a.dp(20),a.dp(16),a.dp(20),a.dp(20));row.setBackground(UiSkin.shape(a.SURFACE,a.BORDER,a.dp(20)));LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=a.dp(16);results.addView(row,layout);
      boolean has=p.getBoolean("in_stock"),allowed=Core.budget(p.getString("display_price"),cap);row.addView(a.label(Core.cash(Core.money(p.get("display_price")))+" / 月 · "+(has?"● 有库存":"暂无库存"),19,has?a.GREEN:a.MUTED));row.addView(a.label(p.getString("id"),14,a.INK));row.addView(a.label(Inventory.specs(p),13,a.MUTED));if(!allowed)row.addView(a.label("超出当前 $"+cap+" 上限。需要调整上限后重新查询。",13,a.MUTED));
      String caption=!has?"暂无库存":!allowed?"超出月费上限":"选择此套餐";Button pick=a.actionButton(caption,MainActivity.Tone.PRIMARY);pick.setEnabled(has&&allowed);a.styleButton(pick,caption);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=a.dp(12);row.addView(pick,bp);pick.setOnClickListener(v->choose(rid,title,p,cap,another.isChecked(),ticket));
     }
     results.post(()->{if(ticket==generation&&a.body.getParent() instanceof ScrollView)((ScrollView)a.body.getParent()).smoothScrollTo(0,results.getTop());});
    });
   }catch(Exception e){a.ui(()->{if(ticket==generation){results.removeAllViews();results.addView(a.label("查询未完成，没有可用的库存结论。请重新查询。",14,a.MUTED));}});throw e;}
  });
 }catch(Exception e){a.message("请输入有效的月费上限和地区。");}}
 void choose(String rid,String title,JSONObject item,String cap,boolean extra,int ticket){
  if(ticket!=generation){a.message("条件已变化，请重新查询库存。");return;}
  a.task("核验所选套餐…",()->{JSONObject existing=m.existing(extra);if(existing!=null){a.ui(()->{a.showDeployment(existing);a.message("已有部署或待核对请求。若要额外创建，请返回库存页勾选“另建一台”；不确定请求必须先核对。");});return;}
   JSONObject selected=m.plan(rid,item.getString("id"),cap);
   a.ui(()->{if(ticket!=generation)return;a.confirm("create",title+"\n"+selected.getString("plan")+"\n"+Inventory.specs(item)+"\nUbuntu 24.04 LTS x64\n基础月费 "+Core.cash(Core.money(selected.get("price")))+"，上限 $"+cap+"。\n税费、超额流量另计，自动备份关闭。"+(extra?"\n额外新建，原实例保留并计费。":"\n新建并自动部署节点。"),()->a.task("再次核验库存后创建…",()->{if(!extra){JSONObject current=m.existing(false);if(current!=null){a.ui(()->a.showDeployment(current));return;}}JSONObject r=m.create(selected);a.ui(()->a.showDeployment(r));}));});
  });
 }
}

package com.yjw.vultr;
import java.util.*;
import java.io.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
public final class Configs {
 public static Map<String,Object> parse(String text)throws IOException{
  try{LoaderOptions o=new LoaderOptions();o.setAllowDuplicateKeys(false);o.setMaxAliasesForCollections(20);o.setNestingDepthLimit(40);o.setCodePointLimit(2*1024*1024);Object result=new Yaml(new SafeConstructor(o)).load(text);if(!(result instanceof Map))throw new IOException("需要 Clash/Mihomo YAML 配置");return (Map<String,Object>)result;}catch(IOException e){throw e;}catch(Exception e){throw new IOException("订阅 YAML 无法安全解析，请检查格式");}
 }
 public static String yaml(Map<String,Object> config){return new Yaml().dump(config);}
 public static List<Map<String,Object>> nodes(Map<String,Object> config)throws IOException{
  Object list=config.get("proxies");if(!(list instanceof List)||((List)list).isEmpty())throw new IOException("未找到内置节点列表；provider-only 或其他格式请先转换成含 proxies 的 Clash 配置");List<Map<String,Object>> result=new ArrayList<>();
  for(Object value:(List)list){if(!(value instanceof Map))throw new IOException("节点格式无效");Map<String,Object> n=new LinkedHashMap<>((Map)value);Object server=n.get("server"),port=n.get("port"),type=n.get("type");if(!(server instanceof String)||!(type instanceof String)||!(port instanceof Number)||((Number)port).intValue()<1||((Number)port).intValue()>65535)throw new IOException("节点缺少服务器、类型或有效端口");result.add(n);}return result;
 }
 public static Map<String,Object> merge(Map<String,Object> baseline,List<String> labels,List<Map<String,Object>> inputs)throws IOException{
  if(inputs.size()<2||inputs.size()!=labels.size())throw new IOException("至少选择两份订阅");List<Map<String,Object>> nodes=new ArrayList<>();List<String> names=new ArrayList<>();for(int i=0;i<inputs.size();i++){int j=0;for(Map<String,Object> node:nodes(inputs.get(i))){String original=String.valueOf(node.getOrDefault("name","节点"));String name=original;if(names.contains(name))name=labels.get(i)+" / "+original+" #"+(i+1)+"-"+(++j);while(names.contains(name))name+="_";node.put("name",name);nodes.add(node);names.add(name);}}
  Map<String,Object> source=new LinkedHashMap<>();source.put("proxies",nodes);return routing(baseline,source);

 }
 public static Map<String,Object> routing(Map<String,Object> baseline,Map<String,Object> source)throws IOException{
  List<Map<String,Object>> list=nodes(source);List<String> names=new ArrayList<>();for(Map<String,Object> node:list){Object n=node.get("name");if(!(n instanceof String)||((String)n).isEmpty()||names.contains(n)||Arrays.asList("DIRECT","REJECT","PROXY","AI","DEV","Google","YouTube","Microsoft","Apple","Claude").contains(n))throw new IOException("节点名称缺失、重复或与策略组冲突，请先修正");names.add((String)n);}
  Map<String,Object> out=new LinkedHashMap<>(baseline);out.putAll(source);out.put("mode","rule");out.put("proxies",list);for(String key:Arrays.asList("dns","rules","rule-providers"))out.put(key,baseline.get(key));
  List<Map<String,Object>> groups=new ArrayList<>();for(String name:Arrays.asList("PROXY","AI","DEV","Google","YouTube","Microsoft","Apple")){Map<String,Object> g=new LinkedHashMap<>();g.put("name",name);g.put("type","select");List<String> choices=new ArrayList<>(names);if(name.equals("Apple"))choices.add(0,"DIRECT");else choices.add("DIRECT");g.put("proxies",choices);groups.add(g);}Map<String,Object> claude=new LinkedHashMap<>();claude.put("name","Claude");claude.put("type","select");claude.put("proxies",new ArrayList<>(names));groups.add(claude);out.put("proxy-groups",groups);out.put("x-vultr-routing-version",2);return out;
 }
 public static String verification(String snapshot,String legacy,String ip){return (snapshot==null||snapshot.isEmpty()?legacy:snapshot).replace("REPLACE_SERVER_IP",ip);}
 public static Map<String,Object> testConfig(Map<String,Object> node,int port){Map<String,Object> n=new LinkedHashMap<>(node);n.put("name","TARGET");Map<String,Object> c=new LinkedHashMap<>();c.put("mixed-port",port);c.put("allow-lan",false);c.put("bind-address","127.0.0.1");c.put("mode","rule");c.put("log-level","silent");c.put("ipv6",false);c.put("dns",Collections.singletonMap("enable",false));c.put("proxies",Collections.singletonList(n));c.put("rules",Collections.singletonList("MATCH,TARGET"));return c;}
}

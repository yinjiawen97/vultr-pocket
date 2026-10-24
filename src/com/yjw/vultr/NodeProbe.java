package com.yjw.vultr;
import android.content.Context;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
final class NodeProbe {
 static volatile boolean cancelled=false;static volatile Process current;
 static void cancel(){cancelled=true;Process p=current;if(p!=null)p.destroy();}
 static String exit(Context context,Map<String,Object> node)throws Exception{
  if(cancelled)throw new IOException("检测已取消");String type=String.valueOf(node.get("type"));if(Arrays.asList("direct","reject","pass","select","url-test").contains(type))throw new IOException("此条目不是可独立检测的远程节点");
  File executable=new File(context.getApplicationInfo().nativeLibraryDir,"libmihomo.so");if(!executable.isFile())throw new IOException("本机架构没有可用检测内核");
  File dir=new File(context.getCacheDir(),"probe-"+UUID.randomUUID());if(!dir.mkdir())throw new IOException("无法创建私有检测目录");Process process=null;
  try{
   int port;try(ServerSocket listener=new ServerSocket(0,0,InetAddress.getByName("127.0.0.1"))){port=listener.getLocalPort();}
   File config=new File(dir,"config.json");Files.write(config.toPath(),Configs.yaml(Configs.testConfig(node,port)).getBytes("UTF-8"));
   process=new ProcessBuilder(executable.getAbsolutePath(),"-d",dir.getAbsolutePath(),"-f",config.getAbsolutePath()).redirectErrorStream(true).redirectOutput(new File(dir,"core.log")).start();current=process;boolean ready=false;
   for(int i=0;i<50&&!cancelled;i++){if(!process.isAlive())throw new IOException("检测内核无法启动，节点配置或系统兼容性需核对");try(Socket s=new Socket()){s.connect(new InetSocketAddress("127.0.0.1",port),100);ready=true;break;}catch(IOException e){Thread.sleep(100);}}
   if(!ready||cancelled)throw new IOException(cancelled?"检测已取消":"本机检测端口未就绪");
   for(String host:new String[]{"api.ipify.org","checkip.amazonaws.com"}){if(cancelled)throw new IOException("检测已取消");try{String ip=viaSocks(port,host).trim();if(ip.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")||ip.matches("[0-9a-fA-F:]{3,}"))return ip;}catch(IOException e){}}
   throw new IOException("经目标节点访问 HTTPS 出口测试站未通过；可能是节点、网络或测试站不可达");
  }finally{if(process!=null){process.destroy();if(!process.waitFor(2,java.util.concurrent.TimeUnit.SECONDS))process.destroyForcibly();}current=null;remove(dir);}
 }
 static String viaSocks(int port,String host)throws IOException{
  try(Socket local=new Socket()){local.connect(new InetSocketAddress("127.0.0.1",port),3000);local.setSoTimeout(8000);DataInputStream in=new DataInputStream(local.getInputStream());OutputStream out=local.getOutputStream();out.write(new byte[]{5,1,0});out.flush();if(in.readUnsignedByte()!=5||in.readUnsignedByte()!=0)throw new IOException("SOCKS 协商失败");byte[] name=host.getBytes(java.nio.charset.StandardCharsets.US_ASCII);out.write(new byte[]{5,1,0,3,(byte)name.length});out.write(name);out.write(new byte[]{1,(byte)187});out.flush();if(in.readUnsignedByte()!=5||in.readUnsignedByte()!=0)throw new IOException("SOCKS 目标连接失败");in.readUnsignedByte();int kind=in.readUnsignedByte();int n=kind==1?4:kind==4?16:kind==3?in.readUnsignedByte():-1;if(n<0)throw new IOException("SOCKS 返回异常");byte[] discard=new byte[n+2];in.readFully(discard);
   javax.net.ssl.SSLSocketFactory factory=(javax.net.ssl.SSLSocketFactory)javax.net.ssl.SSLSocketFactory.getDefault();try(javax.net.ssl.SSLSocket tls=(javax.net.ssl.SSLSocket)factory.createSocket(local,host,443,true)){javax.net.ssl.SSLParameters params=tls.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");tls.setSSLParameters(params);tls.setSoTimeout(8000);tls.startHandshake();tls.getOutputStream().write(("GET / HTTP/1.1\r\nHost: "+host+"\r\nConnection: close\r\nAccept: text/plain\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));byte[] result=new byte[8192];ByteArrayOutputStream bytes=new ByteArrayOutputStream();InputStream response=tls.getInputStream();int k;while((k=response.read(result))!=-1){bytes.write(result,0,k);if(bytes.size()>32768)throw new IOException("出口响应过大");}String raw=bytes.toString("UTF-8");int split=raw.indexOf("\r\n\r\n");if(split<0||!raw.substring(0,raw.indexOf("\r\n")).contains(" 200 "))throw new IOException("出口测试响应异常");String body=raw.substring(split+4);if(raw.substring(0,split).toLowerCase(Locale.ROOT).contains("transfer-encoding: chunked")){String[] lines=body.split("\r\n");if(lines.length<2)throw new IOException("出口响应异常");body=lines[1];}return body;}
  }
 }
 static void remove(File f){File[] list=f.listFiles();if(list!=null)for(File x:list)remove(x);f.delete();}
}

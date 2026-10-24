import com.yjw.vultr.ServerPower;
import org.json.*;
import java.io.*;
import java.util.*;
public class ServerPowerTest {
 static final String ID="11111111-1111-4111-8111-111111111111",OTHER="22222222-2222-4222-8222-222222222222";
 static void check(boolean x,String message){if(!x)throw new AssertionError(message);}
 static JSONObject node(String power)throws Exception{return new JSONObject().put("id",ID).put("label","示例").put("status","active").put("server_status","ok").put("power_status",power).put("default_password","ROOT_SECRET").put("password","SS_SECRET");}
 static class Rejected extends IOException implements ServerPower.HttpFailure {public int status(){return 403;}}
 static class Pending implements ServerPower.Pending {boolean saved,cleared,fail;public void begin(String id,String action)throws Exception{check(id.equals(ID),"pending wrong ID");if(fail)throw new IOException("storage failed");saved=true;}public void clear(String id){cleared=true;}}
 static class Fake implements ServerPower.Transport {JSONObject before,after;Pending mark;int gets,posts;Exception postError;boolean getError;List<String> paths=new ArrayList<>();Fake(String power,Pending p)throws Exception{before=node(power);after=node(power.equals("stopped")?"running":"stopped");mark=p;}public JSONObject call(String method,String path,JSONObject body)throws Exception{paths.add(method+" "+path);check(path.startsWith("/instances/"+ID),"wrong endpoint");if(method.equals("POST")){check(mark.saved,"POST before durable marker");posts++;if(postError!=null)throw postError;return new JSONObject();}gets++;if(gets>1&&getError)throw new IOException("read timeout");return new JSONObject().put("instance",gets==1?before:after);}}
 public static void main(String[] args)throws Exception{
  for(String action:new String[]{"start","halt"}){Pending p=new Pending();Fake api=new Fake(action.equals("start")?"stopped":"running",p);ServerPower.Result r=ServerPower.execute(api,ID,action,p);check(r.sent&&r.confirmed&&!r.uncertain&&api.posts==1&&p.cleared,"normal power action");check(api.paths.get(1).equals("POST /instances/"+ID+"/"+action),"exact single-instance endpoint");check(!r.instance.toString().contains("SECRET"),"credentials leaked to public result");}
  Pending p=new Pending();Fake api=new Fake("running",p);ServerPower.Result r=ServerPower.execute(api,ID,"start",p);check(!r.sent&&r.confirmed&&api.posts==0,"already on should not POST");
  p=new Pending();api=new Fake("stopped",p);api.before.put("server_status","installingbooting");try{ServerPower.execute(api,ID,"start",p);throw new AssertionError("transition allowed");}catch(IOException expected){}check(api.posts==0,"transition POST");
  p=new Pending();p.fail=true;api=new Fake("stopped",p);try{ServerPower.execute(api,ID,"start",p);throw new AssertionError("storage failure ignored");}catch(IOException expected){}check(api.posts==0,"POST despite storage failure");
  p=new Pending();api=new Fake("stopped",p);api.postError=new IOException("response lost");r=ServerPower.execute(api,ID,"start",p);check(r.uncertain&&!r.confirmed&&api.posts==1&&!p.cleared,"lost response replayed or falsely succeeded");
  p=new Pending();api=new Fake("stopped",p);api.postError=new Rejected();try{ServerPower.execute(api,ID,"start",p);throw new AssertionError("403 ignored");}catch(Rejected expected){}check(api.posts==1&&p.cleared,"rejected marker retained");
  p=new Pending();api=new Fake("running",p);api.getError=true;r=ServerPower.execute(api,ID,"halt",p);check(r.uncertain&&api.posts==1&&!p.cleared,"follow-up failure must remain pending");
  p=new Pending();api=new Fake("stopped",p);api.before.put("id",OTHER);try{ServerPower.execute(api,ID,"start",p);throw new AssertionError("mismatched response accepted");}catch(IOException expected){}check(api.posts==0,"wrong target mutated");
  for(String action:new String[]{"reboot","delete","../halt",""}){p=new Pending();api=new Fake("stopped",p);try{ServerPower.execute(api,ID,action,p);throw new AssertionError("invalid action");}catch(IllegalArgumentException expected){}check(api.posts==0&&api.gets==0,"invalid input made network call");}
  try{ServerPower.id("1-1-1-1-1");throw new AssertionError("noncanonical UUID");}catch(IllegalArgumentException expected){}
  JSONObject marker=new JSONObject().put("sent",1000).put("action","start");check(ServerPower.pending(marker,180999)&&!ServerPower.pending(marker,181000)&&!ServerPower.pending(marker,999),"pending window");JSONObject marks=new JSONObject().put(ID,marker).put(OTHER,new JSONObject().put("action","halt"));ServerPower.reconcile(marks,new JSONArray().put(node("running")));check(marks.length()==0,"reconcile completed/deleted targets");
  System.out.println("PASS: power endpoints, exact target, durable marker, no replay, state gates, lost/rejected requests and reconciliation");
 }
}

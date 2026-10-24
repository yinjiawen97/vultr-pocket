import com.yjw.vultr.Refresher;
public class RefresherTest {
 static void check(boolean v,String what){if(!v)throw new AssertionError(what);}
 public static void main(String[] args){
  Refresher r=new Refresher();long ttl=Refresher.INSTANCES;
  check(r.due("instances",ttl,0,1000),"never fetched is due");
  check(!r.due("instances",ttl,1000,1000+ttl-1),"fresh cache is not due");
  check(r.due("instances",ttl,1000,1000+ttl),"expired cache is due");
  check(r.due("instances",ttl,5000,1000),"future timestamp (clock change) is due");
  check(r.start("instances")&&!r.start("instances"),"second start of same key is merged");
  check(!r.due("instances",ttl,0,99999),"running key is never due");
  check(r.start("account")&&!r.idle(),"different keys load in parallel");
  r.finish("instances",2000);check(!r.running("instances")&&r.running("account"),"finish releases only its key");
  check(!r.due("instances",ttl,0,2000+ttl-1),"failed/finished attempt waits one ttl");
  check(r.due("instances",ttl,0,2000+ttl),"retry after ttl");
  r.finish("account",3000);check(r.idle(),"idle after all finish");
  check(Refresher.CLOUD==900_000L&&Refresher.ACCOUNT==300_000L&&Refresher.COSTS==21_600_000L,"tier intervals");
  System.out.println("PASS: tiered due/fresh, future clock, merge same key, parallel keys, retry wait, idle");
 }
}

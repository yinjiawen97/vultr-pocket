import com.yjw.vultr.Core;
import java.math.BigDecimal;
public class CoreTest {
 static int n=0;
 static void check(boolean v){n++;if(!v)throw new AssertionError("case "+n);}
 public static void main(String[] args){
  check(Core.budget("5","5"));check(!Core.budget("5.01","5"));
  for(String s:new String[]{"0","-1","NaN","Infinity",""}){boolean fail=false;try{Core.budget("5",s);}catch(IllegalArgumentException e){fail=true;}check(fail);}
  check(Core.cash(Core.money("0.1").add(Core.money("0.2"))).equals("$0.30"));
  check(Core.cash(Core.money("-305.76")).equals("$-305.76"));
  check(Core.overlaps("2026-10-21T07:00:00Z","2026-10-20T00:00:00+00:00"));
  check(!Core.overlaps("2026-10-19T07:00:00Z","2026-10-20T00:00:00+00:00"));
  check(!Core.overlaps("bad","bad"));check(Core.confirm(" CREATE ","create"));check(!Core.confirm("yes","delete"));
  check(Core.uncertain("create-pending"));check(Core.uncertain("reinstall-preparing"));check(!Core.uncertain("created"));
  check(Core.inactive("create-rejected"));check(Core.inactive("deleted"));check(!Core.inactive("subscription-ready"));
  System.out.println("PASS: "+n+" core checks");
 }
}

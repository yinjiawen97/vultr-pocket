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
  for(String os:new String[]{"Ubuntu 22.04 LTS x64","Ubuntu 24.04 LTS x64","Ubuntu 26.04 LTS x64","Ubuntu 26.10 x64","Ubuntu 27.04 x64","Ubuntu 28.04 LTS x64","Debian 12 x64 (bookworm)","Debian 13 x64 (trixie)","Debian 14 x64 (forky)"})check(Core.deployable(os));
  for(String os:new String[]{null,"","Ubuntu 20.04 LTS x64","Ubuntu 26.10","Ubuntu 26.04 LTS arm64","ubuntu 26.04 lts x64","Ubuntu 26.04 LTS x64 extra","Debian 11 x64 (bullseye)","Debian 13 x64","Rocky Linux 9 x64","Windows 2025 Standard x64","myUbuntu 26.04 LTS x64"})check(!Core.deployable(os));
  for(String s:Core.SYSTEMS){check(Core.deployable(s));check(Core.listed(s));check(Core.system(s).equals(s));check(Core.blocked(s).isEmpty());check(Core.switchHint(s).isEmpty());}
  check(Core.listed(Core.DEFAULT_SYSTEM));for(String s:new String[]{null,"","Ubuntu 27.04 x64","Rocky Linux 9 x64"})check(Core.system(s).equals("Ubuntu 24.04 LTS x64"));
  check(Core.choices("Ubuntu 26.10 x64")==Core.SYSTEMS);check(Core.choices("Rocky Linux 9 x64")==Core.SYSTEMS);check(Core.choices(null)==Core.SYSTEMS);String[] newer=Core.choices("Ubuntu 27.04 x64");check(newer.length==Core.SYSTEMS.length+1&&newer[0].equals("Ubuntu 27.04 x64")&&newer[1].equals(Core.SYSTEMS[0]));
  for(String s:new String[]{null,"","  "}){check(Core.blocked(s).contains("尚未获取系统信息"));check(Core.switchHint(s).isEmpty());}for(String s:new String[]{"Ubuntu 20.04 LTS x64","Rocky Linux 9 x64"}){check(Core.blocked(s).isEmpty());check(Core.switchHint(s).startsWith("当前系统 "+s+" 不能直接部署"));}check(Core.switchHint("Ubuntu 27.04 x64").isEmpty());
  for(int code:new int[]{400,401,403,404,422})check(Core.refused(code));for(int code:new int[]{200,408,429,500,503})check(!Core.refused(code));
  System.out.println("PASS: "+n+" core checks");
 }
}

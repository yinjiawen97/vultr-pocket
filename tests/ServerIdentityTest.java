import com.yjw.vultr.ServerIdentity;
public class ServerIdentityTest {
 static void family(String os,String expected){if(!ServerIdentity.family(os).equals(expected))throw new AssertionError("OS identification: "+os);}
 public static void main(String[] args){
  family("Ubuntu 24.04 LTS x64","ubuntu");family("uBuNtU 22.04","ubuntu");family("Debian GNU/Linux 12 x64","debian");family("Windows Server 2025 Standard x64","windows");family("WINDOWS 11","windows");family("macOS Sonoma","macos");family("Mac OS X 10.15","macos");family("Darwin 24","macos");family("Fedora 42","fedora");family("Rocky Linux 9","rocky");family("AlmaLinux 9","alma");family("Arch Linux","arch");
  for(String os:new String[]{"Alpine Linux","CentOS Stream 9","RHEL 9","Red Hat Enterprise Linux","openSUSE Leap","Oracle Linux","Flatcar Container Linux"})family(os,"linux");
  for(String os:new String[]{null,"","   ","Custom ISO","FreeBSD 14","machine","myubuntuimage","Windowsish","macOSish"})family(os,"unknown");
  if(!ServerIdentity.name(null).equals("系统待获取")||!ServerIdentity.name(" ").equals("系统待获取")||!ServerIdentity.name(" Custom ISO ").equals("Custom ISO"))throw new AssertionError("unknown OS text not preserved");
  if(!ServerIdentity.host("vc2-1c-1gb").equals("Vultr 云主机 · vc2-1c-1gb")||!ServerIdentity.host(null).equals("Vultr 云主机 · 套餐待获取"))throw new AssertionError("cloud host is guessed or missing plan invented");
  System.out.println("PASS: OS family identification, case folding, original names, missing fields, no label/plan inference");
 }
}

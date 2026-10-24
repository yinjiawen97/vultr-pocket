import com.yjw.vultr.HomeState;
public class HomeStateTest {
 static int n=0;static void check(boolean v){n++;if(!v)throw new AssertionError("home state "+n);}
 public static void main(String[] args){
  check(HomeState.fresh(10000,20000));check(!HomeState.fresh(10000,100000));check(!HomeState.fresh(0,20000));check(!HomeState.fresh(20000,10000));
  check(HomeState.running("active","running"));check(!HomeState.running("active","stopped"));check(!HomeState.running("pending","running"));
  check(HomeState.state("active","running",false).equals("等待更新"));check(HomeState.state("active","running",true).equals("运行中"));check(HomeState.state("active","stopped",true).equals("已关机"));check(HomeState.state("pending","unknown",true).equals("部署中"));check(HomeState.state("unknown","unknown",true).equals("待核对"));
  System.out.println("PASS: "+n+" home state checks");
 }
}

import com.yjw.vultr.CloudState;
public class CloudStateTest {
 public static void main(String[] args){
  if(CloudState.healthy(200,401,200)||CloudState.healthy(200,200,-2)||CloudState.healthy(-1,200,200))throw new AssertionError("partial failures must not show healthy");
  if(!CloudState.healthy(200,200,200))throw new AssertionError();
  if(CloudState.fresh(1000,999)||CloudState.fresh(0,1000)||CloudState.fresh(1000,901001))throw new AssertionError("stale or future checks must not show fresh");
  if(!CloudState.fresh(1000,901000))throw new AssertionError();
  if(!CloudState.summary(true,1000,901001,200,200,200).contains("过期"))throw new AssertionError("old success must not remain green");
  if(!CloudState.stage(-1).contains("未知")||!CloudState.stage(401).contains("权限")||!CloudState.stage(-2).contains("格式"))throw new AssertionError("actionable distinction");
  System.out.println("PASS: Cloudflare partial failure, stale/future records, network/auth/format distinction");
 }
}

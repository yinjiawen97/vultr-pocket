import com.yjw.vultr.NavPolicy;
public class NavPolicyTest {
 public static void main(String[] args){String[] labels={"总览","服务器","订阅","费用","设置"};if(!java.util.Arrays.equals(labels,NavPolicy.TABS))throw new AssertionError("five top-level destinations");for(String s:labels)if(!NavPolicy.section(s).equals(s))throw new AssertionError(s);for(String s:new String[]{"库存与方案","服务器详情","部署记录","部署","管理"})if(!NavPolicy.section(s).equals("服务器"))throw new AssertionError(s);for(String s:new String[]{"固定订阅","固定入口设置","合并订阅"})if(!NavPolicy.section(s).equals("订阅"))throw new AssertionError(s);if(NavPolicy.owns(8,9)||!NavPolicy.owns(9,9))throw new AssertionError("stale results must not own a new page");System.out.println("PASS: five tabs, detail ownership, legacy aliases and stale UI results");}
}

package servicefinder;

import com.iflytek.ccr.finder.FinderManager;
import com.iflytek.ccr.finder.handler.ServiceHandle;
import com.iflytek.ccr.finder.service.RouteService;
import com.iflytek.ccr.finder.utils.ByteUtil;
import com.iflytek.ccr.finder.utils.RemoteUtil;
import com.iflytek.ccr.finder.value.*;
import com.iflytek.ccr.zkutil.ZkHelper;
import configcenter.SdkInit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import utils.ListUtil;
import utils.Md5Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * 6）从路由规则中，调整到非路由规则中
 * 预期：路由规则中的实例下线，路由规则外的实例上线
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteUtil.class)
@PowerMockIgnore({ "org.apache.*"})
public class RouteModuleTest4_6 {
    static RouteService routeService;
    static FinderManager finderManager;
    static String project = "ds";
    static String group = "ds";
    static String service = "se";
    static String version = "4.0";
    static String baseConfigPath;
    static ZkHelper zkHelper = new ZkHelper("10.1.87.69:2183");

    static List<ServiceInstance> a = new ArrayList<>();
    static List<ServiceInstance> b = new ArrayList<>();
    static String servicePath;
    //改变后的配置节点
    String changedData = "{\"loace\":\"loance\",\"key1\":\"val\",\"key2\":\"val\"}";
    static String instanceDataY = "1234567890{\"user\": {\"loadbalance\": \"loadbalance\",\"key1\": \"val\",\"key2\": \"val\"},\"sdk\": {\"is_valid\": true}}";
    static String instanceDataN = "1234567890{\"user\": {\"loadbalance\": \"loadbalance\",\"key1\": \"val\",\"key2\": \"val\"},\"sdk\": {\"is_valid\": false}}";

    @BeforeClass
    public static void setUp() throws Exception {
        baseConfigPath = "/polaris/" +
                "service/" +
                Md5Util.getMD5(project + group) + "/" +
                service + "/" +
                version;
        finderManager = new FinderManager();
        pre();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    public static void pre() {
        //    ",{\"id\":\"2\",\"consumer\":[\"11.2.3.4:8080\",\"2.2.3.4:8080\",\"199.99.99.99:99\"],\"provider\":[\"1.1.1.1:1111\",\"1.1.1.2:1111\",\"1.1.1.3:1111\",\"1.1.1.4:1111\"],\"only\":\"Y\"}}]";
        String configData = "{\"loadbalance\":\"loadbalance\",\"key1\":\"val\",\"key2\":\"val\"}";
        try {
            byte[] zkBytes = ByteUtil.getZkBytes(routeData.getBytes("UTF-8"), "1234567890");
            zkHelper.addOrUpdatePersistentNode(baseConfigPath + "/route", zkBytes);
            zkHelper.addOrUpdatePersistentNode(baseConfigPath + "/provider", "".getBytes());
            zkHelper.addOrUpdatePersistentNode(baseConfigPath + "/conf", ByteUtil.getZkBytes(configData.getBytes("utf-8"), "1234567890"));
//            zkHelper.addOrUpdatePersistentNode(baseConfigPath + "/provider","".getBytes());

            zkHelper.addOrUpdateEphemeralNode(baseConfigPath + "/provider/p5", ByteUtil.getZkBytes(instanceDataY.getBytes(), "1234567890"));
            zkHelper.addOrUpdateEphemeralNode(baseConfigPath + "/provider/p4", ByteUtil.getZkBytes(instanceDataN.getBytes(), "1234567890"));
            zkHelper.addOrUpdateEphemeralNode(baseConfigPath + "/provider/p1", ByteUtil.getZkBytes(configData.getBytes("utf-8"), "1234567890"));
            zkHelper.addOrUpdateEphemeralNode(baseConfigPath + "/provider/p2", ByteUtil.getZkBytes(instanceDataY.getBytes(), "1234567890"));
            zkHelper.addOrUpdateEphemeralNode(baseConfigPath + "/provider/p3", ByteUtil.getZkBytes(instanceDataY.getBytes(), "1234567890"));


            //  zkHelper.remove(baseConfigPath+"/provider/1.1.1.1:1111");
            //  zkHelper.remove(baseConfigPath+"/provider/1.1.1.3:1111");
            //  zkHelper.remove(baseConfigPath+"/provider/1.1.1.2:1111");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


    }

    @AfterClass
    public static void tearDown() throws Exception {

        // System.out.println("tearDown");


    }

    //路由规则改变之前
    static String routeData = "[{\"id\":\"1\",\"consumer\":[\"c1\",\"c2\",\"c3\"],\"provider\":[\"p1\",\"p2\",\"p3\"],\"only\":\"Y\"}]";
    //路由规则改变之后
    String changedRouteDataAdd = "[{\"id\":\"1\",\"consumer\":[\"c3\",\"c2\"],\"provider\":[\"p1\",\"p2\",\"p3\"],\"only\":\"Y\"}]";


    @Test
    public void moduleTest() {
        try {
            SdkInit.init(finderManager, project, group, service, version);
            testSubscribeService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    List<InstanceChangedEvent> eventList;

    private void testSubscribeService() throws Exception {
        SubscribeRequestValue value = new SubscribeRequestValue();
        value.setServiceName(service);
        value.setApiVersion(version);

        CommonResult<List<Service>> listCommonResult = finderManager.useAndSubscribeService(ListUtil.collectAsList(value), new ServiceHandle() {
            @Override
            public boolean onServiceInstanceConfigChanged(String serviceName, String instance, String jsonConfig) {
                System.out.println("1 called");
                Assert.assertEquals(instanceDataY, jsonConfig);

                return false;
            }

            @Override
            public boolean onServiceConfigChanged(String serviceName, String jsonConfig) {
                System.out.println("2 called");
                System.out.println("jsonConfig:" + jsonConfig);

                Assert.assertEquals(changedData, jsonConfig);
                return false;
            }

            @Override
            public boolean onServiceInstanceChanged(String serviceName, List<InstanceChangedEvent> eventList) {
                System.out.println("3 called");
                RouteModuleTest4_6.this.eventList = eventList;
                for (InstanceChangedEvent event : eventList) {
                    System.out.println(event.getType() + " " + event.getServiceInstanceList());
                }
                return false;
            }
        });
        System.out.println("result: " + listCommonResult);
        Thread.sleep(13000);
        changeRouteFromNtoY();



        //预期的数据
        List removed = ListUtil.collectAsArrayList("p1", "p2", "p3");
        List added = ListUtil.collectAsArrayList("p4", "p5");
        Assert.assertEquals(2, eventList.size());
        for (InstanceChangedEvent e : eventList) {
            List<String> addActural = new ArrayList<>();
            for (ServiceInstance instance : e.getServiceInstanceList()) {
                addActural.add(instance.getAddr());
            }
            if (e.getType().equals(InstanceChangedEvent.Type.ADD)) {

                Assert.assertEquals(true, ListUtil.equals(added, addActural));
            } else {
                Assert.assertEquals(true, ListUtil.equals(removed, addActural));
            }
        }
    }

    private void changeRouteFromNtoY() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        zkHelper.update(baseConfigPath + "/route", ByteUtil.getZkBytes(changedRouteDataAdd.getBytes(), "1234567890"));
    }


}

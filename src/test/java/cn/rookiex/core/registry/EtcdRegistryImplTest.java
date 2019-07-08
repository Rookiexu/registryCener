package cn.rookiex.core.registry;

import cn.rookiex.core.center.EtcdRegisterCenterImpl;
import cn.rookiex.core.factory.EtcdServiceFactoryImpl;
import cn.rookiex.core.service.Service;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @Author : Rookiex
 * @Date : 2019/07/05
 * @Describe :
 */
@SuppressWarnings("Duplicates")
public class EtcdRegistryImplTest {
    String endpoint = "http://etcd.rookiex.cn:2379";
    String serevice = "testService";
    String ip = "192.168.2.26";
    int port = 1111;

    @BeforeClass
    public static void before() {
        EtcdRegisterCenterImpl.setFactory(new EtcdServiceFactoryImpl());
    }

    @Test
    public void init() {
    }

    @Test
    public void getServiceList() {
    }

    @Test
    public void registerService() {
        int size = 5;
        EtcdRegistryImpl etcdRegister = new EtcdRegistryImpl();
        etcdRegister.init(endpoint);

        List<Service> serviceList = etcdRegister.getServiceList(serevice);
        System.out.println("before start");
        serviceList.forEach(service -> {
            String serviceName = service.getServiceName();
            String fullPath = service.getFullPath();
            System.out.println("before path ==> " + fullPath + " ,name ==> " + serviceName + " serverIsBand ==> " + service.isBanned());
        });
        System.out.println("before over");

        try {
            for (int i = 0; i < size; i++) {
                String newIp = ip + ":" + (port + i);
                etcdRegister.registerService(serevice, newIp);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        serviceList = etcdRegister.getServiceList(serevice);
        System.out.println("after start");
        serviceList.forEach(service -> {
            String serviceName = service.getServiceName();
            String fullPath = service.getFullPath();
            System.out.println("after path ==> " + fullPath + " ,name ==> " + serviceName + " serverIsBand ==> " + service.isBanned());
        });
        System.out.println("after over");

    }

    @Test
    public void bandService() {
        int size = 5;
        EtcdRegistryImpl etcdRegister = new EtcdRegistryImpl();
        etcdRegister.init(endpoint);
        try {
            for (int i = 0; i < size; i++) {
                String newIp = ip + ":" + (port + i);
                etcdRegister.registerService(serevice, newIp);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        List<Service> serviceList = etcdRegister.getServiceList(serevice);
        System.out.println("after start");
        serviceList.forEach(service -> {
            String serviceName = service.getServiceName();
            String fullPath = service.getFullPath();
            System.out.println("registry path ==> " + fullPath + " ,name ==> " + serviceName + " serverIsBand ==> " + service.isBanned());
        });
        System.out.println("after over");

        try {
            for (int i = 0; i < size; i++) {
                String newIp = ip + ":" + (port + i);
                etcdRegister.bandService(serevice, newIp);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        serviceList = etcdRegister.getServiceList(serevice);
        System.out.println("band start");
        serviceList.forEach(service -> {
            String serviceName = service.getServiceName();
            String fullPath = service.getFullPath();
            System.out.println("band path ==> " + fullPath + " ,name ==> " + serviceName + " serverIsBand ==> " + service.isBanned());
        });
        System.out.println("band over");
    }

    @Test
    public void watch() {
    }

    @Test
    public void unWatch() {
    }
}
package cn.rookiex.core.registry;

import cn.rookiex.core.center.EtcdRegisterCenterImpl;
import cn.rookiex.core.factory.EtcdServiceFactoryImpl;
import cn.rookiex.core.service.Service;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author : Rookiex
 * @Date : 2019/07/05
 * @Describe :
 */
@SuppressWarnings("Duplicates")
public class EtcdRegistryImplTest {
    private Logger log = Logger.getLogger(getClass());
    private String endpoint = "http://etcd.rookiex.cn:2379";
    private String service = "testService";
    private String ip = "192.168.2.26";
    private int port = 1111;
    String endpoints = "";
    String userName = "";
    String password = "";

    @BeforeClass
    public static void before() {
        EtcdRegisterCenterImpl.setFactory(new EtcdServiceFactoryImpl());
    }

    @Test
    public void init() {
        EtcdRegistryImpl etcdRegister = new EtcdRegistryImpl();
        etcdRegister.init(endpoints, userName, password);
        dealRegisterService(etcdRegister, 1);
    }

    private void dealRegisterService(EtcdRegistryImpl etcdRegister, int size) {
        List<Service> serviceList = etcdRegister.getServiceList(service, true);
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
                etcdRegister.registerService(service, newIp);
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }

        serviceList = etcdRegister.getServiceList(service, true);
        System.out.println("after start");
        serviceList.forEach(service -> {
            String serviceName = service.getServiceName();
            String fullPath = service.getFullPath();
            System.out.println("after path ==> " + fullPath + " ,name ==> " + serviceName + " serverIsBand ==> " + service.isBanned() + " ,version == " + service.getVersion());
        });
        System.out.println("after over");
    }

    @Test
    public void getServiceList() {
        int size = 1;
        EtcdRegistryImpl etcdRegister = new EtcdRegistryImpl();
        etcdRegister.init(endpoint);

        List<Service> data1 = etcdRegister.getServiceList("data1");
        List<Service> serviceList = etcdRegister.getServiceList("data1-logic");
        List<Service> publicService = etcdRegister.getServiceList("publicService");

        List<Service> objects = Lists.newArrayList();
        objects.addAll(data1);
        objects.addAll(serviceList);
        objects.addAll(publicService);

        objects.forEach(service->{
            log.info("service name == "+ service.getServiceName() + " service path == " + service.getFullPath());
        });
    }

    @Test
    public void registerService() {
        int size = 1;
        EtcdRegistryImpl etcdRegister = new EtcdRegistryImpl();
        etcdRegister.init(endpoint);

        dealRegisterService(etcdRegister, size);

        dealRegisterService(etcdRegister, size);

        dealRegisterService(etcdRegister, size);

    }

    @Test
    public void registerServiceKeepAlive() {
        int size = 1;
        EtcdRegistryImpl etcdRegister = new EtcdRegistryImpl();
        etcdRegister.init(endpoint);

        List<Service> serviceList;
        try {
            for (int i = 0; i < size; i++) {
                String newIp = ip + ":" + (port + i);
                etcdRegister.registerService(service, newIp);
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 1; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            serviceList = etcdRegister.getServiceList(service);
            log.info("after start");
            serviceList.forEach(service -> {
                String serviceName = service.getServiceName();
                String fullPath = service.getFullPath();
                log.info("after path ==> " + fullPath + " ,name ==> " + serviceName + " serverIsBand ==> " + service.isBanned());
            });
            log.info("after over");
        }
    }

    @Test
    public void bandService() {
        int size = 5;
        EtcdRegistryImpl etcdRegister = new EtcdRegistryImpl();
        etcdRegister.init(endpoint);
        etcdRegister.startConnect();
        try {
            for (int i = 0; i < size; i++) {
                String newIp = ip + ":" + (port + i);
                etcdRegister.registerService(service, newIp);
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }

        List<Service> serviceList = etcdRegister.getServiceList(service);
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
                etcdRegister.bandService(service, newIp);
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
        serviceList = etcdRegister.getServiceList(service);
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

    @Test
    public void keepAlive() {
        testKeepAlive(1);
    }

    private static int TTL_TIME = 10;
    private AtomicBoolean atomicBoolean = new AtomicBoolean();
    private ScheduledExecutorService keepAliveService;
    private void testKeepAlive(int level) {
         keepAliveService = Executors.newSingleThreadScheduledExecutor();
        keepAliveService.scheduleAtFixedRate(() -> {
            try {
                log.debug("test keep alive == " + level);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (!atomicBoolean.get())
                if (atomicBoolean.compareAndSet(false, true)) {
                    keepAliveService.shutdown();
                    testKeepAlive(20);
                }

        }, TTL_TIME / 3, TTL_TIME / 3, TimeUnit.SECONDS);
    }

    @Test
    public void getServiceName() {
    }

    @Test
    public void setServiceName() {
    }

    @Test
    public void getIp() {
    }

    @Test
    public void setIp() {
    }
}
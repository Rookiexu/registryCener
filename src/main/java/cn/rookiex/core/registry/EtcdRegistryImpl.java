package cn.rookiex.core.registry;

import cn.rookiex.core.center.EtcdRegisterCenterImpl;
import cn.rookiex.core.updateEvent.EtcdServiceUpdateEventImpl;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cn.rookiex.core.center.RegisterCenter;
import cn.rookiex.core.RegistryConstants;
import cn.rookiex.core.service.Service;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe : 目前的设计下,一个服务器只需要注册一个服务
 */
public class EtcdRegistryImpl implements Registry {

    public static final String BAN = "ban", OPEN = "open";
    public static int TTL_TIME = 10;

    private long leaseId = 0;

    private Logger logger = Logger.getLogger(getClass());

    private Lease leaseClient;
    private KV kvClient;
    private Watch watchClient;

    private ScheduledExecutorService keepAliveService;
    private ExecutorService executorService;
    private boolean keepAlive = true;

    private Map<String, RegisterCenter> watchServiceMap = Maps.newConcurrentMap();

    @Override
    public void init(String url) {
        Client client = Client.builder().endpoints(url).build();
        this.leaseClient = client.getLeaseClient();
        this.kvClient = client.getKVClient();
        this.watchClient = client.getWatchClient();
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * 获取服务信息
     *
     * @param serviceName 服务名字
     * @param usePrefix
     */
    @Override
    public List<Service> getServiceList(String serviceName, boolean usePrefix) {
        List<Service> serviceList = Lists.newCopyOnWriteArrayList();
        try {
            ByteSequence seqKey = ByteSequence.fromString(serviceName);
            GetResponse response;
            if (usePrefix) {
                response = kvClient.get(seqKey, GetOption.newBuilder().withPrefix(ByteSequence.fromString(serviceName)).build()).get();
            } else {
                response = kvClient.get(seqKey).get();
            }
            List<KeyValue> kvs = response.getKvs();
            kvs.forEach(keyValue -> {
                ByteSequence key = keyValue.getKey();
                ByteSequence value = keyValue.getValue();
                long lease = keyValue.getLease();
                String keyS = key.toStringUtf8();
                Service service = EtcdRegisterCenterImpl.factory.getService(keyS, value.toStringUtf8().equals(EtcdRegistryImpl.BAN), lease);
                if (service != null)
                    serviceList.add(service);
            });
            return serviceList;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return serviceList;
    }

    /**
     * 获得serviceName下的所有服务,默认开启前缀条件
     *
     * @param serviceName
     */
    @Override
    public List<Service> getServiceList(String serviceName) {
        return this.getServiceList(serviceName, RegistryConstants.USE_PREFIX);
    }

    /**
     * 注册服务
     *
     * @param serviceName 服务名字
     * @param ip          地址
     */
    @Override
    public void registerService(String serviceName, String ip) throws ExecutionException, InterruptedException {
        ByteSequence key = getServiceKey(serviceName, ip);
        ByteSequence val = ByteSequence.fromString(OPEN);
        LeaseGrantResponse leaseGrantResponse = leaseClient.grant(TTL_TIME).get();
        leaseId = leaseGrantResponse.getID();
        kvClient.put(key, val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
        keepAlive();
    }

    private ByteSequence getServiceKey(String serviceName, String ip) {
        return ByteSequence.fromString(serviceName + RegistryConstants.SEPARATOR + ip);
    }

    /**
     * 屏蔽服务
     *
     * @param serviceName 服务名字
     * @param ip          地址
     */
    @Override
    public void bandService(String serviceName, String ip) throws ExecutionException, InterruptedException {
        ByteSequence key = getServiceKey(serviceName, ip);
        ByteSequence val = ByteSequence.fromString(BAN);
        kvClient.put(key, val).get();
        keepAlive();
    }

    private void shutdown() {
        this.keepAlive = false;
    }

    /**
     * 发送心跳到ETCD,表明该host是活着的
     */
    private void keepAlive() {
        keepAliveService = Executors.newSingleThreadScheduledExecutor();
        keepAliveService.scheduleAtFixedRate(() -> {
            if (!keepAlive) {
                this.keepAliveService.shutdown();
            } else {
                try {
                    CompletableFuture<LeaseKeepAliveResponse> responseCompletableFuture = leaseClient.keepAliveOnce(leaseId);
                    LeaseKeepAliveResponse leaseKeepAliveResponse = responseCompletableFuture.get();
                    logger.debug("KeepAlive  ttl == " + leaseKeepAliveResponse.getTTL() + " lease:" + leaseKeepAliveResponse.getID()
                            + "; Hex format:" + Long.toHexString(leaseKeepAliveResponse.getID()));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }, TTL_TIME / 3, TTL_TIME / 3, TimeUnit.SECONDS);
    }

    @Override
    public void watch(String serviceName, boolean usePrefix, RegisterCenter center) {
        if (center == null) {
            logger.warn("watch service -> " + serviceName + " ,center is null !!!!!!!!!!");
            return;
        }
        watchServiceMap.put(serviceName, center);
        dealWatch(serviceName, usePrefix, center);
    }

    /**
     * 监听服务,默认开启前缀条件
     *
     * @param serviceName
     * @param center
     */
    @Override
    public void watch(String serviceName, RegisterCenter center) {
        this.watch(serviceName, RegistryConstants.USE_PREFIX, center);
    }

    @Override
    public void unWatch(String serviceName, boolean usePrefix) {
        watchServiceMap.forEach((k,v)->{
            if (k.startsWith(serviceName)){
                watchServiceMap.remove(k);
            }
        });
    }

    /**
     * 取消监听服务,默认开启前缀条件
     *
     * @param serviceName
     */
    @Override
    public void unWatch(String serviceName) {
        watchServiceMap.remove(serviceName);
    }

    private void dealWatch(String serviceName, Boolean usePrefix, RegisterCenter center) {
        executorService.execute(() -> {
            try {
                Watch.Watcher watcher;
                if (usePrefix) {
                    watcher = watchClient.watch(ByteSequence.fromString(serviceName), WatchOption.newBuilder().withPrefix(ByteSequence.fromString(serviceName)).build());
                } else {
                    watcher = watchClient.watch(ByteSequence.fromString(serviceName));
                }
                List<WatchEvent> events = watcher.listen().getEvents();
                callBackUpdateEvents(events, center);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            RegisterCenter registerCenter = watchServiceMap.get(serviceName);
            if (registerCenter != null)
                this.dealWatch(serviceName, usePrefix, registerCenter);
        });
    }

    private boolean needWatchService(String serviceName) {
        return watchServiceMap.get(serviceName) != null;
    }


    private void callBackUpdateEvents(List<WatchEvent> events, RegisterCenter center) {
        List<ServiceUpdateEvent> updateEvents = Lists.newCopyOnWriteArrayList();
        events.forEach(event -> {
            EtcdServiceUpdateEventImpl updateEvent = new EtcdServiceUpdateEventImpl();
            WatchEvent.EventType eventType = event.getEventType();
            KeyValue keyValue = event.getKeyValue();
            KeyValue prevKV = event.getPrevKV();
            String keyS = keyValue.getKey().toStringUtf8();
            String[] split = keyS.split(RegistryConstants.SEPARATOR);
            if (split.length == 2) {
                String serviceName = split[0];
                updateEvent.setEventType(eventType);
                updateEvent.setServiceName(serviceName);
                updateEvent.setKeyValue(keyValue);
                updateEvent.setPrevKV(prevKV);
                updateEvent.setFullPath(keyS);
                updateEvents.add(updateEvent);
            }
        });
        center.callback(updateEvents);
    }
}

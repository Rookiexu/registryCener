package cn.rookiex.core.registry;

import cn.rookiex.core.RegistryConstants;
import cn.rookiex.core.center.EtcdRegisterCenterImpl;
import cn.rookiex.core.lister.WatchServiceLister;
import cn.rookiex.core.service.Service;
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
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    /**
     * 本机注册的租约
     */
    private Lease leaseClient;
    /**
     * 本机注册的服务名字
     */
    private String serviceName;
    /**
     * 本级注册的ip
     */
    private String ip;
    /**
     * 服务开启状态
     */
    private String isOpen = OPEN;
    /**
     * 心跳开关
     */
    private volatile boolean keepAlive = true;
    /**
     * 注册状态开关
     */
    private volatile boolean registryOK = false;

    private KV kvClient;
    private Watch watchClient;

    private ScheduledExecutorService keepAliveService;
    private ExecutorService executorService;

    private Map<String, List<WatchServiceLister>> watchServiceListMap = Maps.newConcurrentMap();
    private Map<String, AtomicBoolean> watchTaskRunMap = Maps.newConcurrentMap();
    private static final int ETCD_TIME_OUT = 30000;

    @Override
    public void init(String url) {
        String[] urls = url.split(";");
        List<String> urlList = Lists.newArrayList();
        Client client;
        if (urls.length > 1) {
            urlList.addAll(Arrays.asList(urls));
            client = Client.builder().endpoints(urlList).build();
        } else {
            client = Client.builder().endpoints(url).build();
        }
        this.leaseClient = client.getLeaseClient();
        this.kvClient = client.getKVClient();
        this.watchClient = client.getWatchClient();
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void init(String url, String user, String password) {
        String[] urls = url.split(";");
        List<String> urlList = Lists.newArrayList();
        Client client;
        if (urls.length > 1) {
            urlList.addAll(Arrays.asList(urls));
            client = Client.builder().endpoints(urlList).authority(user).password(ByteSequence.fromString(password)).build();
        } else {
            client = Client.builder().endpoints(url).authority(user).password(ByteSequence.fromString(password)).build();
        }
        this.leaseClient = client.getLeaseClient();
        this.kvClient = client.getKVClient();
        this.watchClient = client.getWatchClient();
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * 获取服务信息
     *
     * @param serviceName 服务名字
     * @param usePrefix   是否使用前缀
     */
    @Override
    public List<Service> getServiceList(String serviceName, boolean usePrefix) {
        if (!usePrefix && !serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName += RegistryConstants.SEPARATOR;
        }
        List<Service> serviceList = Lists.newCopyOnWriteArrayList();
        try {
            ByteSequence seqKey = ByteSequence.fromString(serviceName);
            GetResponse response;
            if (usePrefix) {
                response = kvClient.get(seqKey, GetOption.newBuilder().withPrefix(ByteSequence.fromString(serviceName)).build()).get(ETCD_TIME_OUT, TimeUnit.MILLISECONDS);
            } else {
                response = kvClient.get(seqKey).get(ETCD_TIME_OUT, TimeUnit.MILLISECONDS);
            }
            List<KeyValue> kvs = response.getKvs();
            kvs.forEach(keyValue -> {
                ByteSequence key = keyValue.getKey();
                ByteSequence value = keyValue.getValue();
                long lease = keyValue.getLease();
                String keyS = key.toStringUtf8();
                long version = keyValue.getVersion();
                Service service = EtcdRegisterCenterImpl.factory.getService(keyS, value.toStringUtf8().equals(EtcdRegistryImpl.BAN), lease, version);
                if (service != null)
                    serviceList.add(service);
            });
            return serviceList;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return serviceList;
    }

    /**
     * 获得serviceName下的所有服务,默认开启前缀条件
     *
     * @param serviceName serviceName
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
    public void registerService(String serviceName, String ip) throws ExecutionException, InterruptedException, TimeoutException {
        setServiceName(serviceName);
        setIp(ip);
        this.isOpen = OPEN;
        ByteSequence key = getServiceKey(serviceName, ip);
        ByteSequence val = ByteSequence.fromString(isOpen);
        LeaseGrantResponse leaseGrantResponse = leaseClient.grant(TTL_TIME).get();
        leaseId = leaseGrantResponse.getID();
        kvClient.put(key, val, PutOption.newBuilder().withLeaseId(leaseId).build()).get(ETCD_TIME_OUT, TimeUnit.MILLISECONDS);
        keepAlive();
        registryOK = true;
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
    public void bandService(String serviceName, String ip) throws ExecutionException, InterruptedException, TimeoutException {
        this.isOpen = BAN;
        ByteSequence key = getServiceKey(serviceName, ip);
        ByteSequence val = ByteSequence.fromString(BAN);
        kvClient.put(key, val).get(ETCD_TIME_OUT, TimeUnit.MILLISECONDS);
        keepAlive();
    }

    private void shutdown() {
        this.keepAlive = false;
    }

    /**
     * 发送心跳到ETCD,表明该host是活着的
     */
    private void keepAlive() {
        if (keepAliveService != null)
            keepAliveService.shutdown();
        keepAliveService = Executors.newSingleThreadScheduledExecutor();
        keepAliveService.scheduleAtFixedRate(() -> {
            if (!keepAlive) {
                this.keepAliveService.shutdown();
            } else {
                if (registryOK) {
                    try {
                        registerService(getServiceName(), getIp());
                    } catch (ExecutionException | InterruptedException | TimeoutException e1) {
                        logger.error("重试注册服务器出现异常 ", e1);
                    }
                } else {
                    try {
                        CompletableFuture<LeaseKeepAliveResponse> responseCompletableFuture = leaseClient.keepAliveOnce(leaseId);
                        LeaseKeepAliveResponse leaseKeepAliveResponse = responseCompletableFuture.get();
                        logger.debug("KeepAlive  ttl == " + leaseKeepAliveResponse.getTTL() + " lease:" + leaseKeepAliveResponse.getID()
                                + "; Hex format:" + Long.toHexString(leaseKeepAliveResponse.getID()));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        registryOK = false;
                    }
                }
            }
        }, TTL_TIME / 3, TTL_TIME / 3, TimeUnit.SECONDS);
    }

    /**
     * 监听服务
     *
     * @param serviceName
     * @param usePrefix
     * @param watchList
     */
    @Override
    public void watch(String serviceName, boolean usePrefix, List<WatchServiceLister> watchList) {
        if (!usePrefix && !serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName += RegistryConstants.SEPARATOR;
        }
        if (watchList == null) {
            logger.warn("watch service -> " + serviceName + " ,center is null !!!!!!!!!!");
            return;
        }
        watchServiceListMap.merge(serviceName, watchList, (v1, v2) -> {
            List<WatchServiceLister> list = Lists.newCopyOnWriteArrayList();
            list.addAll(v1);
            list.addAll(v2);
            return list;
        });
        dealWatch(serviceName, usePrefix);
    }

    /**
     * 监听服务
     *
     * @param serviceName
     * @param usePrefix
     * @param lister
     */
    @Override
    public void watch(String serviceName, boolean usePrefix, WatchServiceLister lister) {
        watch(serviceName, usePrefix, Lists.newArrayList(lister));
    }

    /**
     * 监听服务,默认开启前缀条件
     *
     * @param serviceName
     * @param watchList
     */
    @Override
    public void watch(String serviceName, List<WatchServiceLister> watchList) {
        watch(serviceName, true, watchList);
    }

    /**
     * 取消监听服务
     *
     * @param serviceName
     * @param usePrefix
     * @param watchList
     */
    @Override
    public void unWatch(String serviceName, boolean usePrefix, List<WatchServiceLister> watchList) {
        watchServiceListMap.forEach((k, v) -> {
            if (serviceName.equals(RegistryConstants.WATCH_ALL)) {
                watchServiceListMap.forEach((k1, v1) -> {
                    if (usePrefix) {
                        if (k1.startsWith(serviceName)) {
                            v1.removeAll(watchList);
                        }
                    } else {
                        if (k1.equals(serviceName)) {
                            v1.removeAll(watchList);
                        }
                    }
                });
            } else {
                if (usePrefix) {
                    if (k.startsWith(serviceName)) {
                        List<WatchServiceLister> watchServiceListers = watchServiceListMap.get(k);
                        if (watchServiceListers != null)
                            watchServiceListers.removeAll(watchList);
                    }
                } else {
                    if (k.equals(serviceName)) {
                        List<WatchServiceLister> watchServiceListers = watchServiceListMap.get(k);
                        if (watchServiceListers != null)
                            watchServiceListers.removeAll(watchList);
                    }
                }
            }
        });
    }

    /**
     * 取消监听服务,默认开启前缀条件
     *
     * @param serviceName
     * @param watchList
     */
    @Override
    public void unWatch(String serviceName, List<WatchServiceLister> watchList) {
        unWatch(serviceName, true, watchList);
    }

    /**
     * 取消监听服务,默认开启前缀条件
     *
     * @param serviceName
     * @param usePrefix
     * @param lister
     */
    @Override
    public void unWatch(String serviceName, boolean usePrefix, WatchServiceLister lister) {
        unWatch(serviceName, usePrefix, Lists.newArrayList(lister));
    }


    private void dealWatch(String serviceName, Boolean usePrefix) {
        if (serviceName.equals(RegistryConstants.WATCH_ALL)) {
            return;
        }
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        AtomicBoolean atomicBoolean2 = watchTaskRunMap.putIfAbsent(serviceName, atomicBoolean);
        if (atomicBoolean2 == null) {
            atomicBoolean2 = atomicBoolean;
        }
        if (atomicBoolean2.compareAndSet(false, true)) {
            executorService.execute(() -> {
                try {
                    Watch.Watcher watcher;
                    if (usePrefix) {
                        watcher = watchClient.watch(ByteSequence.fromString(serviceName), WatchOption.newBuilder().withPrefix(ByteSequence.fromString(serviceName)).build());
                    } else {
                        watcher = watchClient.watch(ByteSequence.fromString(serviceName));
                    }
                    List<WatchEvent> events = watcher.listen().getEvents();
                    List<WatchServiceLister> watchList = watchServiceListMap.get(serviceName);
                    callBackUpdateEvents(events, watchList);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    if (needWatchService(serviceName))
                        this.dealWatch(serviceName, usePrefix);
                    else
                        atomicBoolean.compareAndSet(true, false);
                }
            });
        }
        if (!atomicBoolean.get()) {
            this.dealWatch(serviceName, usePrefix);
        }
    }

    private boolean needWatchService(String serviceName) {
        return watchServiceListMap.get(serviceName) != null && !watchServiceListMap.get(serviceName).isEmpty();
    }


    private void callBackUpdateEvents(List<WatchEvent> events, List<WatchServiceLister> watchServiceListers) {
        List<ServiceUpdateEvent> updateEvents = Lists.newCopyOnWriteArrayList();
        events.forEach(event -> {
            EtcdServiceUpdateEventImpl updateEvent = new EtcdServiceUpdateEventImpl();
            WatchEvent.EventType eventType = event.getEventType();
            KeyValue keyValue = event.getKeyValue();
            KeyValue prevKV = event.getPrevKV();
            String keyS = keyValue.getKey().toStringUtf8();
            String[] split = keyS.split(RegistryConstants.SEPARATOR);
            if (split.length == 2) {
                String serviceName = split[0] + RegistryConstants.SEPARATOR;
                updateEvent.setEventType(eventType);
                updateEvent.setServiceName(serviceName);
                updateEvent.setKeyValue(keyValue);
                updateEvent.setPrevKV(prevKV);
                updateEvent.setFullPath(keyS);
                updateEvent.setVersion(keyValue.getVersion());
                updateEvents.add(updateEvent);
            }
        });
        watchServiceListers.forEach(lister -> lister.watchCallback(updateEvents));
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}

package com.github.memcached.server;

import com.github.memcached.common.Request;
import com.github.memcached.common.Response;
import com.github.memcached.common.utils.Closer;
import com.github.memcached.common.utils.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.memcached.common.utils.MemcachedConfig.*;

public class MemcachedServer {
    private static final Logger logger = LoggerFactory.getLogger(MemcachedServer.class);

    private static final Map<String, MemcachedValue> cache = new ConcurrentHashMap<>();

    private static final String ERROR_MESSAGE = "illegal operation";

    private static final int NEVER_EXPIRED_TIME_STAMP = -1;

    private ExecutorService executor;

    private ScheduledExecutorService scheduledExecutor;

    private int port;

    private boolean started;

    private ServerSocket serverSocket;

    public MemcachedServer() {
        started = false;
        initPort();
        initExecutor();
        initScheduleExecutor();
    }

    /**
     * 初始化端口号
     */
    private void initPort() {
        this.port = Integer.parseInt(System.getProperty(MEMCACHED_SERVER_PORT, DEFAULT_MEMCACHED_SERVER_PORT));
    }

    /**
     * 初始化线程池
     */
    private void initExecutor() {
        int poolSize = Integer.parseInt(System.getProperty(MEMCACHED_SERVER_POOL_SIZE, DEFAULT_MEMCACHED_SERVER_POOL_SIZE));
        executor = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private AtomicLong atomicLong = new AtomicLong(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                // 设置线程名称，区分普通线程
                thread.setName("memcached-thread-" + atomicLong.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    /**
     * 初始化定時清理過期數據
     */
    private void initScheduleExecutor() {
        this.scheduledExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                // 设置线程名称，区分普通线程
                thread.setName("clear-expired-value-thread");
                thread.setDaemon(true);
                return thread;
            }
        });
        int period = Integer.valueOf(System.getProperty(MEMCACHED_SERVER_SCHEDULE_PERIOD, DEFAULT_MEMCACHED_SERVER_SCHEDULE_PERIOD));
        this.scheduledExecutor.scheduleAtFixedRate(new ClearExpiredValueTask(), period, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 启动服务器
     */
    public synchronized void start() {
        try {
            if (started) {
                return;
            } else {
                started = true;
                logger.info("start memcached server successfully.");
            }

            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            while (started) {
                final Socket socket = serverSocket.accept();
                // 异步执行
                executor.execute(new Task(socket));
            }
        } catch (Exception e) {
            logger.error("happened unknown exception and will close sever, detail : ", e);
        } finally {
            stop();
        }
    }

    /**
     * 关闭服务器
     */
    public synchronized void stop() {
        if (!started) {
            return;
        } else {
            started = false;
        }

        Closer.closeQuietly(serverSocket);
        executor.shutdownNow();
        scheduledExecutor.shutdownNow();
        logger.info("close memecached server successfully.");
    }

    /**
     * 处理客户端请求
     */
    public class Task implements Runnable {
        private Socket socket;

        public Task(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            boolean running = true;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            while (running) {
                Request request = new Request();
                Response response = new Response();
                try {
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    // 从流中读取数据
                    request.readFrom(dataInputStream);
                    InetSocketAddress remoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
                    logger.info("receiver data : {} from client  host : {}, port : {}", request, remoteSocketAddress.getHostString(), remoteSocketAddress.getPort());
                    // 解析指令
                    switch (Operation.from(request.getOperation())) {
                        case DELETE:
                            cache.remove(request.getKey());
                            response.setOperation(Operation.SUCCESS.getCode());
                            response.setMessageLength(0);
                            response.setMessage(new byte[0]);
                            break;
                        case GET:
                            MemcachedValue memcachedValue = cache.get(request.getKey());
                            response.setOperation(Operation.SUCCESS.getCode());
                            if (memcachedValue == null) {
                                response.setMessageLength(0);
                                response.setMessage(new byte[0]);
                            } else {
                                // 未過期
                                long expiredTimestamp = memcachedValue.getExpiredTimestamp();
                                if (expiredTimestamp == -1 || expiredTimestamp > System.currentTimeMillis()) {
                                    byte[] valueBytes = memcachedValue.getValue().getBytes();
                                    response.setMessageLength(valueBytes.length);
                                    response.setMessage(valueBytes);
                                } else {
                                    response.setMessageLength(0);
                                    response.setMessage(new byte[0]);
                                }
                            }
                            break;
                        case SET:
                            int ttl = request.getTtl();
                            if (ttl > 0) {
                                long expiredTimestamp = System.currentTimeMillis() + ttl;
                                cache.put(request.getKey(), new MemcachedValue(request.getValue(), expiredTimestamp));
                            } else {
                                cache.put(request.getKey(), new MemcachedValue(request.getValue(), NEVER_EXPIRED_TIME_STAMP));
                            }
                            response.setOperation(Operation.SUCCESS.getCode());
                            response.setMessageLength(0);
                            response.setMessage(new byte[0]);
                            break;
                        default:
                            response.setOperation(Operation.FAIL.getCode());
                            byte[] errorMessageBytes = ERROR_MESSAGE.getBytes();
                            response.setMessageLength(errorMessageBytes.length);
                            response.setMessage(errorMessageBytes);
                    }
                    // 输出响应
                    response.writeTo(dataOutputStream);
                } catch (SocketException e) {
                    running = false;
                    logger.error("happened socket exception and will close socket, detail : ", e);
                } catch (IOException e) {
                    response = new Response();
                    response.setOperation(Operation.FAIL.getCode());
                    String message = e.getMessage();
                    byte[] bytes = message.getBytes();
                    response.setMessageLength(bytes.length);
                    response.setMessage(bytes);
                    logger.error("happened IO exception, detail : ", e);
                } catch (Exception e) {
                    logger.error("happened unknown exception, detail : ", e);
                } finally {
                    if (!running) {
                        Closer.closeQuietly(dataInputStream);
                        Closer.closeQuietly(dataOutputStream);
                        Closer.closeQuietly(socket);
                        logger.info("socket happened exception and close socket successfully.");
                    }
                }
            }
        }
    }

    public class ClearExpiredValueTask implements Runnable {
        @Override
        public void run() {
            Iterator<Map.Entry<String, MemcachedValue>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, MemcachedValue> entry = iterator.next();
                long expiredTimestamp = entry.getValue().getExpiredTimestamp();
                if (expiredTimestamp > 0 && expiredTimestamp < System.currentTimeMillis()) {
                    iterator.remove();
                    logger.info("key : \"{}\", value : \"{}\" has expired and remove.", entry.getKey(), entry.getValue().getValue());
                }
            }
        }
    }

    public static void main(String[] args) {
        MemcachedServer memcachedServer = new MemcachedServer();
        memcachedServer.start();
    }
}

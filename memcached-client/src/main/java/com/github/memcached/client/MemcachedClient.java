package com.github.memcached.client;

import com.github.memcached.common.Request;
import com.github.memcached.common.Response;
import com.github.memcached.common.utils.Closer;
import com.github.memcached.common.utils.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import static com.github.memcached.common.utils.MemcachedConfig.*;

public class MemcachedClient {
    private static final Logger logger = LoggerFactory.getLogger(MemcachedClient.class);

    private static String ILLEGAL_OPERATION = "illegal operation";

    private static String OPERATION_CLOSE = "close";

    public static void main(String[] args) {
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;
        Socket socket = null;
        String host = System.getProperty(MEMCACHED_SERVER_HOST, DEFAULT_MEMCACHED_SERVER_HOST);
        int port = Integer.parseInt(System.getProperty(MEMCACHED_SERVER_PORT, DEFAULT_MEMCACHED_SERVER_PORT));
        try {
            socket = new Socket(host, port);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            // 轮询读取输入的命令
            while (running) {
                try {
                    System.out.print("memcache > ");
                    String message = scanner.nextLine().toLowerCase();
                    if (OPERATION_CLOSE.equals(message)) {
                        running = false;
                    } else {
                        String[] split = message.trim().split("\\s+");
                        if (split.length < 2 || split.length > 4) {
                            System.out.println(ILLEGAL_OPERATION);
                        } else {
                            Operation operation = Operation.valueOf(split[0].toUpperCase());
                            Request request = new Request();
                            Response response = new Response();
                            String key = split[1];
                            switch (operation) {
                                case SET:
                                    if (split.length < 3) {
                                        System.out.println(ILLEGAL_OPERATION);
                                    } else {
                                        request.setOperation(Operation.SET.getCode());
                                        String value = split[2];
                                        request.setValue(value);
                                        request.setKey(key);
                                        if (split.length > 3) {
                                            request.setTtl(Integer.parseInt(split[3]));
                                        }
                                        request.writeTo(dataOutputStream);
                                        response.readFrom(dataInputStream);
                                        printResponse(Operation.SET, response);
                                    }
                                    break;
                                case GET:
                                    request.setOperation(Operation.GET.getCode());
                                    request.setKey(key);
                                    request.writeTo(dataOutputStream);
                                    response.readFrom(dataInputStream);
                                    printResponse(Operation.GET, response);
                                    break;
                                case DELETE:
                                    request.setOperation(Operation.DELETE.getCode());
                                    request.setKey(key);
                                    request.writeTo(dataOutputStream);
                                    response.readFrom(dataInputStream);
                                    printResponse(Operation.DELETE, response);
                                    break;
                                default:
                                    System.out.println(ILLEGAL_OPERATION);
                            }
                        }
                    }
                } catch (SocketException e) {
                    running = false;
                    logger.error("happened socket exception and will close socket, detail : ", e);
                } catch (IOException e) {
                    running = false;
                    logger.error("happened IO exception and will close socket, detail : ", e);
                } catch (Exception e) {
                    logger.error("happened unknown exception, detail : ", e);
                }
            }
        } catch (UnknownHostException e) {
            logger.error("unknown host : {}, port : {} exception, detail : ", host, port, e);
        } catch (IOException e) {
            logger.error("happened socket IO exception, detail : ", e);
        } catch (Exception e) {
            logger.error("happened unknown exception, detail : ", e);
        } finally {
            Closer.closeQuietly(dataInputStream);
            Closer.closeQuietly(dataOutputStream);
            Closer.closeQuietly(socket);
            logger.info("close memcached client successfully.");
        }

    }

    /**
     * 输出响应
     *
     * @param operation
     * @param response
     */
    private static void printResponse(Operation operation, Response response) {
        if (response.getOperation() == Operation.FAIL.getCode()) {
            System.out.println("error : " + new String(response.getMessage()));
        } else {
            if (operation == Operation.GET) {
                System.out.println("ok : " + new String(response.getMessage()));
            } else {
                System.out.println("ok");
            }

        }
    }
}

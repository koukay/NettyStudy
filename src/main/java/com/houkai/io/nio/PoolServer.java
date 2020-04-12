package com.houkai.io.nio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PoolServer {
    ExecutorService pool = Executors.newFixedThreadPool(50);
    private Selector selector;

    public static void main(String[] args) throws IOException {
        PoolServer server= new PoolServer();
        server.initServer(8000);
        server.listen();
    }

    private void listen() throws IOException {
        //轮询访问selector
        selector.select();
        while (true){

            Iterator<SelectionKey> it = this.selector.selectedKeys().iterator();
            while (it.hasNext()){
                SelectionKey key=it.next();
                it.remove();
                if (key.isAcceptable()){
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    sc.register(this.selector,SelectionKey.OP_READ);
                }else if(key.isReadable()){
                    key.interestOps(key.interestOps()&(~SelectionKey.OP_READ));
                    pool.execute(new ThreadHandlerChannel(key));
                }
            }
        }
    }

    private void initServer(int port) {
        try {
            ServerSocketChannel ssc=ServerSocketChannel.open();

            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(port));
            this.selector=Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务端启动成功");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class ThreadHandlerChannel extends Thread{
    private SelectionKey key;

    public ThreadHandlerChannel(SelectionKey key) {
        this.key = key;
    }
    @Override
    public void run(){
        SocketChannel channel= (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int size=0;
            while ((size=channel.read(buffer))>0){
                buffer.flip();
                baos.write(buffer.array(),0,size);
                buffer.clear();
            }
            baos.close();
            byte[] content = baos.toByteArray();
            ByteBuffer writeBuf = ByteBuffer.allocate(content.length);
            writeBuf.put(content);
            writeBuf.flip();
            channel.write(writeBuf);
            if (size==-1){
                channel.close();
            }else {
                key.interestOps(key.interestOps()|SelectionKey.OP_READ);
                key.selector().wakeup();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
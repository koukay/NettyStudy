package com.houkai.io.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Server {
    public static void main(String[] args) throws IOException, InterruptedException {
       final AsynchronousServerSocketChannel assc= AsynchronousServerSocketChannel.open()
               .bind(new InetSocketAddress(8888));
        assc.accept(
                null,new CompletionHandler<AsynchronousSocketChannel,Object>(){
                    @Override
                    public void completed(AsynchronousSocketChannel client, Object attachment) {
                        assc.accept(null,this);
                        try {
                            System.out.println(client.getRemoteAddress());
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            client.read(buffer,buffer ,new CompletionHandler<Integer,ByteBuffer>(){

                                @Override
                                public void completed(Integer result, ByteBuffer attachment) {
                                    attachment.flip();
                                    System.out.println(new String(attachment.array(),0,result));
                                    client.write(ByteBuffer.wrap("HelloClient aio".getBytes()));
                                }

                                @Override
                                public void failed(Throwable exc, ByteBuffer attachment) {
                                    exc.printStackTrace();
                                }
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        exc.printStackTrace();
                    }
                }
        );
        while (true){
            Thread.sleep(1000);
        }

    }


}

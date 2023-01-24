package nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于BIO的服务端
 */
public class NioServer {
	
	public static void main(String[] args) throws Exception{
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(13);
        serverSocketChannel.socket().bind(inetSocketAddress);
        //设置成非阻塞
        serverSocketChannel.configureBlocking(false); 
        Selector selector = Selector.open();
        // 对serverSocket仅监听ACCEPT事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
 
        while(true) {
            selector.select();
            //遍历selectionKeys
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                //处理连接事件
                if(key.isAcceptable()) {  
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    //设置为非阻塞
                    socketChannel.configureBlocking(false);  
                    System.out.println("client:" + socketChannel.getRemoteAddress() + " connected");
                    //注册socket的READ事件到selector
                    socketChannel.register(selector, SelectionKey.OP_READ); 
                    //处理读取事件
                } else if (key.isReadable()) {  
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    SocketChannel channel = (SocketChannel) key.channel();
					int len = channel.read(byteBuffer);
					if (len >= 0) {
						byte[] array = byteBuffer.array();
						String msg = new String(array);
						System.out.println("receive " + msg + " from client:" + channel.getRemoteAddress());
					}
					// <0说明客户端断开（一定要处理，否则会一直收到READ事件）
					else {
						System.out.println("client:" + channel.getRemoteAddress() + " disconnected");
						channel.close();
					}
                }
                //事件处理完毕，要记得清除
                iterator.remove();  
            }
        }
    }
}

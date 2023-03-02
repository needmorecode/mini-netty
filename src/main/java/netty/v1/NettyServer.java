package netty.v1;

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
 * mini-netty的服务端
 */
public class NettyServer {
	
	/**
	 * worker线程池
	 */
	private static List<NioEventLoop> eventLoops = new ArrayList<>();
	
	/**
	 * 下一次轮询到的下标
	 */
	private static int nextIndex = 0;
	
	/**
	 * worker线程池中线程数量
	 */
	private static final int WORKER_THREAD_NUM = 8;
	
	static {
		// 做一些worker线程池的初始化工作
		for (int i = 1; i <= WORKER_THREAD_NUM; i++) {
			NioEventLoop el = new NioEventLoop();
			el.start();
			eventLoops.add(el);
		}
	}
	
	/**
	 * 获取下一个event loop
	 */
	private static NioEventLoop getNextEventLoop() {
		NioEventLoop nextEventLoop = eventLoops.get(nextIndex);
		nextIndex++;
		if (nextIndex > eventLoops.size() - 1) {
			nextIndex = 0;
		}
		return nextEventLoop;
	}
	
	public static void main(String[] args) throws  Exception{
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(13);
        serverSocketChannel.socket().bind(inetSocketAddress);
        //设置成非阻塞
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        int num = 0;
        while(true) {
        	//监听所有通道
            selector.select();
            //遍历selectionKeys
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                //处理连接事件
                if(key.isAcceptable()) {  
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    System.out.println("client:" + socketChannel.getRemoteAddress() + " connected");
                    //设置为非阻塞
                    socketChannel.configureBlocking(false);  
                    System.out.println("bind new channel to event loop" + nextIndex);
                    NioEventLoop nextEventLoop = getNextEventLoop();
                    //注册socket的READ事件到所选eventLoop的selector
                    nextEventLoop.getSelector().wakeup();
                    socketChannel.register(nextEventLoop.getSelector(), SelectionKey.OP_READ);
                    num++;
                    System.out.println(num + " clients connected");
                }
                //事件处理完毕，要记得清除
                iterator.remove();  
            }
        }
 
    }

}

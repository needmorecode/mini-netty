package netty.v1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.jctools.queues.MpscUnboundedArrayQueue;

/**
 * worker线程池中的线程
 * 功能类似于Netty中的同名类
 */
public class NioEventLoop extends Thread {

	public NioEventLoop() {
		try {
			//构造方法中初始化自己的selector
			this.selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Selector selector;
	
	public Selector getSelector() {
		return this.selector;
	}
	
	@Override
	public void run() {
	       while (true) {
	    	   int selectNum = 0;
	    	   try {
	    		    //select超时时间设为10ms，避免一直阻塞
		    	   	selectNum = selector.select();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	    if (selectNum > 0) {
		            //监听所有通道
		            //遍历selectionKeys
		            Set<SelectionKey> selectionKeys = selector.selectedKeys();
		            Iterator<SelectionKey> iterator = selectionKeys.iterator();
		            while (iterator.hasNext()) {
		                SelectionKey key = iterator.next();
		                //处理读取事件
		                if (key.isReadable()) {  
		                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
		                    SocketChannel channel = (SocketChannel) key.channel();
		                    try {
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
							} catch (IOException e) {
								e.printStackTrace();
							}
		                }
		                //事件处理完毕，要记得清除
		                iterator.remove();  
		            }
	    	    }
	        }
	}

}

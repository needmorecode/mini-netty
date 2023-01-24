package netty.v2;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * mini-netty的客户端
 */
public class NettyClient {
	 
	public static void main(String[] args) throws Exception{
		// 启动16个socket分别发送消息
		for (int i = 1; i <= 16; i++) {
			InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 13);
	        SocketChannel socketChannel = SocketChannel.open(inetSocketAddress);
	        socketChannel.configureBlocking(false);
	        String msg = "msg" + i;
	        ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());
	        System.out.println("socket" + i + " send msg:" + msg);
	        socketChannel.write(byteBuffer);
	        socketChannel.close();
	        Thread.sleep(100);
		}
	}
}

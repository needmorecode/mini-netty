package bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 基于BIO的服务端
 */
public class BioServer {
    public static void main(String[] args) throws IOException{
        try (ServerSocket ss = new ServerSocket(13)) {
			while(true){
				// 循环接收新的连接，并启动新的线程处理对应连接的IO消息
			    Socket socket = ss.accept();
			    new Thread(){
			    	@Override
			    	public void run() {
			            try{
			                InputStream is = socket.getInputStream();
			                BufferedReader br = new BufferedReader(new InputStreamReader(is));
			                String msg;
			                while((msg = br.readLine())!=null){
			                    System.out.println("receive msg:" + msg);
			                }
			            }catch (Exception e){
			                e.printStackTrace();
			            }
			    	}
			    	
			    }.start();
			}
		}
    }
}


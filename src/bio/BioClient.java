package bio;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * 基于BIO的客户端
 */
public class BioClient {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 13)) {
			OutputStream os = socket.getOutputStream();
			PrintStream ps = new PrintStream(os);
			try (Scanner sc = new Scanner(System.in)) {
			    while(true){
			    	// 循环从系统输入获取消息并发送
			    	System.out.print("type in your msg:");
			        String msg = sc.nextLine();
			        ps.println(msg);
			        ps.flush();
			    }
			}
		}
    }
}



package http.demo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;

public class HTTPServer {
	private final int PORT;			//�˿ں�
	private String last = "";		//�ϴ�������¼
	private String indexHtml = "";	//�ļ� index.html
	public HTTPServer(int port) throws IOException {
		PORT = port;
		//���� index.html �ļ�
		BufferedReader bufferedReader = new BufferedReader(new FileReader("html" + File.separator + "index.html"));
		String buffer = bufferedReader.readLine();
		StringBuilder builder = new StringBuilder();
		while (buffer != null) {
			builder.append(buffer);
			buffer = bufferedReader.readLine();
		}
		bufferedReader.close();
		indexHtml = builder.toString();
	}
	public void start() throws Exception {
		ServerSocket serverSocket = new ServerSocket(PORT);
		//����������
		Engine engine = new Engine();
		System.out.println("HttpServer start...");
		
		while (true) {
			//����http���󲢷���
			Socket socket = serverSocket.accept();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String header = bufferedReader.readLine();
			System.out.println(header);
			if (header == null) {
				continue;
			}
			String path = header.split(" ")[1];
			String html, key = "";
			//main page or searching page
			int index = 0;
			//��������
			if (path.indexOf("/?search=") == 0) {
				key = URLDecoder.decode(path.substring("/?search=".length()), "utf-8");
				last = key;
			}
			//��ҳ����
			else if (path.matches("/\\d+\\.index")) {
				index = Integer.parseInt(path.substring(1,path.indexOf(".")));
			}
			//�հ�����
			if (key.replaceAll("\\s+", "").isEmpty() == false) {
				html = engine.search(key);
			}
			//���ض�Ӧ��ҳ���������
			else if (index != 0) {
				html = engine.search(last, index - 1); // index begin from 0
			}
			else {
				html = indexHtml;
			}
			//���ɷ��ؽ����httpͷ��
			OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), "utf-8");
			writer.write("HTTP/1.1 200 OK\n");
			writer.write("Content-type:text/html;charset=UTF-8\n");
			writer.write("\n");
			writer.write(html);
			
			writer.flush();
			socket.close();
		}
	}
	public static void main(String[] args) throws IOException{
		//����������
		HTTPServer server = new HTTPServer(8080);
		while (true) {
			try {
				server.start();
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println("HttpServer crash!...restart now");
			}
		}
	}
}

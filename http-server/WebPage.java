package http.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebPage {
	private String page;		//html�ļ�
	private static final String HTMLFOLDER = "html";
	public WebPage(int index) throws IOException {
		//��ȡhtml�ļ�
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
				HTMLFOLDER + File.separator + index + ".html"), "gbk"));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line != null) {
			builder.append(line);
			line = reader.readLine();
		}
		reader.close();
		page = builder.toString();
	}
	//ͨ��ģ���������Ϊ��ץȡhtmlҳ�棬�˹��췽��������
	public WebPage(String url) throws Exception {
		URL readlUrl = new URL("http://" + url);
		URLConnection conn = readlUrl.openConnection();
		conn.setRequestProperty("accept", "*/*");  
        conn.setRequestProperty("connection", "Keep-Alive");  
        conn.setRequestProperty("user-agent",  
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");  
        try {
        	conn.connect();
        }
        catch (Exception e) {
        	page = "";
        	return;
        }
        
        Map<String, List<String>> map = conn.getHeaderFields();
        List<String> strings = map.get("Content-Type"); 
        String charset = "utf-8";
        if (strings != null) {
        	for (String string : strings) {
        		if (string.indexOf("charset=") != -1) {
        			charset = string.substring(string.indexOf("charset=") + "charset=".length());
        		}
        }
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
        String line = reader.readLine();
        StringBuilder builder = new StringBuilder();
        while (line != null) {
        	builder.append(line + "\n");
        	line = reader.readLine();
        }
        page = builder.toString();
	}
	//���title
	public String getTitle() {
		String pattern = "<title>(.*)</title>";
		Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
		Matcher m = r.matcher(page);
		if (m.find() == false) {
			return "";
		}
		return m.group(1);
	}
	//���Ԥ����Ϣ
	public String getPreview(String[] keyArray) {
		// ��������ʽƥ��ؼ���
		StringBuilder keyPattern = new StringBuilder("(" + keyArray[0]);
		for (int i = 1; i < keyArray.length; i ++) {
			keyPattern.append("|").append(keyArray[i]);
		}
		keyPattern.append(")");
		String pattern = ">([^\"\'<>]*?" + keyPattern.toString() + "[^\"\'<>]*?)</(?!script|style)";
		Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
		// ƥ�� <body></body>��ǩ�ڵ��ַ���������ƥ�䵽����
		int body = page.indexOf("<body");
		if (body == -1) {
			body = 0;
		}
		Matcher m = r.matcher(page.substring(body));
		StringBuilder builder = new StringBuilder();
		ArrayList<String> strings = new ArrayList<String>(); 
		while (m.find()) {
			strings.add(m.group(1));
		}
		// �ٴ�ƥ��ȫ�ģ�����ǰ��ƥ�����Ϣ����
		pattern = ">([^\"\'<>]*?)</(?!script|style)";
		r = Pattern.compile(pattern, Pattern.DOTALL);
		m = r.matcher(page);
		while (m.find()) {
			strings.add(m.group(1));
		}
		
		for (String s : strings) {
			// �滻��html��ת���ַ�
			s = s.replaceAll("(\\s+|&\\w+;|&#\\d+;)", "");
			if (s.isEmpty()) {
				continue;
			}
			for (int i = 0; i < keyArray.length; i ++) {
				// ���ؼ������<em></em>��ǩ���ص�ǿ��
				s = s.replaceAll(keyArray[i], "<em>" + keyArray[i] + "</em>");
			}
			// �ÿո����
			builder.append(s + "&nbsp;&nbsp;");
		}
		return builder.toString();
	}
}

package http.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode;
import com.huaban.analysis.jieba.SegToken;

public class Engine {
	public final String AND = "AND", OR = "OR", NOT = "NOT";
	private Search searcher = new Search();			//������
	private String lastSearch = "";					//�ϴ�����
	private String resultHtml = "";					//�ļ�result.html
	JiebaSegmenter segmenter = new JiebaSegmenter();//����͡��ִ�
	public Engine() throws IOException {
		//����result.html�ļ�
		StringBuilder htmlBuilder = new StringBuilder();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(
				"html" + File.separator + "result.html"), "utf-8"));
		String buffer = bufferedReader.readLine();
		while (buffer != null) {
			htmlBuilder.append(buffer);
			buffer = bufferedReader.readLine();
		}
		bufferedReader.close();
		resultHtml = htmlBuilder.toString();
	}
	public String search(String keyString) throws Exception {
		return search(keyString, 0);
	}
	public String search(String keyString, int index) throws Exception {
		//�����������ִ��ִ�
		List<SegToken> tokens = segmenter.process(keyString, SegMode.SEARCH);
		List<String> keyList = new ArrayList<String>();
		for (SegToken token : tokens) {
			if (token.word.matches("\\s+")) {
				continue;
			}
				keyList.add(token.word);
		}
		
		String[] keyArray = new String[keyList.size()];
		if (keyString.equals(lastSearch) == false) {
			//������µ�����������������
			getResults((String[])keyList.toArray(keyArray));
			lastSearch = keyString;
		}
		for (int i = 0; i < keyList.size(); i ++) {
			//ȥ���������������Ӵ�
			if (keyList.get(i).equalsIgnoreCase(AND)|| keyList.get(i).equalsIgnoreCase(OR)
					|| keyList.get(i).equalsIgnoreCase(NOT)) {
				keyList.remove(i);
				i --;
			}
		}
		keyArray = new String[keyList.size()];
		//���ɷ��ؽ����html�ļ�
		return createHtml(keyString, (String[])keyList.toArray(keyArray), index);
	}
	
	private ArrayList<Result> results = new ArrayList<Result>();
	private void getResults(String[] keyArray) {
		results = searcher.search(keyArray);
	}
	
	private String createHtml(String keyString, String[] keyArray, int index) throws Exception {
		StringBuilder htmlBuilder = new StringBuilder(resultHtml);
		
		//����title���滻��result.html��
		int title = htmlBuilder.indexOf("<!--title-->");
		htmlBuilder.replace(title, title + "<!--title-->".length(), keyString);
		
		//����content���滻��result.html��
		String resultContent = createContent(keyArray, index);
		int content = htmlBuilder.indexOf("<!--content-->");
		htmlBuilder.replace(content, content + "<!--content-->".length(), resultContent);
		
		//����bottom���滻��result.html��
		String resultBottom = createBottom(index);
		int bottom = htmlBuilder.indexOf("<!--bottom-->");
		htmlBuilder.replace(bottom, bottom + "<!--bottom-->".length(), resultBottom);
		return htmlBuilder.toString();
	}
	private final int MAX_PER_PAGE = 10, MAX_INDEX = 10;
	private String createContent(String[] keyArray, int index) throws Exception {
		StringBuilder content = new StringBuilder();
		//����һҳ���������
		for (int i = index * 10; i < index * 10 + MAX_PER_PAGE && i < results.size(); i ++) {
			Result r = results.get(i);
			WebPage page = new WebPage(r.index);
			String title = page.getTitle();
			if (title.isEmpty()) { // �Ҳ���title�����������
				results.remove(i);
				i --;
				continue;
			}
			content.append("<div class=\"result\"><h3><a href=\"http://" + r.url + "\">");
			
			content.append(title);
			content.append("</a></h3>" + "<cite>pagerank:" + r.pagerank + " - degreerank"
					+ r.degreerank + "</cite><p class=\"rp\">");
			// getPreview(key): �����ҪԤ������Ϣ
			content.append(page.getPreview(keyArray));
			content.append("</p></div>");
		}
		return content.toString();
	}
	private String createBottom(int index) {
		index ++;
		StringBuilder bottom = new StringBuilder();
		//���ɵײ���html���룺����Ӧ��ҳ����ҳ�Ľ�������
		if (index > 1) {
			bottom.append("<a class=\"index\" id=\"pre\" href=\"/" + (index - 1) 
					+ ".index\">&lt&lt</a>");
		}
		int beg = index > MAX_INDEX / 3 ? index - MAX_INDEX / 3 : 1;
		for (int i = beg; i <= results.size() / MAX_PER_PAGE && i - beg < MAX_INDEX; i ++) {
			if (i == index) {
				bottom.append("<strong class=\"index\" id=\"cur\">" + index + "</strong>");
			}
			else {
				bottom.append("<a class=\"index\" href=\"/" + i + ".index\">" + i + "</a>");
			}
		}
		if (index < results.size() / MAX_PER_PAGE) {
			bottom.append("<a class=\"index\" id=\"suc\" href=\"/" + (index + 1) 
					+ ".index\">&gt&gt</a>");
		}
		return bottom.toString();
	}
}


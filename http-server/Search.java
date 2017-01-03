package http.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

class Result {
	public String url;		//url��ַ
	public int index;		//��ҳ��ţ�Ԥ���ѱ�ţ�
	public double pagerank;	//pagerank����
	public int degreerank;	//���ֵ
	public Result(int index) {
		this.index = index;
	}
}

public class Search {
	//������Ϊcppʵ�ֵĺ�����ͨ������Search��̬��������
	private native void initData();
	private native Result[] searchResults(String[] keyArray);
	static {
		System.loadLibrary("Search");
	}
	private ArrayList<String> urls= new ArrayList<String>();
	private ArrayList<Double> pageranks = new ArrayList<Double>();
	private ArrayList<Integer> degreeranks = new ArrayList<Integer>();
	public Search() throws IOException {
		//cpp��������ʼ������������Ϣ
		initData();
		//����url, pagerank, degreerank��Ϣ
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
				"html" + File.separator + "pagelist.txt"), "gbk"));
		String line;
		while ((line = reader.readLine()) != null) {
			int beg = line.indexOf("://") + "://".length();
			if (beg < "://".length()) { beg = 0; }
			urls.add(line.substring(beg));
		}
		reader.close();
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(
				"html" + File.separator + "pagerank.txt"), "gbk"));
		while ((line = reader.readLine()) != null) {
			pageranks.add(new Double(line));
		}
		reader.close();
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(
				"html" + File.separator + "degreerank.txt"), "gbk"));
		while ((line = reader.readLine()) != null) {
			degreeranks.add(new Integer(line));
		}
		reader.close();
	}
	public ArrayList<Result> search(String[] keyArray) {
		ArrayList<Result> results = new ArrayList<Result>();
		//����cpp���������������˽ӿڰ����˲��������Ĺ���
		Result[] results2 = searchResults(keyArray);
		for (int i = 0; i < results2.length; i ++) {
			try {
				//���������Ϣ
				results2[i].url = urls.get(results2[i].index);
				results2[i].pagerank = pageranks.get(results2[i].index);
				results2[i].degreerank = degreeranks.get(results2[i].index);
				results.add(results2[i]);
			}
			catch (Exception e) {
				continue;
			}
		}
		//�������򣬸���pagerankֵ
		results.sort(new Comparator<Result>() {
			@Override
			public int compare(Result o1, Result o2) {
				if (o1.pagerank == o2.pagerank) { return 0; }
				if (o1.pagerank > o2.pagerank) { return -1; }
				return 1;
			}
		});
		return results;
	}
}

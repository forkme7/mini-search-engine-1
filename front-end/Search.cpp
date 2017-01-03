#include "http_demo_Search.h"
#include "inverted-index.h"
#include <vector>
#include <iostream>
#include <fstream>
#include <string>
#include <sstream>
#include <unordered_map>
using std::string;
using std::vector;

JNIEXPORT void JNICALL Java_http_demo_Search_initData
(JNIEnv *, jobject){
	std::cout << "Load inverted index" << std::endl;
	std::ifstream fin;
	fin.open("inverted_index\\inverted_index.txt");
	if (fin.fail()) {
		std::cout << "Fail to load inverted index!" << std::endl;
		return;
	}
	else {
		std::cout << "Load successful" << std::endl;
	} 
	read_inverted_index(fin);
	std::cout << "Create inverted index...Done!" << std::endl; 
	return;
}

struct Result { //��������Result�ṹ�廹�����������ĳ�Ա�������� 
	int index;	//�ŵ�java�Ĵ�����ȥ�ˣ�����ֻʣ��һ��������ĳ�Ա 
};

int setResults(Result *&results,	// output
		string *keyArr, int len); 

//���´�����JNI�ӿڣ�JAVA��CPP�����Ĳ��֣��е�dirty 
JNIEXPORT jobjectArray JNICALL Java_http_demo_Search_searchResults
(JNIEnv *env, jobject jobj, jobjectArray jstrArr){
	//��java��Stringת��Ϊcpp��string��������Ҫת��ΪGBK 
	jint len = env->GetArrayLength(jstrArr);	
	string *keyArr = new string[len];
	jclass jstr_class = env->FindClass("java/lang/String");
	jmethodID get_bytes = env->GetMethodID(jstr_class, "getBytes", "(Ljava/lang/String;)[B");
	jstring codetype = env->NewStringUTF("GBK");
	for (jint i = 0; i < len; i ++) {
		jstring jstr = (jstring)env->GetObjectArrayElement(jstrArr, i);
		//���������java�ķ�������java��Stringת��ΪGBK���ֽڴ� 
		jbyteArray byteArr = (jbyteArray)env->CallObjectMethod(jstr, get_bytes, codetype);
		char *key = (char *)env->GetByteArrayElements(byteArr, JNI_FALSE);
		int strLen = env->GetArrayLength(byteArr);
		key[strLen] = '\0';
		keyArr[i] = key;
	}
	Result *results;
	//setResult :cpp����������keyArr���м��������������results�� 
	int resLen = setResults(results, keyArr, len);
	//���´���Ĺ���Ϊ��cpp��Result�ṹת��Ϊjava�е�Result�࣬�����ؽ�� 
	jclass jresult = env->FindClass("http/demo/Result");
	jobjectArray jresults = env->NewObjectArray(resLen, jresult, 0);
	jmethodID newID = env->GetMethodID(jresult, "<init>", "(I)V");
	for (int i = 0; i < resLen; i ++) {
		jobject jres = env->NewObject(jresult, newID, results[i].index);
		env->SetObjectArrayElement(jresults, i, jres);
	}
	return jresults;
}

const string AND = "and", OR = "or", NOT = "not";
const int PRI_N = 1, PRI_A = 2, PRI_O = 3;
	// NOT�����߼� 
vector<int> logic_n(vector<int> a, vector<int> b) {
	int ia = 0, ib = 0;
	vector<int> iv;
	while (ia < a.size() && ib < b.size()) {
		if (a[ia] == b[ib]) { ia ++; ib ++; }
		else if (a[ia] < b[ib]) { iv.push_back(a[ia ++]); }
		else { ib ++; }
	}
	while (ia < a.size()) { iv.push_back(a[ia ++]); }
	return iv;
}
	// AND�����߼� 
vector<int> logic_a(vector<int> a, vector<int> b) {
	int ia = 0, ib = 0;
	vector<int> iv;
	while (ia < a.size() && ib < b.size()) {
		if (a[ia] == b[ib]) { iv.push_back(a[ia ++]); ib ++; }
		else if (a[ia] < b[ib]) { ia ++; }
		else { ib ++; }
	}
	return iv;
}
	// OR�����߼� 
vector<int> logic_o(vector<int> a, vector<int> b) {
	int ia = 0, ib = 0;
	vector<int> iv;
	while (ia < a.size() || ib < b.size()) {
		if (ia == a.size()) { iv.push_back(b[ib ++]); }
		else if (ib == b.size()) { iv.push_back(a[ia ++]); }
		else if (a[ia] == b[ib]) { iv.push_back(a[ia ++]); ib ++; }
		else if (a[ia] < b[ib]) { iv.push_back(a[ia ++]); }
		else { iv.push_back(b[ib ++]); }
	}
	return iv;
}
	// ���ݵ���������ȡһ��key��Ӧ���ļ����� 
vector<int> getIndex(string key) {
	vector<int> iv;
	vector<Posting> pv = search_inverted_index(key);
	for (int i = 0; i < pv.size(); i ++) {
		iv.push_back(pv[i].docId);
	}
	return iv;
}
	// �ݹ麯�������ڽ������������ؼ��ʴ������ݹ���� 
vector<int> retrieval(string *keyArr, int len) {
	vector<int> iv;
	if (len <= 0) { return iv; }
	if (len == 1) { return getIndex(keyArr[0]); }
	int level = 0, prio = 99, pos = -1;
	bool paren = false;
	for (int i = 0; i < len; i ++) {
		if (keyArr[i] == "(") { level --; paren = true; }
		else if (keyArr[i] == ")") { level ++; }
		else if (level < 0) { continue; }
		else if (keyArr[i] == NOT && prio > PRI_N) { pos = i; prio = PRI_N; }
		else if (keyArr[i] == AND && prio > PRI_A) { pos = i; prio = PRI_A; }
		else if (keyArr[i] == OR && prio > PRI_O) { pos = i; prio = PRI_O; }
	}
	if (!paren && pos == -1) { return logic_o(getIndex(keyArr[0]), retrieval(keyArr + 1, len - 1)); }
	if (pos == -1) { return retrieval(keyArr + 1, len - 1); }
	switch (prio) {
		case PRI_N: return logic_n(retrieval(keyArr, pos), retrieval(keyArr + pos + 1, len - pos - 1)); break;
		case PRI_A: return logic_a(retrieval(keyArr, pos), retrieval(keyArr + pos + 1, len - pos - 1)); break;
		case PRI_O: return logic_o(retrieval(keyArr, pos), retrieval(keyArr + pos + 1, len - pos - 1)); break;
		default: std::cout << "bad expression!" << std::endl;
	}
	return iv;
}
	// ����Result�ṹ��ֵ��Ϊ���ؽ����׼�� 
int setResults(Result *&results,	// output
		string *keyArr, int len) {
	vector<int> iv = retrieval(keyArr, len);
	results = new Result[iv.size()];
	for (int i = 0; i < iv.size(); i ++) {
		results[i].index = iv[i];
	}
	return iv.size();
}

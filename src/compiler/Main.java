package compiler;

import execute.ExecuteCode;
import lexical.LexicalAnalyzer;
import lexical.Token;
import parsing.Code;
import parsing.Declaration;
import parsing.DeclarationTableParser;
import parsing.Parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
	static String sourcePath = "/src/code/test.pl0"; // 测试代码地址
	static String testPath = "/src/code/code.pl0code"; // 测试目标代码地址

	public static void main(String[] args) throws IOException {
		//获取当前文件路径--到pl0 master
		File directory = new File("");// 参数为空
		String courseFile = directory.getCanonicalPath();
		String sourceCodePath = courseFile + sourcePath;

		//把整个文件直接读进来
		System.out.println("\n==========源代码==========");
		String sourceCode = SourceCodeFileReader.readFileContent(sourceCodePath, "\n");
		System.out.println(sourceCode);

		//分析Token，用的是暴力，不是DFA。
		System.out.println("\n==========词法分析后的Token==========");
		ArrayList<Token> wordsToken = LexicalAnalyzer.getsym(sourceCode);
		for (int a = 0; a < wordsToken.size(); a++) {
			System.out.println(a + " " + wordsToken.get(a).toString());
		}

		//之所以进行这种分析是因为，此语言要求先声明再调用
		//table：const 类型变量和对应值
		//var： 类型变量的层级和地址
		//PROCEDURE：记录起始和结束，同时会帮我们进行语法的分析。
		System.out.println("\n==========声明的引用table表==========");
		ArrayList<Declaration> declarationList = DeclarationTableParser.parse(wordsToken, 0, 3);
		for (Declaration declaration : declarationList) {
			System.out.println(declaration.toString());
		}

		System.out.println("\n==========翻译成的中间代码==========");
//		ArrayList<Code> codeList = importTest(); // 使用测试中间代码
		Parser.init(wordsToken, declarationList);
		ArrayList<Code> codeList = Parser.getCode();
		for (int a = 0; a < codeList.size(); a++) {
			System.out.println(a + " " + codeList.get(a).toString());
		}



		System.out.println("\n==========执行代码==========");
		ExecuteCode.execute(codeList);
	}

	/**
	 * 导入测试中间代码
	 * @return code list
	 */
	public static ArrayList<Code> importTest() throws IOException {
		File directory = new File("");// 参数为空
		String courseFile = directory.getCanonicalPath();
		String testCodePath = courseFile + testPath;
		String testCode = SourceCodeFileReader.readFileContent(testCodePath, "\n");
		String[] testCodeLine = testCode.split("\n");
		ArrayList<Code> codeList = new ArrayList<Code>();
		for (String code : testCodeLine) {
			String[] detail = code.split(" ");
			Code temp = new Code(detail[0], detail[1], detail[2]);
			codeList.add(temp);
		}
		return codeList;
	}
}

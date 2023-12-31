package parsing;

import error.PL0Error;
import lexical.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * 语句分析类，生成目标代码
 *
 * @author ZY
 */
public class Parser {
    private static int tokenListIndex = 0;  // 扫描token表用的指针
    private static ArrayList<Code> code = new ArrayList<Code>();  // 生成的output list，即为作业中的code数组
    private static int filledId = -1;  // 回填用的id
    private static int level = 0;//记录当前的层数
    private static int number = 0;//记录最外层变量的数目

    public static void init(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList) {
        gen("JMP", "0", "main");
        for (Declaration declaration : declarationList) {
            if (declaration.getKind().equals("PROCEDURE")) {
                //帮忙封装code的方法。
                gen("JMP", "0", declaration.getName());
            }
        }
        for (Declaration declaration : declarationList) {
            if (declaration.getKind().equals("PROCEDURE")) {
                level++;
                //这个变量用来后面回填跳转的位置。
                declaration.setCodeStartIndex(code.size());
                //在栈中开辟3个数据单元
                gen("INT", "0", "VAR");
                //传入token,declaration，以及PROCEDURE的token开始行和结束行
                parse(tokenList, declarationList, declaration.getStart(), declaration.getEnd());
                gen("OPR", "0", "0");
                level--;
            }
        }

        filledCodeList("main", code.size() + "");  // 回填main函数指令
        gen("INT", "0", "MAINVAR");
        parse(tokenList, declarationList, declarationList.get(declarationList.size() - 1).getEnd(), tokenList.size());
        gen("OPR", "0", "0");

        // 回填call和JMP中的所有函数地址
        for (Declaration declaration : declarationList) {
            if (declaration.getKind().equals("PROCEDURE")) {
                filledCodeList(declaration.getName(), declaration.getCodeStartIndex() + "");
            }
        }
        //动态开辟变量空间
        List<Integer> list = new ArrayList<>();
        int num = 0;
        for(Declaration declaration:declarationList){
            if(!"PROCEDURE".equals(declaration.getKind())){
                num++;
            }else {
                list.add(num);
                num = 0;
            }
        }
        list.add(num);
        num = 1;
        for(Code code1:code){
            if("VAR".equals(code1.getAddressOffset())){
                code1.setAddressOffset(String.valueOf(3+list.get(num)));
                num++;
            }else if("MAINVAR".equals(code1.getAddressOffset())){
                code1.setAddressOffset(String.valueOf(3+list.get(0)));
            }
        }

    }

    /**
     * 对语句进行翻译，生成code list
     *
     * @param tokenList token列表
     */
    private static void parse(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int startIndex, int endIndex) {
        while (tokenListIndex < endIndex) {
            switch (tokenList.get(tokenListIndex).getSym()) {
                //处理变量 -- 声明和赋值
                case "IDENT":
                    identParser(tokenList, declarationList);
                    break;
                case "BEGINSYM":
                    beginParser(tokenList, declarationList, startIndex);
                    break;
                case "IFSYM":
                    ifParser(tokenList, declarationList, startIndex, endIndex);
                    break;
                case "CALLSYM":
                    callParser(tokenList, declarationList);
                    break;
                case "WHILESYM":
                    whileParser(tokenList, declarationList, startIndex, endIndex);
                    break;
                case "READSYM":
                    readParser(tokenList,declarationList);
                    break;
                case "WRITESYM":
                    writeParser(tokenList,declarationList);
                    break;

            }
            tokenListIndex++;
        }
    }

    /**
     * 对read进行处理，生成对应语句
     */
    private static void readParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList){
        Token left = getNext(tokenList);
        Token var = getNext(tokenList);
        Token right = getNext(tokenList);
        Token end = getNext(tokenList);
        if(left.getSym().equals("SYM_(")&&right.getSym().equals("SYM_)")){
            if(end.getSym().equals("SYM_;")){
                int index = getItemIndexInDeclarationList(var, declarationList);
                Declaration declaration = declarationList.get(index);
                gen("OPR","0","13");
                gen("STO",declaration.getLevel(),declaration.getAdr());

            }else {
                PL0Error.log(16);
            }
        }else {
            PL0Error.log(19);
        }
    }
    /**
     * 对write进行处理，生成对应语句
     */
    private static void writeParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList){
        Token left = getNext(tokenList);
        Token var = getNext(tokenList);
        Token right = getNext(tokenList);
        Token end = getNext(tokenList);
        if(left.getSym().equals("SYM_(")&&right.getSym().equals("SYM_)")){
            if(end.getSym().equals("SYM_;")){
                int index = getItemIndexInDeclarationList(var, declarationList);
                Declaration declaration = declarationList.get(index);
                gen("LOD",declaration.getLevel(),declaration.getAdr());
                gen("OPR","0","14");
            }else {
                PL0Error.log(16);
            }
        }else {
            PL0Error.log(17);
        }
    }

    /**
     * 翻译ident，例如 x := x + y 或 x := 20
     * 此方法用于处理两种情况，变量声明和变量赋值。
     * @param tokenList       token列表
     * @param declarationList 声明table
     *
     *
     */
    private static void identParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList) {
        Token left = tokenList.get(tokenListIndex);
        int leftIndex = getItemIndexInDeclarationList(left, declarationList);
        if (left != null && leftIndex != -1) {
            Token equals = getNext(tokenList);
            if (equals != null && equals.getSym().equals("SYM_:=")) {
                expressionParser(tokenList, declarationList, declarationList.get(leftIndex));
            }
            else if("SYM_++".equals(equals.getSym())||"SYM_--".equals(equals.getSym())){
                //处理自增和自减少（LOD 处理 STO）
                Token end = getNext(tokenList);
                Declaration declaration = declarationList.get(leftIndex);
                if("SYM_;".equals(end.getSym())){
                    gen("LOD",declaration.getLevel(),declaration.getAdr());
                    if("SYM_++".equals(equals.getSym()))
                        gen("OPR","0","15");
                    else
                        gen("OPR","0","16");
                    gen("STO",declaration.getLevel(),declaration.getAdr());
                }else {
                    PL0Error.log(16);
                }
            }
            else if("SYM_+=".equals(equals.getSym())||"SYM_-=".equals(equals.getSym())||"SYM_*=".equals(equals.getSym())||"SYM_/=".equals(equals.getSym())){
                expressionParser2(tokenList,declarationList,declarationList.get(leftIndex));
            }
            else if (equals != null && !(equals.getSym().equals("SYM_,") || equals.getSym().equals("SYM_;"))) {
                PL0Error.log(8);
            }
        } else {
            PL0Error.log(9);
        }
    }

    /**
     * 处理赋值时 := 的右半部分，即处理表达式（这里针对完整的加减 a=b+c这种）
     * TODO：可简化代码
     *
     * @param tokenList       token列表
     * @param declarationList 变量声明表
     * @param left            := 的左半部分Token对应的Declaration
     */
    private static void expressionParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, Declaration left) {
        Token first = getNext(tokenList);
        int firstIndex = getItemIndexInDeclarationList(first, declarationList);
        if (first != null && firstIndex != -1 && first.getSym().equals("IDENT")) {
            Token next = getNext(tokenList);
            if (next != null && next.getSym().equals("SYM_+")) {
                // 如 x := x + y;
                generateOperatorCode(tokenList, declarationList, left, firstIndex, "2");
            } else if (next != null && next.getSym().equals("SYM_-")) {
                // 如 x := x - y
                generateOperatorCode(tokenList, declarationList, left, firstIndex, "3");
            } else if (next != null && next.getSym().equals("SYM_*")) {
                // 如 x := x * y
                generateOperatorCode(tokenList, declarationList, left, firstIndex, "4");
            } else if (next != null && next.getSym().equals("SYM_/")) {
                // 如 x := x / y
                generateOperatorCode(tokenList, declarationList, left, firstIndex, "5");
            } else if(next != null && next.getSym().equals("SYM_%")){
                // 如 x := x%y
                generateOperatorCode(tokenList, declarationList, left, firstIndex, "17");
            } else if (next != null && next.getSym().equals("SYM_;")) {
                // 如 x := y;
                gen("LOD", declarationList.get(firstIndex).getLevel(), declarationList.get(firstIndex).getAdr());
                gen("STO", left.getLevel(), left.getAdr());
            } else {
                PL0Error.log(10);
            }
            //对应赋值表达式
        } else if (first != null && first.getSym().equals("NUMBER")) {
            // 如 x := 20;
            gen("LIT", "0", first.getNum());
            gen("STO", left.getLevel(), left.getAdr());
        } else {
            PL0Error.log(9);
        }
    }

    /**
     * 处理赋值时 := 的右半部分，即处理表达式（这里针对*=,+=这种）
     * TODO：可简化代码
     *
     * @param tokenList       token列表
     * @param declarationList 变量声明表
     * @param left            := 的左半部分Token对应的Declaration
     */
    private static void expressionParser2(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, Declaration left) {
        Token operator = tokenList.get(tokenListIndex);
        Token first = getNext(tokenList);
        int firstIndex = getItemIndexInDeclarationList(first, declarationList);
        if (first != null && firstIndex != -1 && first.getSym().equals("IDENT")) {
            if ("SYM_+=".equals(operator.getSym())) {
                generateOperatorCode2(tokenList, declarationList, left, firstIndex, "2");
            } else if ("SYM_-=".equals(operator.getSym())) {
                generateOperatorCode2(tokenList, declarationList, left, firstIndex, "3");
            } else if ("SYM_*=".equals(operator.getSym())) {
                generateOperatorCode2(tokenList, declarationList, left, firstIndex, "4");
            } else if ("SYM_/=".equals(operator.getSym())) {
                generateOperatorCode2(tokenList, declarationList, left, firstIndex, "5");
            } else {
                PL0Error.log(10);
            }
        } else {
            PL0Error.log(9);
        }
    }

    /**
     * 翻译call
     *
     * @param tokenList       token列表
     * @param declarationList 声明列表
     */
    private static void callParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList) {
        Token ident = getNext(tokenList);
        int identIndex = getItemIndexInDeclarationList(ident, declarationList);
        if (ident != null && ident.getSym().equals("IDENT")) {
            Token end = getNext(tokenList);
            if (end != null && end.getSym().equals("SYM_;")) {
                if (identIndex != -1) {
                    gen("CAL", declarationList.get(identIndex).getLevel(), declarationList.get(identIndex).getName());
                }
            } else {
                PL0Error.log(16);
            }
        } else {
            PL0Error.log(15);
        }
    }

    /**
     * 本方法用来处理begin
     *
     * @param tokenList token裂变
     * @param declarationList 声明列表
     * @param startIndex 本次PROCEDURE的开始行数
     */
    private static void beginParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int startIndex) {
        //这里要去找endIndex，注意不是直接找end，此PROCEDURE中可能还有一个begin、end
        int endIndex = findEnd(tokenList, tokenListIndex);

        while (tokenListIndex < endIndex) {
            Token next = getNext(tokenList);
            //结束之后加入结束符
            if (next != null && next.getSym().equals("ENDSYM")) {
                Token end = getNext(tokenList);
                if (end != null && (end.getSym().equals("SYM_.") || end.getSym().equals("SYM_;"))) {
                    gen("OPR", "0", "0");
                    return;
                } else {
                    PL0Error.log(14);
                }
            } else {
                //递归调用方法来继续读取token
                parse(tokenList, declarationList, startIndex, endIndex);
            }
        }
    }

    /**
     * 翻译if
     *
     * @param tokenList       token列表
     * @param declarationList 声明列表
     */
    private static void ifParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int startIndex, int endIndex) {
        Token left = getNext(tokenList);
        Token operator = getNext(tokenList);
        Token right = getNext(tokenList);
        Token then = getNext(tokenList);
        if (left != null && operator != null && right != null && then != null && then.getSym().equals("THENSYM")) {
            booleanParser(left, right, operator, declarationList);

            Token next = getNext(tokenList);
            if (next != null && next.getSym().equals("BEGINSYM")) {
                int tempKey = --filledId;
                gen("JPC", "0", tempKey + "");
                beginParser(tokenList, declarationList, startIndex);
                filledCodeList(tempKey + "", code.size() + "");
            } else {
                PL0Error.log(11);
            }
        } else {
            PL0Error.log(11);
        }
    }

    /**
     * 翻译while
     *
     * @param tokenList       token列表
     * @param declarationList 声明列表
     */
    private static void whileParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int startIndex, int endIndex) {
        Token left = getNext(tokenList);
        Token operator = getNext(tokenList);
        Token right = getNext(tokenList);
        Token doIdent = getNext(tokenList);
        if (left != null && operator != null && right != null && doIdent != null && doIdent.getSym().equals("DOSYM")) {
            int loopStartPosition = code.size();
            booleanParser(left, right, operator, declarationList);

            Token next = getNext(tokenList);
            if (next != null && next.getSym().equals("BEGINSYM")) {
                int tempKey = --filledId;
                gen("JPC", "0", filledId + "");
                beginParser(tokenList, declarationList, startIndex);
                gen("JMP", "0", loopStartPosition + "");
                filledCodeList(tempKey + "", code.size() + "");
            } else {
                PL0Error.log(13);
            }
        } else {
            PL0Error.log(13);
        }
    }

    /**
     * 翻译布尔表达式，如 x < y
     *
     * @param left            左面的元素
     * @param right           右面的元素
     * @param operator        操作符
     * @param declarationList 声明列表
     */
    private static void booleanParser(Token left, Token right, Token operator, ArrayList<Declaration> declarationList) {
        booleanItemParser(left, declarationList);
        booleanItemParser(right, declarationList);
        switch (operator.getSym()) {
            case "SYM_==":
                gen("OPR", "0", "7");
                break;
            case "SYM_<>":
                gen("OPR", "0", "8");
                break;
            case "SYM_<":
                gen("OPR", "0", "9");
                break;
            case "SYM_<=":
                gen("OPR", "0", "10");
                break;
            case "SYM_>":
                gen("OPR", "0", "11");
                break;
            case "SYM_>=":
                gen("OPR", "0", "12");
                break;
        }
    }

    /**
     * 翻译布尔表达式中的某一项
     *
     * @param token           某一个token
     * @param declarationList 声明列表
     */
    private static void booleanItemParser(Token token, ArrayList<Declaration> declarationList) {
        if (token.getSym().equals("IDENT")) {
            int index = getItemIndexInDeclarationList(token, declarationList);
            if (index != -1) {
                gen("LOD", declarationList.get(index).getLevel(), declarationList.get(index).getAdr());
            } else {
                PL0Error.log(9);
            }
        } else if (token.getSym().equals("NUMBER")) {
            gen("LIT", "0", token.getNum());
        } else {
            PL0Error.log(12);
        }
    }

    /**
     * 根据操作符生成目标代码
     *
     * @param tokenList       token列表
     * @param declarationList 声明列表
     * @param left            操作符左面的声明
     * @param firstIndex      :=左面的声明
     * @param offset          操作符的偏移量
     */
    private static void generateOperatorCode(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, Declaration left, int firstIndex, String offset) {
        Token second = getNext(tokenList);
        int secondIndex = getItemIndexInDeclarationList(second, declarationList);
        Token end = getNext(tokenList);
        if (second != null && end != null && end.getSym().equals("SYM_;")) {
            gen("LOD", declarationList.get(firstIndex).getLevel(), declarationList.get(firstIndex).getAdr());
            gen("LOD", declarationList.get(secondIndex).getLevel(), declarationList.get(secondIndex).getAdr());
            gen("OPR", "0", offset);
            gen("STO", left.getLevel(), left.getAdr());
        }
    }

    /**
     * 处理 *=,/*等值
     * @param tokenList
     * @param declarationList
     * @param left
     * @param firstIndex
     * @param offset
     */
    private static void generateOperatorCode2(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, Declaration left, int firstIndex, String offset) {
        Token end = getNext(tokenList);
        if (end != null && end.getSym().equals("SYM_;")) {
            gen("LOD", left.getLevel(), left.getAdr());
            gen("LOD", declarationList.get(firstIndex).getLevel(), declarationList.get(firstIndex).getAdr());
            gen("OPR", "0", offset);
            gen("STO", left.getLevel(), left.getAdr());
        }
    }
    /**
     * 回填数组
     *
     * @param key   占位数值
     * @param value 回填的数值
     */
    private static void filledCodeList(String key, String value) {
        for (Code item : code) {
            if (item.getAddressOffset().equals(key)) {
                item.setAddressOffset(value);
            }
        }
    }

    /**
     * 获取下一个token
     *
     * @param tokenList token列表
     * @return token
     */
    private static Token getNext(ArrayList<Token> tokenList) {
        if (tokenListIndex < tokenList.size() - 1) {
            tokenListIndex++;
            return tokenList.get(tokenListIndex);
        } else {
            return null;
        }
    }

    /**
     * 判断某个token是否已经在declaration list中声明
     *
     * @param token           token
     * @param declarationList 声明列表
     * @return 如果有，返回index，若没有，返回-1
     */
    private static int getItemIndexInDeclarationList(Token token, ArrayList<Declaration> declarationList) {
        for (int a = 0; a < declarationList.size(); a++) {
            if (token.getId().equals(declarationList.get(a).getName())) {
                return a;
            }
        }
        return -1;
    }

    /**
     * 将翻译好的功能码加入code数组中
     *
     * @param function        功能码
     * @param levelDifference 层次差
     * @param addressOffset   位移量
     */
    private static void gen(String function, String levelDifference, String addressOffset) {
        if (levelDifference.equals("")) {
            levelDifference = "0";
        }
        if (addressOffset.equals("")) {
            addressOffset = "0";
        }
        //更改错误使用层级信息的问题
        if(function.equals("STO")||function.equals("LOD")){
            levelDifference = String.valueOf(level - Integer.parseInt(levelDifference));
        }
        Code addCode = new Code(function,levelDifference,addressOffset);
        code.add(addCode);
    }

    /**
     * 匹配与当前begin对应的end
     *
     * @param tokenArrayList token列表
     * @param index          begin的位置
     * @return 对应的end的位置
     */
    private static int findEnd(ArrayList<Token> tokenArrayList, int index) {
        int begin = 1;
        for (int a = index + 1; a < tokenArrayList.size(); a++) {
            if (tokenArrayList.get(a).getSym().equals("BEGINSYM")) {
                begin++;
            } else if (tokenArrayList.get(a).getSym().equals("ENDSYM")) {
                begin--;
                if (begin == 0) {
                    return a;
                }
            }
        }
        return 0;
    }

    public static ArrayList<Code> getCode() {
        return code;
    }
}

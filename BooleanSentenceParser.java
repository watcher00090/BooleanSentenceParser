import java.lang.Integer;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

// Token Types.
enum Tok {
    LPAR,       // '('
    RPAR,       // ')'
    TRUE,       // 'T'
    FALSE,      // 'F'
    AND,        // '&'
    OR,         // '|'
    NOT,        // '~'
    VARIABLE,   // [a-z]
    EOS,        // 0
    INVALID,    // unrecognized character
    UNDEF;      // value of pushed back token prior to any pushback
}

class Tokenizer {

    String in;
    char[] chars;   // buffered input characters
    int i;          // index of current character
    char name;      // side effect: variable name
    Tok backToken;  // pushback token

    Tokenizer(String in) {
        this.in  = in;
        name = 0;
        i = 0;
        backToken = Tok.UNDEF;
        chars = new char[in.length()+1];
        for (int k=0; k<in.length(); k++){
            chars[k]=in.charAt(k);
        }
        chars[in.length()] = 0; // end with the null char
    }

    public Tok nextToken() {
        Tok t = nextTokenContinued();
        //System.out.println(t);
        return t;
    }

    public Tok nextTokenContinued() {

        if (backToken != Tok.UNDEF) {
            Tok result = backToken;
            backToken = Tok.UNDEF;
            return result;
        }

        name = 0;

        while (true) {
            char c = chars[i++];
            if (Character.isWhitespace(c)) continue;
            if (Character.isAlphabetic(c)) {
                if (c == 'T') return Tok.TRUE;
                else if (c == 'F') return Tok.FALSE;
                else {
                    name = c;
                    return Tok.VARIABLE;
                }
            }
            if (c == '&') return Tok.AND;
            if (c == '|') return Tok.OR;
            if (c == '~') return Tok.NOT;
            if (c == '(') return Tok.LPAR;
            if (c == ')') return Tok.RPAR;
            if (c == 0)   return Tok.EOS;
            return Tok.INVALID;
        }
    }
}

abstract class Node {
    Node left;
    Node right;

    public abstract boolean eval(HashMap<Character, Boolean> argList) throws Exception;

    public abstract void print(int depth);

    public void print() {
        print(0);
        System.out.println();
    }

    public static void printSpaces(int depth) {
        for (int i=0; i<depth; i++) System.out.print(" ");
    }

    public boolean eval(HashMap<Character, Boolean> argList, ArrayList<Character> vars, boolean... args) throws Exception {
        if (args.length != argList.size()) throw new Exception("ERROR: INVALID NUMBER OF ARGUMENTS");
        for (int i=0; i<vars.size(); i++) {
            argList.put(vars.get(i), args[i]);
        }
        return eval(argList);
    }

}

class OrNode extends Node {

    public OrNode(Node left, Node right) {
        this.left = left;
        this.right = right;
    }

    public boolean eval(HashMap<Character, Boolean> argList) throws Exception {
        boolean x = left.eval(argList);
        boolean y = right.eval(argList);
        if (x == true || y == true) return true;
        return false;
    }

    public void print(int depth) {
        Node.printSpaces(depth);
        System.out.println("|");
        left.print(depth+1);
        System.out.println();
        right.print(depth+1);
    }

    public String toString() {
        return "(" + left.toString() + " | " + right.toString() + ")";
    }

}

class AndNode extends Node {

    public AndNode(Node left, Node right) {
        this.left = left;
        this.right = right;
    }

    public boolean eval(HashMap<Character, Boolean> argList) throws Exception {
        boolean x = left.eval(argList);
        boolean y = right.eval(argList);
        if (x == true && y == true) return true;
        return false;
    }

    public void print(int depth) {
        Node.printSpaces(depth);
        System.out.println("&");
        left.print(depth+1);
        System.out.println();
        right.print(depth+1);
    }

    public String toString() {
        return "(" + left.toString() + " & " + right.toString() + ")";
    }

}
class NotNode extends Node {
    Node argExpr;

    public NotNode(Node argExpr) {
        this.argExpr = argExpr;
        left = null;
        right = null;
    }

    public boolean eval(HashMap<Character, Boolean> argList) throws Exception {
        return !argExpr.eval(argList);
    }

    public void print(int depth) {
        Node.printSpaces(depth);
        System.out.println("~");
        argExpr.print(depth+1);
    }

    public String toString() { return "~(" + argExpr.toString() + ")"; }
}

class VarNode extends Node {
    char name;

    VarNode(char name) {
        this.name = name;
        left = null;
        right = null;
    }

    public boolean eval(HashMap<Character, Boolean> argList) throws Exception {
        if (!argList.containsKey(name)) throw new Exception("ERROR: MISSED VARIABLE");
        return argList.get(name);
    }

    public void print(int depth) {
        Node.printSpaces(depth);
        System.out.print(name);
    }

    public String toString() { return Character.toString(name); }
}

class ConstNode extends Node {
    boolean status;

    ConstNode(boolean status) {
        this.status = status;
        this.left = null;
        this.right = null;
    }

    public boolean eval(HashMap<Character, Boolean> argList) throws Exception {
        return status;
    }

    public void print(int depth) {
        Node.printSpaces(depth);
        System.out.print(status ? "T" : "F");
    }

    public String toString() { return (status ? "T" : "F"); };
}

class BooleanSentenceGenerator {

    Node sent() {
        Node result = clause();
        while (Math.random() < 0.33) {
            result = new OrNode(result, sent());
        }
        return result;
    }

    Node clause() {
        Node result = atom();
        while (Math.random() < 0.33) result = new AndNode(result, clause());
        return result;
    }

    Node atom() {
        double r = Math.random();
        if (r < 0.6) return term();
        else return new NotNode(term());
    }

    Node term() {
        double r = Math.random();
        if (r < 0.7) return new VarNode( var() );
        if (r < 0.8) return new ConstNode(true);
        if (r < 0.9) return new ConstNode(false);
        else return sent();
    }

    char var() {
        double r = Math.random();
        if (r < 0.10) return 'p';
        if (r < 0.20) return 'q';
        if (r < 0.30) return 'r';
        if (r < 0.40) return 's';
        if (r < 0.50) return 'u';
        if (r < 0.60) return 'v';
        if (r < 0.70) return 'w';
        if (r < 0.80) return 'x';
        if (r < 0.90) return 'y';
        return 'z';
    }

}

/*
The Grammar

    sent ::=
      |  clause
      |  clause '|' sent

    clause ::=
      |  atom
      |  atom '&' clause

    atom ::=
      |  term
      |  '~' term

    term ::=
      |   VAR
      |  '(' sent ')'
      |  'true'             # these could go into 'atom'
      |  'false'            #

*/

public class BooleanSentenceParser {

    String input;
    Tokenizer str;
    Node root;
    HashMap<Character, Boolean> argList;
    ArrayList<Character> vars;

    public BooleanSentenceParser(Tokenizer str) {
        this.input = str.in;
        this.str = str;
        this.vars = new ArrayList<Character>();
        this.argList = new HashMap<Character, Boolean>();
        try {
            root = sent();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        Collections.sort(vars);
    }

    public Node sent() throws Exception {
        Node left = clause();
        Tok t = str.nextToken();
        if (t == Tok.OR) return new OrNode(left, sent());
        else if (t == Tok.RPAR || t == Tok.EOS)  {
            pushBack("sent", t);
            return left;
        }
        else throw new Exception("ERROR: INVALID SYNTAX");
    }

    public Node clause() throws Exception {
        Node left = atom();
        Tok t = str.nextToken();
        if (t == Tok.AND) return new AndNode(left, clause());
        else {
            pushBack("clause", t);
            return left;
        }
    }

    public Node atom() throws Exception {
        Tok t = str.nextToken();
        if (t == Tok.NOT) return new NotNode( term() );
        else {
            pushBack("atom", t);
            return term();
        }
    }

    public Node term() throws Exception {
        Tok t = str.nextToken();
        if (t == Tok.VARIABLE) {
            if (!vars.contains(str.name)) vars.add(str.name);
            argList.put(str.name, null);
            return new VarNode(str.name);
        }
        else if (t == Tok.LPAR) {
            Node interior = sent();
            Tok t2 = str.nextToken();
            if (t2 != Tok.RPAR) throw new Exception("ERROR: EXPECTING RPAR");
            else return interior;
        }
        else if (t == Tok.TRUE) return new ConstNode(true);
        else if (t == Tok.FALSE) return new ConstNode(false);
        else if (t == Tok.EOS) { //end of String
            pushBack("term", t);
            return null;
        }
        else if (t == Tok.INVALID) throw new Exception("ERROR: INVALID TOKEN");
        else if (t == Tok.UNDEF) throw new Exception("ERROR: NOT EXPECTING UNDEF");
        else throw new Exception("INVALID SYNTAX");
    }

    public void pushBack(Tok token) {
        System.out.println("            PUSHBACK_REQUEST: " + token);
        str.backToken = token;
    }

    public void pushBack(String s, Tok token) {
        System.out.println("            PUSHBACK_REQUEST: " + s + ", " +  token);
        str.backToken = token;
    }

    public static void testParser(String[] args) {
        Tokenizer T = new Tokenizer(args[0]);
        BooleanSentenceParser P = new BooleanSentenceParser(T);
        P.root.print();
    }

    public static void testTokenizer(String[] args) {
        for (int i=0; i<args.length; i++) {
            Tokenizer T = new Tokenizer(args[i]);
            while (true) {
                Tok t = T.nextToken();
                System.out.print(t);
                if (t == Tok.VARIABLE) System.out.print(", " + T.name);
                if (t == Tok.EOS) {
                    System.out.println();
                    break;
                }
                System.out.println();
            }
        }
    }

    public static void testPrint(String[] args) {
        Tokenizer T = new Tokenizer(args[0]);
        BooleanSentenceParser P = new BooleanSentenceParser(T);
        Node f = P.root;
        f.print(0);
    }

    public static void testToString(String[] args) {
        Tokenizer T = new Tokenizer(args[0]);
        BooleanSentenceParser P = new BooleanSentenceParser(T);
        P.root.toString();
    }

    public static void testSort(String[] args) {
        ArrayList<Character> vars = new ArrayList<Character>();
        char[] chars = new char[args[0].length()];
        args[0].getChars(0, args[0].length(), chars, 0);
        for (char c : chars) vars.add(c);
        print(vars);
        System.out.println();
        Collections.sort(vars);
        print(vars);
        System.out.println();
    }

    public static void print(ArrayList<Character> vars) {
        for (char c : vars) System.out.print(c + " ");
        System.out.println();
    }

    public static void testSAT(String[] args) throws Exception {
        Tokenizer T = new Tokenizer(args[0]);
        BooleanSentenceParser P = new BooleanSentenceParser(T);
        Node sentence = P.root;
        ArrayList<Character> vars = P.vars;
        HashMap<Character, Boolean> argList = P.argList;
        int n = vars.size();

        sentence.print();
        System.out.println();
        print(vars);
        System.out.println();

        // enumerate all possible n-bit patterns
        // that's the same as counting from 0 to 2^n-1
        boolean satisfied = false;
        if (n == 0) {
            if (sentence.eval(argList)) System.out.println("true");
            else System.out.println("false");
        }
        else {
            for (int i = 0; i < (1<<n); i++) {
                // i is the bit pattern

                // assign the bits as truth values to the variables
                for (int j=0; j<n; j++) {
                    argList.put(vars.get(j), ( (i >> j) & 1) == 1 ? true : false);
                }
                if (sentence.eval(argList)) {
                    satisfied = true;
                    for (int l=0; l<n; l++) {
                        System.out.println(vars.get(l) + " : " + (( (i >> l) & 1 )== 1 ? true : false) );
                    }
                    System.out.println();
                }
            }
            if (!satisfied) System.out.println("no luck :(");
        }
    }

    public static void testEval(String[] args) throws Exception {
        Tokenizer T = new Tokenizer(args[0]);
        BooleanSentenceParser P = new BooleanSentenceParser(T);
        Node sentence = P.root;
        ArrayList<Character> vars = P.vars;
        HashMap<Character, Boolean> argList = P.argList;

        sentence.print();
        System.out.println();
        print(vars);

        for (int i = 1; i < args.length; i++) {
            boolean b = Integer.parseInt(args[i]) == 1 ? true : false;
            argList.put(vars.get(i-1), b);
        }
        System.out.println(sentence.eval(argList));
    }

    public static void randomTestParser(String[] args) {
        try {
            BooleanSentenceGenerator G = new BooleanSentenceGenerator();
            Node sent = G.sent();
            String s = sent.toString();
            System.out.println("input = \n" + sent);
            Tokenizer T = new Tokenizer(s);
            BooleanSentenceParser P = new BooleanSentenceParser(T);
            System.out.println(P.root.toString());
        }
        catch(Exception e) {
             e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            //randomTestParser(args);
            testSAT(args);
            //testEval(args);
            //testParser(args);
            //testSort(args);
            //testTokenizer(args);
            //testToString(args);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}

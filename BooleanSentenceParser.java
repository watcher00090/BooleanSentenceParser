import java.lang.Integer;
import java.util.ArrayList;

//Token Types. UNDEF = pushback token when nothing's been pushed back, token returned when state is unknown.
enum Tok { 
    LPAR, RPAR, TRUE, FALSE, AND, OR, NOT, VARIABLE, EOS, INVALID, UNDEF; 
}

class Tokenizer { 

    String in;
    char[] chars; 
    int i;
    char name; //stores function or variable names
    Tok backToken;

    Tokenizer(String in) { 
        this.in  = in;
        name = 0;
        i = 0;
        backToken = Tok.UNDEF;
        chars = new char[in.length()+1];
        for (int k=0; k<in.length(); k++){
            chars[k]=in.charAt(k);    
        }
        chars[in.length()] = 0; //end with the null byte character, so the Tokenizer knows when the String is done.
    }

    public Tok nextToken() { 
        Tok t = nextTokenContinued();
        //System.out.println(t);
        return t;
    }

    public Tok nextTokenContinued() { 
        name = 0;
        if (backToken !=Tok.UNDEF) {    
            Tok result = backToken;      
            backToken = Tok.UNDEF;    
            return result;             
        }

        while (true) { 
            char c = chars[i++]; 
            if (Character.isWhitespace(c)) continue;
            if (Character.isAlphabetic(c)) {
                if (c == 't') return Tok.TRUE;
                else if (c == 'f') return Tok.FALSE;
                else { 
                    name = c;
                    return Tok.VARIABLE; 
                }
            }
            else if (c == '^') return Tok.AND;
            else if (c == '|') return Tok.OR;
            else if (c == '~') return Tok.NOT;
            else if (c == '(') return Tok.LPAR;
            else if (c == ')') return Tok.RPAR;
            else if (c == 0) return Tok.EOS;
            else return Tok.INVALID;
        }
    }
}

abstract class Sentence {
    Sentence left; 
    Sentence right;

    public abstract boolean eval(ArrayList<Character> vars, ArrayList<Boolean> vals) throws Exception; 

    void print() { System.out.println(this.toString()); }

}

class Disjunction extends Sentence {

    public Disjunction(Sentence left, Sentence right) {
        this.left = left;
        this.right = right;
    }

    public boolean eval(ArrayList<Character> vars, ArrayList<Boolean> vals) throws Exception {         
        boolean x = left.eval(vars, vals);
        boolean y = right.eval(vars, vals);
        if (x == true || y == true) return true;
        else return false;
    }

}

class Conjunction extends Sentence { 

    public Conjunction(Sentence left, Sentence right) {
        this.left = left;
        this.right = right;
    }

    public String toString() { 
        return left.toString() + "^ (" + right.toString() + ")";
    }

    public boolean eval(ArrayList<Character> vars, ArrayList<Boolean> vals) throws Exception {         
        boolean x = left.eval(vars, vals);
        boolean y = right.eval(vars, vals);
        if (x == true && y == true) return true;
        else return false;
    }
    
}

class Not extends Sentence {
    Sentence argExpr;

    public Not(Sentence argExpr) {
        left = null;
        right = null;
        this.argExpr = argExpr;
    }

    public boolean eval(ArrayList<Character> vars, ArrayList<Boolean> vals) throws Exception {
        return !argExpr.eval(vars, vals);
    }

}

class Variable extends Sentence { 
    char name;

    public String toString() { return Character.toString(name); }
    
    public boolean eval(ArrayList<Character> vars, ArrayList<Boolean> vals) throws Exception {
        return vals.get(vars.indexOf(name));
    }
    
    Variable(char name) { 
        this.name = name; 
        left = null;
        right = null; 
    }

}

class Constant extends Sentence {
    boolean status;

    Constant(boolean status) {
        this.status = status;
    }

    public boolean eval(ArrayList<Character> vars, ArrayList<Boolean> vals) throws Exception {
        return status;
    }

}

/*
The Grammar

CFG: 
    sent ::= 
         |  sent '^' sent
         |  sent '|' sent  
         |  '(' sent ')'
         |  '~' sent
         |  const 

    const ::=
         |  true
         |  false
         |  [a-z]{1}  

precedence:
    ~ 
    ^ 
    |

Right-associative PEG:
//lowest precedence first
    A ::= 
         B   
      |  B | A
    
    B ::=
         C
      |  C ^ B 
    
    C ::=
         D
      |  '~' D

    D ::=
      |  '(' A ')'
      | true
      |  false
      |  [a-z]{1}

*/

public class BooleanSentenceParser { 
   
    public Sentence A() throws Exception { 
        Sentence B = B(); 
        Tok t = str.nextToken();
        if (t == Tok.OR) return new Disjunction(B, A());    
        else  { 
            pushBack("A", t);
            return B;
        }
    }

    public Sentence B() throws Exception { 
        Sentence C = C();
        Tok t = str.nextToken();   
        if (t == Tok.AND) return new Conjunction(C, B()); 
        else {
            pushBack("B", t);
            return C;
        }
    }

    public Sentence C() throws Exception { 
        Tok t = str.nextToken();   
        if (t == Tok.NOT) return new Not( C() ); 
        else {
            pushBack("C", t);
            return D();
        }
    }

    public Sentence D() throws Exception { 
        Tok t = str.nextToken();   
        if (t == Tok.VARIABLE) {
            vars.add(str.name);
            return new Variable(str.name); 
        }
        else if (t == Tok.LPAR) {
            Sentence interior = A();
            Tok t2 = str.nextToken();
            if (t2 != Tok.RPAR) throw new Exception("ERROR: EXPECTING RPAR");
            else return interior;
        }
        else if (t == Tok.TRUE) return new Constant(true); 
        else if (t == Tok.FALSE) return new Constant(false);
        else if (t == Tok.EOS) { //end of String
            pushBack("D", t);    
            return null;
        }
        else if (t == Tok.INVALID) throw new Exception("ERROR: INVALID TOKEN");
        else if (t == Tok.UNDEF) throw new Exception("ERROR: NOT EXPECTING UNDEF");
        else throw new Exception("UNREGONIZED ERROR");
    }

    public void pushBack(Tok token) {
        System.out.println("            PUSHBACK_REQUEST: " + token);
        str.backToken = token;
    }

    public void pushBack(String s, Tok token) {
        System.out.println("            PUSHBACK_REQUEST: " + s + ", " +  token);
        str.backToken = token;
    }

    String input;
    Tokenizer str;
    Sentence root;
    ArrayList<Character> vars;
    ArrayList<Boolean> vals; 

    public BooleanSentenceParser(Tokenizer str) { 
        this.input = str.in;
        this.str = str;
        this.vars = new ArrayList<Character>();
        this.vals = new ArrayList<Boolean>();
        try { 
            root = A();
        }
        catch(Exception e) { 
            e.printStackTrace();
        }
    }

    public static String generateFuzzTestString(int length) { 
        char[] chars = {
            'x', '1', '+', '-', '*', '/', '(', ')', '^'
        };    
        String str = "";    
        for (int i=0; i<length; i++) { 
            int pos = (int) (chars.length * Math.random());
            str += chars[pos];
        }
        return str;
    }

    public static void fuzzTestParser(String[] args) {
        if (args.length < 2) {
            System.out.println("ERROR: expecting 2 arguments");
            return;
        }            
        int num = Integer.parseInt(args[0]);
        int len = Integer.parseInt(args[1]); 
        for (int i=0; i<num; i++) { 
            String s = "";
            try { 
                s = generateFuzzTestString(len);
                System.out.println(s);
                Tokenizer T = new Tokenizer(s);
                BooleanSentenceParser P = new BooleanSentenceParser(T);
                System.out.println(P.root.toString()); 
            }       
            catch(Exception e) { 
                e.printStackTrace();
            }
        }
    }

    public static void testParser(String[] args) {
        Tokenizer T = new Tokenizer(args[0]);
        BooleanSentenceParser P = new BooleanSentenceParser(T);
        System.out.println(P.root.toString());
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
        Sentence f = P.root;
        f.print();
    }

    public static void main(String[] args) { 
        //testParser(args);
        testTokenizer(args);
        //testFunction(args);
        //testOpCompareTo(args);
        //testToString(args);
        //testDeriv(args);
        //testNewton(args);
    }

}


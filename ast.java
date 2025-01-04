import java.io.*;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.ArrayList;;
// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a "Simple" program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a sequence (for nodes that may have a variable number of children)
// or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         IdNode, ClassBodyNode
//     ClassBodyNode       DeclListNode
//     DeclListNode        sequence of DeclNode
//     FormalsListNode     sequence of FormalDeclNode
//     MethodBodyNode      DeclListNode, StmtListNode
//     StmtListNode        sequence of StmtNode
//     ExpListNode         sequence of ExpNode
//     SwitchGroupListNode sequence of SwitchGroupNode
//
//     DeclNode:
//       FieldDeclNode     TypeNode, IdNode
//       VarDeclNode       TypeNode, IdNode
//       MethodDeclNode    IdNode, FormalsListNode, MethodBodyNode
//       MethodDeclNodeInt IdNode, FormalsListNode, MethodBodyNode
//       FormalDeclNode    TypeNode, IdNode
//
//     TypeNode:
//       IntNode             -- none --
//       BooleanNode         -- none --
//       StringNode          -- none --
//
//     StmtNode:
//       PrintStmtNode       ExpNode
//       AssignStmtNode      IdNode, ExpNode
//       IfStmtNode          ExpNode, StmtListNode
//       IfElseStmtNode      ExpNode, StmtListNode, StmtListNode
//       WhileStmtNode       ExpNode, StmtListNode
//       CallStmtNode        IdNode, ExpListNode
//       ReturnStmtNode      -- none --
//       ReturnWithValueNode ExpNode
//
//       BlockStmtNode       DeclListNode, StmtListNode
//       SwitchStmtNode      ExpNode, SwitchGroupListNode
//      
//     SwitchLabelNode:
//       SwitchLabelNodeCase  ExpNode
//       SwitchLabelNodeDefault -- none --
//
//     SwitchGroupNode:
//       SwitchGroupNode      SwitchLabelNode, StmtListNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//         PowerNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with sequences of kids, or internal
// nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode, BooleanNode, StringNode, IntLitNode,
//	  StrLitNode, TrueNode, FalseNode, IdNode, ReturnStmtNode
//
// (2) Internal nodes with (possibly empty) sequences of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,    ClassBodyNode, MethodBodyNode,
//        FieldDeclNode,  VarDeclNode,   MethodDeclNode, FormalDeclNode,
//        PrintStmtNode,  AssignStmtNode,IfStmtNode,     IfElseStmtNode,
//        WhileStmtNode,  CallStmtNode,  UnaryExpNode,   BinaryExpNode,
//        UnaryMinusNode, NotNode,       PlusNode,       MinusNode,
//        TimesNode,      DivideNode,    AndNode,        OrNode,
//        EqualsNode,     NotEqualsNode, LessNode,       GreaterNode,
//        LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************
abstract class ASTnode { 
    // every subclass must provide an decompile operation
    abstract public void decompile(PrintWriter p, int indent);

    // this method can be used by the decompile methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
	for (int k=0; k<indent; k++) p.print(" ");
    }
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
    }
}

// **********************************************************************
// ProgramNode, ClassBodyNode, DeclListNode, FormalsListNode,
// MethodBodyNode, StmtListNode, ExpListNode, SwitchGroupListNode
// **********************************************************************
class ProgramNode extends ASTnode {
    public static boolean errorNameAnalysis = false;
    public ProgramNode(IdNode id, ClassBodyNode classBody) {
	myId = id;
	myClassBody = classBody;
    }
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        SymbolTable symTab = new SymbolTable();
        symTabList.addFirst(symTab);
        myId.nameAnalysis(symTabList, scope, Types.ClassType, false); //class name is global
        myClassBody.nameAnalysis(symTabList, scope);
    }

    public void decompile(PrintWriter p, int indent) {
	p.print("public class ");
	myId.decompile(p, 0);
	p.println(" {");
	myClassBody.decompile(p, 2);
	p.println("}");
    }

    public void typeCheck(){
        if(errorNameAnalysis){
            Errors.fatal(0, 0, "Name analysis failed, not starting with type check");
            return;
        }
        myClassBody.typeCheck();
    }


    // 2 kids
    private IdNode myId;
    private ClassBodyNode myClassBody;
}

class ClassBodyNode extends ASTnode {
    public ClassBodyNode(DeclListNode declList) {
	myDeclList = declList;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        SymbolTable symbTab = new SymbolTable();
        symTabList.addFirst(symbTab); // new scope
        myDeclList.nameAnalysis(symTabList, scope);
        //check if the main method is declared
        SymbolTable symTabScope = symTabList.get(symTabList.size()-2);
        boolean mainDeclared = false;
        for (SymbolTable symTab: symTabList){
            if(symTab.lookup("main") != null && symTab.lookup("main").type() == Types.MethodTypeVoid){
                mainDeclared = true;
            }
        }
        if(!mainDeclared){
            Errors.fatal(0, 0, "No main method declared");
            ProgramNode.errorNameAnalysis = true;
        }
    }

    public void decompile(PrintWriter p, int indent) {
	myDeclList.decompile(p, indent);
    }

    public void typeCheck(){
        myDeclList.typeCheck();
    }

    // 1 kid
    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(Sequence S) {
	myDecls = S;
    }
    
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        try {
            for (myDecls.start(); myDecls.isCurrent(); myDecls.advance()) {
                ((DeclNode)myDecls.getCurrent()).nameAnalysis(symTabList, scope);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in DeclListNode.nameAnalysis");
            System.exit(-1);
        }
    }

    public void decompile(PrintWriter p, int indent) {
	try {
	    for (myDecls.start(); myDecls.isCurrent(); myDecls.advance()) {
		((DeclNode)myDecls.getCurrent()).decompile(p, indent);
	    }
	} catch (NoCurrentException ex) {
	    System.err.println("unexpected NoCurrentException in DeclListNode.print");
	    System.exit(-1);
	}
    }

    public void typeCheck(){
        try {
            for (myDecls.start(); myDecls.isCurrent(); myDecls.advance()) {
                ((DeclNode)myDecls.getCurrent()).typeCheck();
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in DeclListNode.typeCheck");
            System.exit(-1);
        }
    }
    public int length(){
        return myDecls.length();
    }

  // sequence of kids (DeclNodes)
  private Sequence myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(Sequence S) {
	myFormals = S;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        SymbolTable symTab = new SymbolTable();
        symTabList.addFirst(symTab); // new scope
        try {
            for (myFormals.start(); myFormals.isCurrent(); myFormals.advance()) {
                ((FormalDeclNode)myFormals.getCurrent()).nameAnalysis(symTabList, scope);
                myList.add(((FormalDeclNode)myFormals.getCurrent()).getType());
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.nameAnalysis");
            System.exit(-1);
        }
    }

    public void decompile(PrintWriter p, int indent) {
        p.print(" (");
        boolean first = true;
        try {
            for (myFormals.start(); myFormals.isCurrent(); myFormals.advance()) {
                if (!first) {
                    p.print(", ");
                }
                ((FormalDeclNode)myFormals.getCurrent()).decompile(p, indent);
                first = false;
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in FormalsListNode.print");
            System.exit(-1);
        }
        p.print(")");
    }

    public ArrayList<Integer> getFormalList(){
        return myList;
    }

    public int length(){
        return myFormals.length();
    }

  // sequence of kids (FormalDeclNodes)
    private Sequence myFormals;
    private ArrayList <Integer> myList = new ArrayList<Integer>();
}

class MethodBodyNode extends ASTnode {
    public MethodBodyNode(DeclListNode declList , StmtListNode stmtList) {
	myDeclList = declList;
	myStmtList = stmtList;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope){
        myDeclList.nameAnalysis(symTabList, scope);
        myStmtList.nameAnalysis(symTabList, scope);

    }

    public void decompile(PrintWriter p, int indent) {
        myDeclList.decompile(p, indent);
        myStmtList.decompile(p, indent);
    }

    public int getVarNumber(){
        return myDeclList.length();
    }

    public void typeCheck(){
        myStmtList.typeCheck();
    }

    // 2 kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(Sequence S) {
	myStmts = S;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        try {
            for (myStmts.start(); myStmts.isCurrent(); myStmts.advance()) {
                ((StmtNode)myStmts.getCurrent()).nameAnalysis(symTabList, scope);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in StmtListNode.nameAnalysis");
            System.exit(-1);
        }
    }

    public void decompile(PrintWriter p, int indent) {
        try {
            for (myStmts.start(); myStmts.isCurrent(); myStmts.advance()) {
                doIndent(p, indent);
                ((StmtNode)myStmts.getCurrent()).decompile(p, indent);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in StmtListNode.print");
            System.exit(-1);
        }
    }

    public void typeCheck(){
        try {
            for (myStmts.start(); myStmts.isCurrent(); myStmts.advance()) {
                ((StmtNode)myStmts.getCurrent()).typeCheck();
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in StmtListNode.typeCheck");
            System.exit(-1);
        }
    }

    // sequence of kids (StmtNodes)
    private Sequence myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(Sequence S) {
	myExps = S;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        try {
            for (myExps.start(); myExps.isCurrent(); myExps.advance()) {
                ((ExpNode)myExps.getCurrent()).lookup(symTabList, scope);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in ExpListNode.nameAnalysis");
            System.exit(-1);
        }
    }
    public void methodeNameAnlysis(LinkedList<SymbolTable> symTabList, int scope){
        try {
            for (myExps.start(); myExps.isCurrent(); myExps.advance()) {
                ((ExpNode)myExps.getCurrent()).lookup(symTabList, scope);
                if(((ExpNode)myExps.getCurrent())instanceof BinaryExpNode){
                    myList.add(((BinaryExpNode)myExps.getCurrent()).getType(0,0));
                } else{
                    myList.add(((ExpNode)myExps.getCurrent()).getType());
                }
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in ExpListNode.nameAnalysis");
            System.exit(-1);
        }
    }
    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        boolean first = true;
        try {
            for (myExps.start(); myExps.isCurrent(); myExps.advance()) {
                if (!first) {
                    p.print(", ");
                }
                ((ExpNode)myExps.getCurrent()).decompile(p, indent);
                first = false;
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in ExpListNode.print");
            System.exit(-1);
        }
        p.print(")");
    }
    public int length(){
        return myExps.length();
    }
    public ArrayList<Integer> getExpList(){
        return myList;
    }

    // sequence of kids (ExpNodes)
    private Sequence myExps;
    private ArrayList <Integer> myList = new ArrayList<Integer>();
}

// maybe add a nameAnalysis method to this class
class SwitchGroupListNode extends ASTnode {
    public SwitchGroupListNode(Sequence S) {
        mySwitchGroups = S;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        try {
            for (mySwitchGroups.start(); mySwitchGroups.isCurrent(); mySwitchGroups.advance()) {
                ((SwitchGroupNode)mySwitchGroups.getCurrent()).nameAnalysis(symTabList, scope);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in SwitchGroupListNode.nameAnalysis");
            System.exit(-1);
        }
    }

    public void decompile(PrintWriter p, int indent) {
        try {
            for (mySwitchGroups.start(); mySwitchGroups.isCurrent(); mySwitchGroups.advance()) {
                ((SwitchGroupNode)mySwitchGroups.getCurrent()).decompile(p, indent);
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in SwitchGroupList.print");
            System.exit(-1);
        }
    }

    public void typeCheck(){
        try {
            for (mySwitchGroups.start(); mySwitchGroups.isCurrent(); mySwitchGroups.advance()) {
                ((SwitchGroupNode)mySwitchGroups.getCurrent()).typeCheck();
            }
        } catch (NoCurrentException ex) {
            System.err.println("unexpected NoCurrentException in SwitchGroupList.typeCheck");
            System.exit(-1);
        }
    }

    // sequence of kids (SwitchGroupNodes)
    private Sequence mySwitchGroups;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************
abstract class DeclNode extends ASTnode
{
    abstract public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope);
    public void typeCheck(){
        //do nothing
    }
}

class FieldDeclNode extends DeclNode {
    public FieldDeclNode(TypeNode type, IdNode id) {
	myType = type;
	myId = id;
    } 
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myId.nameAnalysis(symTabList, scope, myType.returnType(), false); // all static vars are global
    }
    public void decompile(PrintWriter p, int indent) {
	doIndent(p, indent);
	p.print("static ");
	myType.decompile(p, indent);
	p.print(" ");
	myId.decompile(p, indent);
	p.println(";");
    }


    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id) {
	myType = type;
	myId = id;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myId.nameAnalysis(symTabList, scope, myType.returnType(),true); // all other vars are local
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.decompile(p, indent);
        p.print(" ");
        myId.decompile(p, indent);
        p.println(";");
    }


    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

class MethodDeclNode extends DeclNode {
    public MethodDeclNode(IdNode id, FormalsListNode formalList,
			  MethodBodyNode body) {
	myId = id;
	myFormalsList = formalList;
	myBody = body;
    }
    int num_local_vars;
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        num_local_vars = myBody.getVarNumber();
        myId.methodNameAnalyisis(symTabList, scope, Types.MethodTypeVoid,myFormalsList, num_local_vars);
        myFormalsList.nameAnalysis(symTabList, scope);
        myBody.nameAnalysis(symTabList, scope); 
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("public static void ");
        myId.decompile(p, indent);
        myFormalsList.decompile(p, indent);
        p.println(" {");
        myBody.decompile(p, indent+2);
        doIndent(p, indent);
        p.println("}");
        p.println("I have "+ myId.getTempvars() + "local vars"); //debugging helper
        p.println("I have "+  myId.getTempargs() + "arguments");
    }

    public void typeCheck(){
        myBody.typeCheck();
    }

    public FormalsListNode getFormalList (){
        return myFormalsList;
    }

    // 3 kids
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private MethodBodyNode myBody;
}

// added this to print out the method declaration for int return type. Might consider extending the original MethodDeclNode class to have a return type field
class MethodDeclNodeInt extends MethodDeclNode {
    public MethodDeclNodeInt(IdNode id, FormalsListNode formalList, MethodBodyNode body) {
                super(id,formalList,body);
	myId = id;
	myFormalsList = formalList;
	myBody = body;
    
    }
    int num_local_vars;
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
         num_local_vars = myBody.getVarNumber();
        myId.methodNameAnalyisis(symTabList, scope, Types.MethodTypeInt, myFormalsList, num_local_vars);
        myFormalsList.nameAnalysis(symTabList, scope); 
        myBody.nameAnalysis(symTabList, scope);
        
        //check if the method has a return statement
        if(symTabList.getFirst().lookup("return") == null){
            Errors.fatal(myId.getLineNum(), myId.getCharNum(), "Method must have a return statement");
            ProgramNode.errorNameAnalysis = true;
        }

        symTabList.removeFirst(); 
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("public static int ");
        myId.decompile(p, indent);
        myFormalsList.decompile(p, indent);
        p.println(" {");
        myBody.decompile(p, indent+2);
        doIndent(p, indent);
        p.println("}");
        p.println("I have "+ myId.getTempvars() + "local vars"); // debugging helper
        p.println("I have "+  myId.getTempargs() + "arguments");
    }

    public void typeCheck(){
        myBody.typeCheck();
    }

    public FormalsListNode getFormalList (){
        return myFormalsList;
    }

    // 3 kids
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private MethodBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
	myType = type;
	myId = id;
    }
    //these are all parameter, so they are local
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myId.nameAnalysis(symTabList, scope, myType.returnType(), true);
    }

    public void decompile(PrintWriter p, int indent) {
        myType.decompile(p, indent);
        p.print(" ");
        myId.decompile(p, indent);

    }
    public int getType(){
        return myId.getType();
    }

    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************
abstract class TypeNode extends ASTnode {
    abstract public int returnType();
}

class IntNode extends TypeNode
{
    public IntNode() {
    }

    public void decompile(PrintWriter p, int indent) {
	p.print("int");
    }
    public int returnType() {
        return Types.IntType;
    }
}

class BooleanNode extends TypeNode
{
    public BooleanNode() {
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("boolean");
    }

    public int returnType() {
        return Types.BoolType;
    }
}

class StringNode extends TypeNode
{
    public StringNode() {
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("String");
    }

    public int returnType() {
        return Types.StringType;
    }
}

// **********************************************************************
// SwitchLabelNode and its Subclasses
// **********************************************************************
abstract class SwitchLabelNode extends ASTnode {
    public abstract void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope);
    public abstract void typeCheck();
}

class SwitchLabelNodeCase extends SwitchLabelNode {
    public SwitchLabelNodeCase(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myExp.lookup(symTabList, scope);
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("case ");
        myExp.decompile(p, indent);
        p.println(":");
    }

    public void typeCheck(){
        if (!(myExp instanceof IntLitNode)){
            Errors.fatal(0, 0, "Case expression must be an integer literal");
        }
    }
    // 1 kid
    private ExpNode myExp;
}

class SwitchLabelNodeDefault extends SwitchLabelNode {
    public SwitchLabelNodeDefault() {
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
    }

    public void decompile(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.println("default:");
    }
    public void typeCheck(){
        //do nothing
    }
}
// **********************************************************************
// SwitchGroupNode
// **********************************************************************

class SwitchGroupNode extends ASTnode {
    public SwitchGroupNode(SwitchLabelNode sLabelNode,StmtListNode slist) {
        myStmtList = slist;
        mySwitchLabelNode = sLabelNode;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        mySwitchLabelNode.nameAnalysis(symTabList, scope);
         myStmtList.nameAnalysis(symTabList, scope);
    }
    public void decompile(PrintWriter p, int indent) {
        mySwitchLabelNode.decompile(p, indent);
        myStmtList.decompile(p, indent+2);
    }
    public void typeCheck(){
        myStmtList.typeCheck();
        mySwitchLabelNode.typeCheck();
    }
    // 2 kids
    private StmtListNode myStmtList;
    private SwitchLabelNode mySwitchLabelNode;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************
abstract class StmtNode extends ASTnode {
    public abstract void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope);
    public abstract void typeCheck();
}

class PrintStmtNode extends StmtNode {
    public PrintStmtNode(ExpNode exp) {
	myExp = exp;
    }
    
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myExp.lookup(symTabList, scope);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("System.out.println(");
        myExp.decompile(p, indent);
        p.println(");");
    }
    public void typeCheck(){
        if (myExp.getType() != Types.StringType){
            Errors.fatal(0, 0, "Attempt to print " + Types.ToString(myExp.getType()) + " as a string");
        }
        myType = myExp.getType(); // TODO: revalidate this
    }
    //assuming you can print any type so no typecheck done here
    // 1 kid
    private ExpNode myExp;
    private int myType; //unclear as of now why this needs to be done, printed statments are always Strings.
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(IdNode id, ExpNode exp) {
	myId = id;
	myExp = exp;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myId.lookup(symTabList, scope);
        myExp.lookup(symTabList, scope);
    }

    public void decompile(PrintWriter p, int indent) {
        myId.decompile(p, indent);
        p.print(" = ");
        myExp.decompile(p, indent);
        p.println(";");
    }
    public void typeCheck(){
        int lineNum = myId.getLineNum();
        int charNum = myId.getCharNum();
        int expType = myExp.getType();
        if(myExp instanceof UnaryExpNode){
            expType = ((UnaryExpNode)myExp).getType(lineNum, charNum);
        }
        if(myExp instanceof BinaryExpNode){
            expType = ((BinaryExpNode)myExp).getType(lineNum, charNum);
        }
        if(myId.getType() != expType){ 
            Errors.fatal(lineNum, charNum, "Type mismatch when assigning to " + myId.getStrVal());
        }
    }

    // 2 kids
    private IdNode myId;
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, StmtListNode slist) {
	myExp = exp;
	myStmtList = slist;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myExp.lookup(symTabList, scope);
        myStmtList.nameAnalysis(symTabList, scope);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("if (");
        myExp.decompile(p, indent);
        p.println(") {");
        myStmtList.decompile(p, indent+2);
        doIndent(p, indent);
        p.println("}");
    }

    public void typeCheck(){
        int myExpType = myExp.getType();
        if(myExp instanceof UnaryExpNode){
            myExpType = ((UnaryExpNode)myExp).getType(0, 0);
        }
        if(myExp instanceof BinaryExpNode){
            myExpType = ((BinaryExpNode)myExp).getType(0, 0);
        }
        if(myExpType != Types.BoolType){
            Errors.fatal(0, 0, "Non-boolean expression used as an if condition");
        }
        myStmtList.typeCheck();
    }

    // 2 kids
    private ExpNode myExp;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, StmtListNode slist1,
			  StmtListNode slist2) {
	myExp = exp;
	myThenStmtList = slist1;
	myElseStmtList = slist2;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("if (");
        myExp.decompile(p, indent);
        p.println(") {");
        myThenStmtList.decompile(p, indent+2);
        doIndent(p, indent);
        p.println("} else {");
        myElseStmtList.decompile(p, indent+2);
        doIndent(p, indent);
        p.println("}");
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myExp.lookup(symTabList, scope);
        myThenStmtList.nameAnalysis(symTabList, scope);
        myElseStmtList.nameAnalysis(symTabList, scope);
    }

    public void typeCheck(){
        int myExpType = myExp.getType();
        if(myExp instanceof UnaryExpNode){
            myExpType = ((UnaryExpNode)myExp).getType(0, 0);
        }
        if(myExp instanceof BinaryExpNode){
            myExpType = ((BinaryExpNode)myExp).getType(0, 0);
        }
        if(myExpType != Types.BoolType){
            Errors.fatal(0, 0, "Non-boolean expression used as an if condition");
        }
        myThenStmtList.typeCheck();
        myElseStmtList.typeCheck();
    }

    // 3 kids
    private ExpNode myExp;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, StmtListNode slist) {
	myExp = exp;
	myStmtList = slist;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myExp.lookup(symTabList, scope);
        myStmtList.nameAnalysis(symTabList, scope);
    }

    public void decompile(PrintWriter p, int indent) {
        p.println("do {");
        myStmtList.decompile(p, indent+2);
        doIndent(p, indent);
        p.print("} while (");
        myExp.decompile(p, indent);
        p.println(")");
    }

    public void typeCheck(){
        int myExpType = myExp.getType();
        if(myExp instanceof UnaryExpNode){
            myExpType = ((UnaryExpNode)myExp).getType(0, 0);
        }
        if(myExp instanceof BinaryExpNode){
            myExpType = ((BinaryExpNode)myExp).getType(0, 0);
        }
        if(myExpType != Types.BoolType){
            Errors.fatal(0, 0, "Non-boolean expression used as a while condition");
        }
        myStmtList.typeCheck();
    }

    // 2 kids
    private ExpNode myExp;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(IdNode id, ExpListNode elist) {
	myId = id;
	myExpList = elist;
    }

    public CallStmtNode(IdNode id) {
	myId = id;
	myExpList = new ExpListNode(new Sequence());
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myId.lookup(symTabList, scope);
        myExpList.methodeNameAnlysis(symTabList, scope);
        usedArgsList = myExpList.getExpList();
    }

    public void decompile(PrintWriter p, int indent) {
        myId.decompile(p, indent);
        myExpList.decompile(p, indent);
        p.println(";");
    }
 
    public void typeCheck(){
        if(myId.getType() == Types.MethodTypeVoid || myId.getType() == Types.MethodTypeInt){
            ArrayList<Integer> myFormalList = myId.getArgs().list().getFormalList();
           if (myExpList.length() != myId.getArgs().list().length()){
               Errors.fatal(myId.getLineNum(), myId.getCharNum(), "Method call argument length mismatch");
           } else{
                for (int i = 0; i < myExpList.length(); i++){
                    if (myFormalList.get(i) != usedArgsList.get(i)){
                        Errors.fatal(myId.getLineNum(),myId.getCharNum(), "Method call argument type mismatch");
                    }
                }
           }
        }
    }
    

    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList;
    private ArrayList<Integer> usedArgsList = new ArrayList<Integer>();
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode() {
    }
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        //nothing to do since there is no return value
    }

    public void decompile(PrintWriter p, int indent) {
        p.println("return;");
    }
    public void typeCheck(){
        //do nothing
    }
}
// this helper class has been added to handle return statements with values
class ReturnWithValueNode extends StmtNode {
    public ReturnWithValueNode(ExpNode exp) {
    myExp = exp;
    }
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myExp.lookup(symTabList, scope);
        symTabList.getFirst().insert("return", Types.ReturnIntType); // no need to insert isLocal for methode
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("return ");
        myExp.decompile(p, indent);
        p.println(";");
    }

    public void typeCheck(){
        int myExpType = myExp.getType();
        if(myExp instanceof UnaryExpNode){
            myExpType = ((UnaryExpNode)myExp).getType(0, 0);
        }
        if(myExp instanceof BinaryExpNode){
            myExpType = ((BinaryExpNode)myExp).getType(0, 0);
        }
        if(myExpType != Types.IntType){
            Errors.fatal(0, 0, "Return type does not match method return type int");
        }
    }

    // 1 kid
    private ExpNode myExp;
}
// added to handle nested blocks
class BlockStmtNode extends StmtNode {
    public BlockStmtNode(DeclListNode varDecls, StmtListNode stmts) {
        myVarDecls = varDecls;
        myStmts = stmts;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        SymbolTable symTab = new SymbolTable();
        symTabList.addFirst(symTab); // new scope
        myVarDecls.nameAnalysis(symTabList, scope);
        myStmts.nameAnalysis(symTabList, scope);
        symTabList.removeFirst(); 
    }

    public void decompile(PrintWriter p, int indent) {
        p.println("{");
        myVarDecls.decompile(p, indent+2);
        myStmts.decompile(p, indent+2);
        doIndent(p, indent);
        p.println("}");
    }

    public void typeCheck(){
        myStmts.typeCheck();
    }
    // 2 kids
    private DeclListNode myVarDecls;
    private StmtListNode myStmts;
}

class SwitchStmtNode extends StmtNode {
    public SwitchStmtNode(ExpNode exp, SwitchGroupListNode sgl) {
        myExp = exp;
        mySwitchGroupList = sgl;
    }

    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope) {
        myExp.lookup(symTabList, scope);
        mySwitchGroupList.nameAnalysis(symTabList, scope);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("switch (");
        myExp.decompile(p, indent);
        p.println(") {");
        mySwitchGroupList.decompile(p, indent+2);
        doIndent(p, indent);
        p.println("}");
    }

    public void typeCheck(){
        int myExpType = myExp.getType();
        if(myExp instanceof UnaryExpNode){
            myExpType = ((UnaryExpNode)myExp).getType(0, 0);
        }
        if(myExp instanceof BinaryExpNode){
            myExpType = ((BinaryExpNode)myExp).getType(0, 0);
        }
        if(myExpType != Types.IntType){
            Errors.fatal(0, 0, "Switch expression must be of type int");
        }
        mySwitchGroupList.typeCheck();
    }

    // 2 kids
    private ExpNode myExp;
    private SwitchGroupListNode mySwitchGroupList;
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************
abstract class ExpNode extends ASTnode {
    public void lookup(LinkedList<SymbolTable> symTabList, int scope) {
    }
    //used to get the type of the expression according to Types.java.
    public  int getType(){
        return Types.ErrorType;
    } 
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int colNum, int intVal) {
	myLineNum = lineNum;
	myColNum = colNum;
	myIntVal = intVal;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    public int getType() {
        return Types.IntType;
    }

    private int myLineNum;
    private int myColNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int colNum, String strVal) {
	myLineNum = lineNum;
	myColNum = colNum;
	myStrVal = strVal;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    public int getType() {
        return Types.StringType;
    }

    private int myLineNum;
    private int myColNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int colNum) {
	myLineNum = lineNum;
	myColNum = colNum;
    }

    public int getType() {
        return Types.BoolType;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("true");
    }

    private int myLineNum;
    private int myColNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int colNum) {
	myLineNum = lineNum;
	myColNum = colNum;
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("false");
    }

    public int getType() {
        return Types.BoolType;
    }

    private int myLineNum;
    private int myColNum;
}

class IdNode extends ExpNode
{   
    //TODO: remove when done
    boolean isLocal;
    int offset;
    public IdNode(int lineNum, int charNum, String strVal) {
	myLineNum = lineNum;
	myCharNum = charNum;
	myStrVal = strVal;
    }
    // check if idNode already exists in the symbol table and insert it if it doesn't
    public void nameAnalysis(LinkedList<SymbolTable> symTabList, int scope, int type, boolean isLocal) {
        boolean exists = false;
        this.isLocal = isLocal;
        //once used to check all scopes encapuslating the current scope, commented out since shadowing is allowed
        //commented instead of removed to keep the code for future reference
        //for (SymbolTable symTab: symTabList) {
        //    if (symTab.lookup(myStrVal) != null && symTab.lookup(myStrVal).type() != type) {
        //       exists = true;
                
        //   }
        //}
        //not allowed to redeclare a variable in the same scope
        if(symTabList.getFirst().lookup(myStrVal) != null) {
            exists = true;
        }
        if (!exists) {
            symTabList.getFirst().insert(myStrVal, type, isLocal);
            myType = type;
            offset = symTabList.getFirst().lookup(myStrVal).offset();
        } else {
            myType = Types.ErrorType;
            Errors.fatal(myLineNum, myCharNum, "Multiply declared identifier");
            ProgramNode.errorNameAnalysis = true;
        }
        
    }
    int tempargs; //TODO delete this, this was used for debugging. As soon as project is finished this is not needed anymore
    int tempvars;
    public void methodNameAnalyisis(LinkedList<SymbolTable> symTabList, int scope, int type, FormalsListNode symArgTabList, int num_local_vars) {
        boolean exists = false;
        SymbolTable symTab = symTabList.getFirst();
        if (symTab.lookup(myStrVal) != null) {
            exists = true;
            myType = symTab.lookup(myStrVal).type();
        }
        
        if (!exists) {
            symTabList.getFirst().insert(myStrVal, type, symArgTabList, num_local_vars, symArgTabList.length());
            myType = type;
            tempargs = symTabList.getFirst().lookup(myStrVal).getNumParams();
            tempvars = symTabList.getFirst().lookup(myStrVal).getNumLocalVars();
        } else {
            myType = Types.ErrorType;
            Errors.fatal(myLineNum, myCharNum, "Multiply declared identifier");
            ProgramNode.errorNameAnalysis = true;
        }
    }
    // TODO: delte this, this was used for debugging. As soon as project is finished this is not needed anymore
    int getTempargs(){
        return tempargs;
    }
    int getTempvars(){
        return tempvars;
    }
    public SymbolTable.Sym getArgs(){
        for (SymbolTable symTab: symArgTabList) {
            if (symTab.lookup(myStrVal) != null) {
                return symTab.lookup(myStrVal);
            }
        } 
        return null; 
    }
    // check if idNode exists in the symbol table and set the type of the idNode
    public void lookup(LinkedList<SymbolTable> symTabList, int scope) {
        boolean exists = false;
        for (SymbolTable symTab: symTabList) {
            if (symTab.lookup(myStrVal) != null) {
                exists = true;
                myType = symTab.lookup(myStrVal).type();
            }
            symArgTabList = symTabList;
        }
        if (!exists) {
            myType = Types.ErrorType;
            Errors.fatal(myLineNum, myCharNum, "Undeclared identifier");
            ProgramNode.errorNameAnalysis = true;
        }
    }

    public void decompile(PrintWriter p, int indent) {
	p.print(myStrVal + " (" + Types.ToString(myType) + " offset: "+offset+")" );
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private int myType;
    private LinkedList<SymbolTable> symArgTabList;

    public String getStrVal() {
        return myStrVal;
    }
    public int getLineNum() {
        return myLineNum;
    }
    public int getCharNum() {
        return myCharNum;
    }
    public int getType() {
        return myType;
    }
}

// added by me to have a seperate node for function calls inside an expression
class CallExpNode extends ExpNode {
    public CallExpNode(IdNode id, ExpListNode elist) {
	myId = id;
	myExpList = elist;
    }

    public CallExpNode(IdNode id) {
	myId = id;
	myExpList = new ExpListNode(new Sequence());
    }

    public void lookup(LinkedList<SymbolTable> symTabList, int scope) {
        myId.lookup(symTabList, scope);
        myExpList.methodeNameAnlysis(symTabList, scope);
        usedArgsList = myExpList.getExpList();
        if(myId.getType() == Types.MethodTypeInt){
            ArrayList<Integer> myFormalList = myId.getArgs().list().getFormalList();
            if(myExpList.length() != myId.getArgs().list().length()){
                Errors.fatal(myId.getLineNum(), myId.getCharNum(), "Method call argument length mismatch");
            } else{
                for (int i = 0; i < myExpList.length(); i++){
                    if (myFormalList.get(i) != usedArgsList.get(i)){
                        Errors.fatal(myId.getLineNum(),myId.getCharNum(), "Method call argument type mismatch");
                    }
                }
            }
        }
    }

    public void decompile(PrintWriter p, int indent) {
        myId.decompile(p, indent);
        myExpList.decompile(p, indent);
    }

    //checks if return type of the function is int
    public int getType() {
        if(myId.getType() == Types.MethodTypeInt){
            return Types.IntType;
        }
        return myId.getType();
    }

    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList;
    private ArrayList<Integer> usedArgsList = new ArrayList<Integer>();
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
	myExp = exp;
    }
    public void lookup(LinkedList<SymbolTable> symTabList, int scope) {
        myExp.lookup(symTabList, scope);
    }

    public abstract int getType(int lineNum, int charNum);

    // one child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode
{
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
	myExp1 = exp1;
	myExp2 = exp2;
    }
    public void lookup(LinkedList<SymbolTable> symTabList, int scope) {
        myExp1.lookup(symTabList, scope);
        myExp2.lookup(symTabList, scope);
    }
    public abstract int getType(int lineNum, int charNum);

    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode
{
    public UnaryMinusNode(ExpNode exp) {
	super(exp);
    }
    

    public void decompile(PrintWriter p, int indent) {
        p.print("(-");
        myExp.decompile(p, indent);
        p.print(")");
    }

    public int getType(int lineNum, int charNum) {
        if(myExp.getType() == Types.IntType){
            return Types.IntType;
        }
        Errors.fatal(lineNum, charNum, "Unary minus operator applied to non-integer");
        return Types.ErrorType;
    }
}

class NotNode extends UnaryExpNode
{
    public NotNode(ExpNode exp) {
	super(exp);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("!(");
        myExp.decompile(p, indent);
        p.print(")");
    }

    public int getType(int lineNum, int charNum) {
        if(myExp.getType() == Types.BoolType){
            return Types.BoolType;
        }
        Errors.fatal(lineNum, charNum, "Logical negation operator applied to non-boolean");
        return Types.ErrorType;
    }
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************
class PlusNode extends BinaryExpNode
{
    public PlusNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" + ");
        myExp2.decompile(p, indent);
        p.print(")");

    }
    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        int type2 = myExp2.getType();
        int returnType = Types.IntType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for addition");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of addition is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of addition is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class MinusNode extends BinaryExpNode
{
    public MinusNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" - ");
        myExp2.decompile(p, indent);
        p.print(")");
    }

    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        if (myExp1 instanceof BinaryExpNode){
            type1 = ((BinaryExpNode)myExp1).getType(lineNum, charNum);
        }
        int type2 = myExp2.getType();
        if (myExp2 instanceof BinaryExpNode){
            type2 = ((BinaryExpNode)myExp2).getType(lineNum, charNum);
        }
        int returnType = Types.IntType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for subtraction");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of subtraction is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of subtraction is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class TimesNode extends BinaryExpNode
{
    public TimesNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" * ");
        myExp2.decompile(p, indent);
        p.print(")");
    }
    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        if (myExp1 instanceof BinaryExpNode){
            type1 = ((BinaryExpNode)myExp1).getType(lineNum, charNum);
        }
        int type2 = myExp2.getType();
        if (myExp2 instanceof BinaryExpNode){
            type2 = ((BinaryExpNode)myExp2).getType(lineNum, charNum);
        }
        int returnType = Types.IntType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for multiplication");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of multiplication is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of multiplication is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class DivideNode extends BinaryExpNode
{
    public DivideNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" / ");
        myExp2.decompile(p, indent);
        p.print(")");
    }
    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        if (myExp1 instanceof BinaryExpNode){
            type1 = ((BinaryExpNode)myExp1).getType(lineNum, charNum);
        }
        int type2 = myExp2.getType();
        if (myExp2 instanceof BinaryExpNode){
            type2 = ((BinaryExpNode)myExp2).getType(lineNum, charNum);
        }
        int returnType = Types.IntType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for division");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of divison is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of division is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class AndNode extends BinaryExpNode
{
    public AndNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" && ");
        myExp2.decompile(p, indent);
        p.print(")");
    }
    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        if (myExp1 instanceof BinaryExpNode){
            type1 = ((BinaryExpNode)myExp1).getType(lineNum, charNum);
        }
        int type2 = myExp2.getType();
        if (myExp2 instanceof BinaryExpNode){
            type2 = ((BinaryExpNode)myExp2).getType(lineNum, charNum);
        }
        int returnType = Types.BoolType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for and");
            return Types.ErrorType;
        }
        
        if(type1 != Types.BoolType) {
            Errors.fatal(lineNum, charNum, "First operand of and is not a boolean");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.BoolType) {
            Errors.fatal(lineNum, charNum, "Second operand of and is not a boolean");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class OrNode extends BinaryExpNode
{
    public OrNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" || ");
        myExp2.decompile(p, indent);
        p.print(")");
    }
    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        if (myExp1 instanceof BinaryExpNode){
            type1 = ((BinaryExpNode)myExp1).getType(lineNum, charNum);
        }
        int type2 = myExp2.getType();
        if (myExp2 instanceof BinaryExpNode){
            type2 = ((BinaryExpNode)myExp2).getType(lineNum, charNum);
        }
        int returnType = Types.BoolType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for or");
            return Types.ErrorType;
        }
        
        if(type1 != Types.BoolType) {
            Errors.fatal(lineNum, charNum, "First operand of or is not a boolean");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.BoolType) {
            Errors.fatal(lineNum, charNum, "Second operand of or is not a boolean");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class EqualsNode extends BinaryExpNode
{
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" == ");
        myExp2.decompile(p, indent);
        p.print(")");
    }

    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        int type2 = myExp2.getType();
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for equal comparison");
            return Types.ErrorType;
        }
        // assuming you can compare every type
        if (type1 != type2){
            Errors.fatal(lineNum, charNum, "Equal comparison not possible for different types " + Types.ToString(type1) + " and " + Types.ToString(type2));
            return Types.ErrorType;
        }
        return Types.BoolType;

    }
}

class NotEqualsNode extends BinaryExpNode
{
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" != ");
        myExp2.decompile(p, indent);
        p.print(")");
    }

    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        int type2 = myExp2.getType();
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for not- equal comparison");
            return Types.ErrorType;
        }
        if (type1 != type2){
            Errors.fatal(lineNum, charNum, "Not-Equal comparison not possible for different types " + Types.ToString(type1) + " and " + Types.ToString(type2));
            return Types.ErrorType;
        }
        return Types.BoolType;

    }
}

class LessNode extends BinaryExpNode
{
    public LessNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" < ");
        myExp2.decompile(p, indent);
        p.print(")");
    }
    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        int type2 = myExp2.getType();
        int returnType = Types.BoolType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for less operation");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of less is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of less is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class GreaterNode extends BinaryExpNode
{
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" > ");
        myExp2.decompile(p, indent);
        p.print(")");
    }
    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        int type2 = myExp2.getType();
        int returnType = Types.BoolType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for greater");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of greater is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of greater is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class LessEqNode extends BinaryExpNode
{
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" <= ");
        myExp2.decompile(p, indent);
        p.print(")");
    }

    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        int type2 = myExp2.getType();
        int returnType = Types.BoolType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for less equals");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of less equals is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of less equals is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

class GreaterEqNode extends BinaryExpNode
{
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
	super(exp1, exp2);
    }

    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print(" >= ");
        myExp2.decompile(p, indent);
        p.print(")");
    }

    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        int type2 = myExp2.getType();
        int returnType = Types.BoolType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for greater equals");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of greater equals is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of greater equals is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}

//added to handle exp to the power of exp
class PowerNode extends BinaryExpNode
{
    public PowerNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    public void decompile(PrintWriter p, int indent) {
        p.print("(");
        myExp1.decompile(p, indent);
        p.print("**");
        myExp2.decompile(p, indent);
        p.print(")");
    }

    public int getType(int lineNum, int charNum){
        int type1 = myExp1.getType();
        if (myExp1 instanceof BinaryExpNode){
            type1 = ((BinaryExpNode)myExp1).getType(lineNum, charNum);
        }
        int type2 = myExp2.getType();
        if (myExp2 instanceof BinaryExpNode){
            type2 = ((BinaryExpNode)myExp2).getType(lineNum, charNum);
        }
        int returnType = Types.IntType;
        if(type1 == Types.ErrorType || type2 == Types.ErrorType) {
            Errors.fatal(lineNum, charNum, "Invalid expression for power");
            return Types.ErrorType;
        }
        
        if(type1 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "First operand of power is not an int");
            returnType = Types.ErrorType;
        }
        if(type2 != Types.IntType) {
            Errors.fatal(lineNum, charNum, "Second operand of power is not an int");
            returnType = Types.ErrorType;
        }
        return returnType;

    }
}
class Types {
    public static final int ClassType = 0;
    public static final int IntType = 1;
    public static final int BoolType = 2;
    public static final int StringType = 3;
    public static final int MethodTypeVoid = 4;
    public static final int MethodTypeInt = 5;
    public static final int ErrorType = 6;
    public static final int ReturnIntType = 7;

    public static String ToString(int v) {
        switch(v){
	case 0: return "class";
	case 1: return "int";
	case 2: return "boolean";
	case 3: return "String";
	case 4: return "method void";
    case 5: return "method int";
	case 6: return "error";
    case 7: return "return int";
	default: throw new RuntimeException();
        }
    }
}

import java.util.ArrayList;
import java.util.Hashtable;

public class SymbolTable {
    
    public class Sym {
		//default constructor only takes two args
		public Sym (String id, int t) {
			myName = id;
			myType = t; 
		}

		//constructor for methodes
		public Sym(String id, int t, FormalsListNode list, int num_local_vars, int num_params) {
			myName = id;
			myType = t;
			myList = list;
			this.num_local_vars = num_local_vars;
			this.num_params = num_params;
			
		}

		//construcotr for id
		public Sym(String id, int t, boolean isLocal, int offset){
			myName = id;
			myType = t;
			this.offset = offset;
			this.isLocal = isLocal;
		}

		public String name () { return myName; }
		public int type () { return myType; }
		public FormalsListNode list() { return myList; }
		// new getter for offset
		public int offset() { return offset; }
		// new getter for isLocal
		public boolean isLocal() { return isLocal; }
		// getter for vars and args
		public int getNumLocalVars(){
			return num_local_vars;
		}
		public int getNumParams(){
			return num_params;
		}

		// private fields
		private String myName;
		private int myType;
		private FormalsListNode myList;
		// offset for codegen
		private int offset;
		// field to set local or global
		private boolean isLocal;
		// fields to save number of vars and args of a methode
		private int num_local_vars;
		private int num_params;
    };

    Hashtable table;
	int offset = 0; //offsett for each symtable - it is always a new scope so starting at 4

    SymbolTable () { table = new Hashtable(); }

    public Sym lookup (String name) { 
		return (Sym) table.get(name); 
    }

	//insert default
    public Sym insert (String name, int type) {
		if (table.containsKey(name)) 
			return (Sym) table.get(name);
		Sym sym = new Sym(name, type);
		table.put(name, sym);
		return sym;
    }

	//insert methods
	public Sym insert (String name, int type, FormalsListNode list, int num_local_vars, int num_params) {
		if (table.containsKey(name))
			return (Sym) table.get(name);
		Sym sym = new Sym(name, type, list, num_local_vars, num_params);
		table.put(name, sym);
		return sym;
	}

	//insert ids
	public Sym insert(String name, int type, boolean isLocal){
		if (table.containsKey(name))
			return (Sym) table.get(name);
		offset+=4;
		Sym sym = new Sym(name, type, isLocal, offset);
		table.put(name, sym);
		return sym;
	}

	@Override
	public String toString() {
		return table.toString();
	}
}
import java.util.ArrayList;
import java.util.Hashtable;

public class SymbolTable {
    
    public class Sym {
		//default constructor only takes two args
		public Sym (String id, int t) {
			myName = id;
			myType = t; 
		}

		//constructor for functions
		public Sym(String id, int t, FormalsListNode list) {
			myName = id;
			myType = t;
			myList = list;
			
		}

		//construcotr for id
		public Sym(String id, int t, boolean isLocal){
			myName = id;
			myType = t;
			this.isLocal = isLocal;
		}

		public String name () { return myName; }
		public int type () { return myType; }
		public FormalsListNode list() { return myList; }
		// new getter and setter for offset
		public int offset() { return offset; }
		public void setOffset(int offset) { this.offset = offset; }
		// new getter for isLocal
		public boolean isLocal() { return isLocal; }

		// private fields
		private String myName;
		private int myType;
		private FormalsListNode myList;
		// offset for codegen
		private int offset;
		// field to set local or global
		private boolean isLocal;
    };

    Hashtable table;

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

	//insert functions
	public Sym insert (String name, int type, FormalsListNode list) {
		if (table.containsKey(name))
			return (Sym) table.get(name);
		Sym sym = new Sym(name, type, list);
		table.put(name, sym);
		return sym;
	}

	//insert ids
	public Sym insert(String name, int type, boolean isLocal){
		if (table.containsKey(name))
			return (Sym) table.get(name);
		Sym sym = new Sym(name, type, isLocal);
		table.put(name, sym);
		return sym;
	}

	@Override
	public String toString() {
		return table.toString();
	}
}
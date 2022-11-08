package com.oauth.automaton;

import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.HashMap;
import java.util.Objects;

// this class creates a data structure object to represent the property automaton edges.
// each edge represents (abstracts) an instruction
public class PropertyAutoEdge {

    public static String TYPE_GETFIELD = "getfield";
    public static String TYPE_BINARYOP = "binaryop";
    public static String TYPE_CONDITIONAL_BRANCH = "conditional branch";
    public static String TYPE_DISPATCH = "dispatch";

    public static String KEY_FIELD_OP = "Operator";

    // to define method name for invoke/dispatch instruction
    public static String KEY_USE_METHOD = "MethodName";
    // to define the field name for 'getfield' instruction
    public static String KEY_USE_FIELDNAME = "FieldName";
    public static String KEY_USE_ARG1 = "Arg1";
    public static String KEY_USE_ARG2 = "Arg2";
    public static String KEY_USE_ARG3 = "Arg3";

    public String instName = null;

    //binaryop(eq) v12 , v15:#GET
    // eq -> instField, #GET ->  instValue
    private HashMap<String,String> fields = HashMapFactory.make();
    private HashMap<String,String> uses = HashMapFactory.make();

    public PropertyAutoEdge(String instName) {
        this.instName = instName;
    }

    public void setField(String key, String val) {
        fields.put(key, val);
    }

    public String getField(String key) {
        return fields.get(key);
    }

    public void setUse(String key, String val) {
        uses.put(key, val);
    }

    public String getUse(String key) {
        return uses.get(key);
    }

    // checks if a PropertyAutoEdge equivalent to a input Statement
    public boolean isEquivalent(Statement stmt) {
        if (stmt.getKind() != Kind.NORMAL) {
            // we don't check equivalent for non-Normal statements
            return false;
        }

        NormalStatement ns = (NormalStatement) stmt;
        SSAInstruction inst = ns.getInstruction();

        if (this.instName == TYPE_GETFIELD
            && inst instanceof SSAGetInstruction) {
            if (this.getField(KEY_USE_FIELDNAME) == null){
                System.err.println("Error: Field name must be set for getfield");
                return false;
            } else if (((SSAGetInstruction) inst)
                .getDeclaredField()
                .getName()
                .toString()
                .equals(this.getField(KEY_USE_FIELDNAME))) {
                return true;
            }
        } else if (this.instName == TYPE_BINARYOP
            && inst instanceof SSABinaryOpInstruction) {
            SSABinaryOpInstruction instbinary = (SSABinaryOpInstruction) inst;
            //pass the symbol table to get the val2 (RHS contant of the binary op)
            String val1 = instbinary.getVal1String(stmt.getNode().getIR().getSymbolTable());
            String val2 = instbinary.getVal2String(stmt.getNode().getIR().getSymbolTable());

            // check if [instruction's operator == edge.operator]
            // and [instruction's val1 == edge.val1] or [instruction's val2 == edge.val2]
            // instructions val2 is returned as v10:#CONSTANT, so we use endswith.
            if (instbinary.getOperator().toString().equals(this.getField(KEY_FIELD_OP))) {
                if ((this.getUse(KEY_USE_ARG1) != null && val1.endsWith(this.getUse(KEY_USE_ARG1))) ||
                    (this.getUse(KEY_USE_ARG2) != null && val2.endsWith(this.getUse(KEY_USE_ARG2)))) {
                    return true;
                }
            }
        } else if (this.instName == TYPE_CONDITIONAL_BRANCH
            && inst instanceof SSAConditionalBranchInstruction) {
            // for now, ignoring conditional branch types (e.g., eq, )
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyAutoEdge that = (PropertyAutoEdge) o;
        return Objects.equals(instName, that.instName) &&
            Objects.equals(fields, that.fields) &&
            Objects.equals(uses, that.uses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instName, fields, uses);
    }
}

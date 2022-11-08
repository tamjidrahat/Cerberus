package com.oauth.util;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.viz.NodeDecorator;

public class SDGStatementDecorator implements NodeDecorator<Statement> {

    @Override
    public String getLabel(Statement stmt) throws WalaException {
        if (stmt.toString().contains("prologue.js")) {
            return "prologue.js";
        }
        int MAX_LENGTH = 40;
        String result = "";

        if (stmt instanceof NormalStatement) {
            SSAInstruction inst = ((NormalStatement)stmt).getInstruction();
            result = "[N] "+inst.toString();
        }
        else {

            //return stmt.getKind().name().toString();

            IMethod method = stmt.getNode().getMethod();
            String origName = method.getName().toString();

            result = origName;
            if (origName.equals("do") || origName.equals("ctor")) {
                result = method.getDeclaringClass().getName().toString();
                result = result.substring(result.lastIndexOf('/') + 1);
                if (result.equals("LFunction")) {
                    String s = method.toString();
                    if (s.indexOf('(') != -1) {
                        String functionName = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
                        functionName = functionName.substring(functionName.lastIndexOf('/') + 1);
                        result += ' ' + functionName;
                    }
                    if (origName.equals("ctor")) {
                        result = "ctor<" + result+">";
                        //result = "ctor";
                    } else if (origName.equals("do")) {
                        result = "do<" + result+">";
                        //result = "do";
                    }

                    if (result.length() > MAX_LENGTH) {
                        result = result.substring(result.length()-MAX_LENGTH-1);
                    }
                    result = stmt.getKind().name() +": "+ result;
                }

            }
        }
        return result;
    }

}

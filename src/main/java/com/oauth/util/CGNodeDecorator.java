package com.oauth.util;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.viz.NodeDecorator;

public class CGNodeDecorator implements NodeDecorator<CGNode> {

    @Override
    public String getLabel(CGNode n) throws WalaException {
        int MAX_LENGTH = 40;
        IMethod method = n.getMethod();
        String origName = method.getName().toString();

        String result = origName;
        if (origName.equals("do") || origName.equals("ctor")) {
            result = method.getDeclaringClass().getName().toString();
            result = result.substring(result.lastIndexOf('/') + 1);
            if (origName.equals("ctor")) {
                if (result.equals("LFunction")) {
                    String s = method.toString();
                    if (s.indexOf('(') != -1) {
                        String functionName = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
                        functionName = functionName.substring(functionName.lastIndexOf('/') + 1);
                        result += ' ' + functionName;
                    }
                }
                result = "ctor<" + result+">";
                //result = "ctor";
            }
        }
        if (result.length() > MAX_LENGTH) {
            result = result.substring(result.length()-MAX_LENGTH-1);
        }
        return result;
    }
}

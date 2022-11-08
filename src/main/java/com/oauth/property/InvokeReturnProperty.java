package com.oauth.property;

// check the returned value of a method invoked for the field
// e.g., if (redirect_uri.indexOf('#') != -1)

import com.ibm.wala.util.collections.HashMapFactory;
import java.util.HashMap;


public class InvokeReturnProperty {
    // name of the method invoked (e.g., indexOf)
    private String invokeMethodName;

    // name of the field for which the method was invoked (e.g., fieldName)
    private String fieldName;

    private String operator;

    private String constVal;

    private HashMap<String,String> argsMap = HashMapFactory.make();

    // if(method())
    public InvokeReturnProperty(String methodName) {
        this.invokeMethodName = methodName;
    }
    // if (field.method())
    public InvokeReturnProperty(String methodName, String fieldName) {
        this.invokeMethodName = methodName;
        this.fieldName = fieldName;
    }

    // if (field.method() == constantValue)
    public InvokeReturnProperty(String methodName, String fieldName, String operator, String constVal) {
        this.invokeMethodName = methodName;
        this.fieldName = fieldName;
        this.operator = operator;
        this.constVal = constVal;
    }

    // if (!field.method())
    public InvokeReturnProperty(String methodName, String fieldName, String operator) {
        this.invokeMethodName = methodName;
        this.fieldName = fieldName;
        this.operator = operator;
    }

    public void setInvokeArgs(String argNum, String argVal) {
        this.argsMap.put(argNum, argVal);
    }

    public String getInvokeArgs(String argNum) {
        return this.argsMap.get(argNum);
    }

    public String getInvokeMethodName(){
        return invokeMethodName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOperator() {
        return operator;
    }

    public String getConstVal() {
        return constVal;
    }





}

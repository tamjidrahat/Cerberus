package com.oauth.property;

public class GlobalMethodProperty {
    //property type: url.parse(redirect_uri)
    //pattern: globalName.method(field)
    private String globalName;
    private String methodName;
    private String fieldName;

    public GlobalMethodProperty(String globalName, String methodName) {
        this.globalName = globalName;
        this.methodName = methodName;
    }

    public GlobalMethodProperty(String globalName, String methodName, String fieldName) {
        this.globalName = globalName;
        this.methodName = methodName;
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getGlobalName() {
        return globalName;
    }

    public String getMethodName() {
        return methodName;
    }
}

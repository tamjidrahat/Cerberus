package com.oauth.property;

import java.util.List;

// checks property: if (field == val) -> Error
public class FieldValueProperty {

    // field of which value is to be checked
    private List<String> fields;

    // constant value that checked against the field value
    private String constVal;

    // operator such as eq, ne, ge etc.
    private String operator;

    public FieldValueProperty(List<String> fields, String op, String val) {
        this.fields = fields;
        this.operator = op;
        this.constVal = val;
    }
    public List<String> getFields() {
        return fields;
    }
    public String getOperator() {
        return operator;
    }
    public String getConstVal() {
        return constVal;
    }
}

package com.oauth.oauthguard;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.SDG;
import com.oauth.property.FieldValueProperty;
import com.oauth.property.GlobalMethodProperty;
import com.oauth.property.InvokeReturnProperty;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

public class PropertyCheckManager {
    SDG<InstanceKey> sdgData;
    SDG<InstanceKey> sdgControl;
    Set<FieldValueProperty> properties;

    public PropertyCheckManager(SDG<InstanceKey> sdgData, SDG<InstanceKey> sdgControl) {
        this.sdgData = sdgData;
        this.sdgControl = sdgControl;

        checkStateParam();
        checkAbsoluteRedirectUri();
        checkFragmentInRedirectUri();
        checkPKCESupportAuthorization();
        checkRequestMethodToken();
        checkContentEncodingToken();
        checkPKCESupportToken();
        revokeCodeUsedOnce();
        revokeTokenIfCodeUsedMultipleTimes();
        checkIfCoundBoundToUser();
        checkIfCodeBoundToClient();
        checkTokenInQuery();
        checkRequestIfTokenInBody();
        checkContentEoncodingIfTokenInBody();
        checkIfTokenBoundToUser();


        // use this to debug a path from src --> dest
        //new ChectTestProperty(sdgData, sdgControl).getResult();
    }

    private void checkStateParam() {
        System.out.println("Property: checkStateParam:: !request.state ");
        new CheckFieldValueProperty(sdgData,sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("body","state")),"ne", "POST"))
            .getResult();
        new CheckFieldValueProperty(sdgData,sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("query","state")),"ne", "POST"))
            .getResult();

    }
    private void checkAbsoluteRedirectUri() {
        System.out.println("Property: checkAbsoluteRedirectUri:: redirect_uri.test() ");
        new CheckInvokeReturnProperty(sdgData,
            new InvokeReturnProperty("test", "redirect_uri")).getResult();
    }

    private void checkFragmentInRedirectUri() {
        System.out.println("Property:checkFragmentInRedirectUri:: redirect_uri.indexOf('#') != -1");
        new CheckInvokeReturnProperty(sdgData,
            new InvokeReturnProperty("indexOf", "redirect_uri")).getResult();

        System.out.println("Property:checkFragmentInRedirectUri:: url.parse(redirect_uri)");
        new CheckGlobalMethodCall(sdgData,sdgControl,
            new GlobalMethodProperty("url", "parse", "redirect_uri")).getResult();
    }

    private void checkPKCESupportAuthorization() {
        System.out.println("Property:checkPKCESupportAuthorization::  code_challenge!== null");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
                (new LinkedList<String>(Arrays.asList("code_challenge")),"strict_ne", "")).getResult();

    }

    private void checkRequestMethodToken() {
        System.out.println("Property:checkRequestMethodToken:: method !== POST");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("method")),"strict_ne", "POST")).getResult();

    }

    private void checkContentEncodingToken() {
        System.out.println("Property:checkContentEncodingToken:: request.is(application/x-www-form-urlencoded)");
        new CheckInvokeReturnProperty(sdgData,
            new InvokeReturnProperty("is", "request")).getResult();
    }

    private void checkPKCESupportToken() {
        System.out.println("Property:checkPKCESupportAuthorization::  code_verrifier!== null");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("code_verrifier")),"strict_ne", "")).getResult();

    }

    private void revokeCodeUsedOnce() {
        System.out.println("Property: revokeCodeUsedOnce:: !request.code ");
        new CheckFieldValueProperty(sdgData,sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("code")),"ne", ""))
            .getResult();

    }

    private void revokeTokenIfCodeUsedMultipleTimes() {
        System.out.println("Property: revokeTokenIfCodeUsedMultipleTimes:: !request.access_token ");
        new CheckFieldValueProperty(sdgData,sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("access_token")),"ne", ""))
            .getResult();
    }

    private void checkIfCoundBoundToUser() {
        System.out.println("Property:checkIfCoundBoundToUser::  code.user!=null");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
                (new LinkedList<String>(Arrays.asList("code", "user")),"neg", "")).getResult();
    }

    private void checkIfCodeBoundToClient() {
        System.out.println("Property:checkIfCodeBoundToClient::  code.client_id!=null");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("code", "client_id")),"neg", "")).getResult();
    }

    private void checkTokenInQuery() {
        System.out.println("Property:checkTokenInQuery::  query.access_token");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("query", "access_token")),"neg", "")).getResult();
    }

    private void checkRequestIfTokenInBody() {
        System.out.println("Property:checkRequestIfTokenInBody:: method !== POST");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("query", "access_token")),"neg", "")).getResult();
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("method")),"strict_ne", "POST")).getResult();
    }

    private void checkContentEoncodingIfTokenInBody() {
        System.out.println("Property:checkContentEoncodingIfTokenInBody:: request.is(application/x-www-form-urlencoded)");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("body", "access_token")),"neg", "")).getResult();
        new CheckInvokeReturnProperty(sdgData,
            new InvokeReturnProperty("is", "request")).getResult();
    }

    private void checkIfTokenBoundToUser() {
        System.out.println("Property:checkIfTokenBoundToUser::  token.user!=null");
        new CheckFieldValueProperty(sdgData, sdgControl, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("token", "user")),"neg", "")).getResult();
    }




}

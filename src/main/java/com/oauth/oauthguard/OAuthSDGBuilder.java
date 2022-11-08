package com.oauth.oauthguard;

import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;

public class OAuthSDGBuilder {
    CallGraph cg;
    PointerAnalysis<InstanceKey> pa;
    //SDG<InstanceKey> sdg;

    DataDependenceOptions dataOption = DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS;
    ControlDependenceOptions ctrlOption = ControlDependenceOptions.NONE;

    public OAuthSDGBuilder(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
        this.cg = cg;
        this.pa = pa;
    }
    public SDG<InstanceKey> buildSDGForJS(boolean dataOp, boolean controlOp) {
        if (dataOp == true && controlOp == true) {
            return new SDG<InstanceKey>(cg, pa
                , new JavaScriptModRef<InstanceKey>()
                , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
                , ControlDependenceOptions.FULL);
        }
        else if (dataOp == true) {
            return new SDG<InstanceKey>(cg, pa
                , new JavaScriptModRef<InstanceKey>()
                , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
                , ControlDependenceOptions.NONE); //ignore control edges
        }
        else if (controlOp == true) {
            return new SDG<InstanceKey>(cg, pa
                , new JavaScriptModRef<InstanceKey>()
                , DataDependenceOptions.NONE
                , ControlDependenceOptions.FULL); //ignore control edges
        }
        else {
            System.err.println("Data and control options both cannot be False!");
            return null;
        }

    }
    public SDG<InstanceKey> buildSDGForJava(boolean dataOp, boolean controlOp) {
        if (dataOp == true && controlOp == true) {
            return new SDG<InstanceKey>(cg, pa
                , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
                , ControlDependenceOptions.FULL);
        }
        else if (dataOp == true) {
            return new SDG<InstanceKey>(cg, pa
                , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
                , ControlDependenceOptions.NONE); //ignore control edges
        }
        else if (controlOp == true) {
            return new SDG<InstanceKey>(cg, pa
                , DataDependenceOptions.NONE
                , ControlDependenceOptions.FULL); //ignore control edges
        }
        else {
            System.err.println("Data and control options both cannot be False!");
            return null;
        }

    }
}

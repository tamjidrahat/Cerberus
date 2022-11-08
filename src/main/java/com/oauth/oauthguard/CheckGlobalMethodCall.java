package com.oauth.oauthguard;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.oauth.oauthguard.TaintAnalysis.EndpointFinder;
import com.oauth.property.GlobalMethodProperty;
import com.oauth.util.BasicBlockUtil;
import java.util.List;
import java.util.Set;

public class CheckGlobalMethodCall {
    SDG<InstanceKey> sdgData;
    SDG<InstanceKey> sdgControl;
    GlobalMethodProperty property;

    CheckGlobalMethodCall(SDG<InstanceKey> sdgData, SDG<InstanceKey> sdgControl, GlobalMethodProperty p) {
        this.sdgData = sdgData;
        this.sdgControl = sdgControl;
        this.property = p;
    }
    public boolean getResult() {
        System.out.println("SDG search:: source -> global "+
            property.getGlobalName());
        System.out.println("SDG search:: sink -> "+property.getMethodName());

        Set<List<Statement>> paths = new SDGSearchManager(sdgData)
            .searchPathsSDG(sourceGlobal, sinkMethodName);

        // if no path exists for field1, then ignore search for field2
        if (BasicBlockUtil.printPaths(paths) == false) {
            return false;
        }
        if(property.getFieldName() != null) {
            System.out.println("SDG search:: source -> field: "+
                property.getFieldName());
            System.out.println("SDG search:: sink -> "+property.getMethodName());

            paths = new SDGSearchManager(sdgData)
                .searchPathsSDG(sourceFieldName, sinkMethodName);
            if (BasicBlockUtil.printPaths(paths) == false) {
                return false;
            }
        }
        return true;
    }

    private EndpointFinder sourceGlobal =
        new EndpointFinder() {
            @Override
            public boolean endpoint(Statement s) {
                if (s.getKind() == Kind.NORMAL) {
                    NormalStatement ns = (NormalStatement) s;
                    SSAInstruction inst = ns.getInstruction();
                    if (inst instanceof AstGlobalRead) {
                        AstGlobalRead global = (AstGlobalRead) inst;
                        if (global.getGlobalName().equals("global "+property.getGlobalName())) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };

    private EndpointFinder sourceFieldName =
        new EndpointFinder() {
            @Override
            public boolean endpoint(Statement s) {
                if (s.getKind() == Kind.NORMAL) {
                    NormalStatement ns = (NormalStatement) s;
                    SSAInstruction inst = ns.getInstruction();
                    if (inst instanceof SSAGetInstruction) {
                        if (((SSAGetInstruction) inst)
                            .getDeclaredField()
                            .getName()
                            .toString()
                            .equals(property.getFieldName())) {
                            return true;
                        }
                    }
                }

                return false;
            }
        };

    public EndpointFinder sinkMethodName = new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
            if (s.getKind()==Kind.PARAM_CALLER) {
                ParamCaller pCaller = (ParamCaller)s;
                SSAInstruction inst = pCaller.getInstruction();

                // get String value of dispatch instruction's first arg -> which contains invoke methodName
                String val0 = s.getNode().getIR().getSymbolTable().getValueString(inst.getUse(0));

                if (val0.endsWith(property.getMethodName())) {
                    return true;
                }
            }

            return false;
        }
    };


}

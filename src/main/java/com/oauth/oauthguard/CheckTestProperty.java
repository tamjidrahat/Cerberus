package com.oauth.oauthguard;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSAInstruction;
import com.oauth.oauthguard.TaintAnalysis.EndpointFinder;
import com.oauth.util.BasicBlockUtil;
import java.util.List;
import java.util.Set;

public class CheckTestProperty {
    SDG<InstanceKey> sdgData;
    SDG<InstanceKey> sdgControl;

    CheckTestProperty(SDG<InstanceKey> sdgData,SDG<InstanceKey> sdgControl) {
        this.sdgData = sdgData;
        this.sdgControl = sdgControl;
    }

    public boolean getResult() {
        Set<List<Statement>> paths = new SDGSearchManager(sdgData)
            .searchPathsSDG(sourceAuthGrant, sinkAuthGrant);

        return BasicBlockUtil.printPaths(paths);
    }

  public static EndpointFinder sourceAuthGrant =
      new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
          //            if (s.getKind()==Kind.PARAM_CALLER) {
          //                MethodReference ref =
          // ((ParamCaller)s).getInstruction().getCallSite().getDeclaredTarget();
          //                if (ref.getName().toString().equals("getAuthorizationCodeGrant")) {
          //                    return true;
          //                }
          //            }
          if (s.getKind() == Kind.NORMAL && s.toString().contains("getAuthorizationCodeGrant")) {
            System.out.println("Source: "+s);
            return true;
          }

          return false;
        }
      };

    public static EndpointFinder sinkAuthGrant = new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
            //System.out.println("--dest--"+s.toString());
            if (s.getKind() == Kind.NORMAL && s.toString().contains("createAccessToken")) {
                System.out.println("Sink: "+s);
                return true;
            }
            return false;
        }
    };

  private EndpointFinder source =
      new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
          if (s.getKind() == Kind.NORMAL) {
            NormalStatement ns = (NormalStatement) s;
            SSAInstruction inst = ns.getInstruction();
            //                              if (inst instanceof SSAGetInstruction) {
            //                                  if (((SSAGetInstruction) inst)
            //                                      .getDeclaredField()
            //                                      .getName()
            //                                      .toString()
            //                                      .equals(property.getFields().get(0))) {
            //                                      return true;
            //                                  }
            //                              }
            if (inst instanceof AstGlobalRead) {
              AstGlobalRead global = (AstGlobalRead) inst;
              if (global.getGlobalName().equals("global url")) {
                System.out.println("-----global---->" + s);
                return true;
              }
            }
          }
          //          if (s.toString().contains("global:global url")) {
          //            return true;
          //          }

          return false;
        }
      };

  private EndpointFinder sink =
      new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
          //                if (s.getKind() == Kind.NORMAL) {
          //                    NormalStatement ns = (NormalStatement) s;
          //                    SSAInstruction inst = ns.getInstruction();
          //                    if (inst instanceof SSAGetInstruction) {
          //                        if (((SSAGetInstruction) inst)
          //                            .getDeclaredField()
          //                            .getName()
          //                            .toString()
          //                            .equals(property.getFields().get(1))) {
          //                            return true;
          //                        }
          //                    }
            //                }
          if (s.toString().contains("dispatch 31")) {
            System.out.println("->"+s);
            return true;
          }

          return false;
        }
      };
}

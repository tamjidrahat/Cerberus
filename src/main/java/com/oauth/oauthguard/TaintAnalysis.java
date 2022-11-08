package com.oauth.oauthguard;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TaintAnalysis {

    interface EndpointFinder {

        boolean endpoint(Statement s);

    }

    public static EndpointFinder sendMessageSink = new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
            if (s.getKind()==Kind.PARAM_CALLER) {
                MethodReference ref = ((ParamCaller)s).getInstruction().getCallSite().getDeclaredTarget();
                if (ref.getName().toString().equals("sendTextMessage")) {
                    return true;
                }
            }

            return false;
        }
    };

    public static EndpointFinder documentWriteSink = new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
            //System.out.println("--dest--"+s.toString());
            if (s.getKind()==Kind.PARAM_CALLEE) {
                String ref = ((ParamCallee)s).getNode().getMethod().toString();

                if (ref.equals("<Code body of function LnodeserverDB.js/DB/nodeserverDB.js@494>")) { //494-read, 547-write
                    return true;
                }
            }
            return false;
        }
    };

  public static EndpointFinder conditionalBranchSink =
      new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
          // System.out.println("--dest--"+s.toString());
          // if it's an statement from a ctor node, ignore it.
          // ctor doesn't contain any necessary statement. but, it often has a conditional branch.
          //            if (s.getNode().getMethod().toString().contains("Lprologue.js")) {
          //                return false;
          //            }
          if (s.getNode().getMethod().getName().toString().equals("ctor")) {
            return false;
          }
          if (s.getKind() == Kind.NORMAL) {
            NormalStatement ns = (NormalStatement) s;
            SSAInstruction inst = ns.getInstruction();

            if (inst instanceof SSAConditionalBranchInstruction) {
              return true;
            }
          }
          return false;
        }
      };

    public static EndpointFinder getDeviceSource = new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
            if (s.getKind()==Kind.NORMAL_RET_CALLER) {
                MethodReference ref = ((NormalReturnCaller)s).getInstruction().getCallSite().getDeclaredTarget();
                if (ref.getName().toString().equals("getDeviceId")) {
                    return true;
                }
            }

            return false;
        }
    };

  public static EndpointFinder getFieldMethodSource =
      new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
          if (s.getKind() == Kind.NORMAL) {
            NormalStatement ns = (NormalStatement) s;
            SSAInstruction inst = ns.getInstruction();
            if (inst instanceof SSAGetInstruction) {
              // System.out.println("--src--"+((SSAGetInstruction)
              // inst).getDeclaredField().getName().toString());
              if (((SSAGetInstruction) inst)
                  .getDeclaredField()
                  .getName()
                  .toString()
                  .equals("method")) {
                return true;
              }
            }
          }

          return false;
        }
      };

  public static EndpointFinder getFieldRedirectUriSource =
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
                  .equals("redirect_uri")) {
                return true;
              }
            }
          }

          return false;
        }
      };

    private static void printPath(List<Statement> path) {
        System.out.println("Path (Instructions): [ ");
        for (Statement s: path) {
            if (s instanceof NormalStatement) {
                NormalStatement ns = (NormalStatement) s;
                System.out.println(" -> "+ns.getInstruction().toString());
                    //+"["+ns.getInstruction().getClass().getName().replace("com.ibm.wala.cast.js","")+"]");
            }
        }
        System.out.println("]");
    }
    private static void printBasicBlockPath(List<BasicBlock> bbpath) {
        System.out.println("BasicBlock path: [");
        for(BasicBlock bb: bbpath) {
            System.out.println("<"+bb.getMethod().getSignature()+", "+bb.getNumber()+">, ");
        }
        System.out.println("]");
    }

    private static List<BasicBlock> getBasicBlocksFromPath(List<Statement> path) {
        List<BasicBlock> bbList = new ArrayList<>();
        BasicBlock prevBlockinPath = null;

        for(Statement stmt: path) {
            if (stmt instanceof NormalStatement) {
                SSAInstruction inst = ((NormalStatement)stmt).getInstruction();
                BasicBlock bb = stmt.getNode().getIR().getControlFlowGraph()
                    .getBlockForInstruction(inst.iIndex());

                if (prevBlockinPath != null && bb.equals(prevBlockinPath)) {
                    continue;
                }
                bbList.add(bb);
                prevBlockinPath = bb;

            }
        }
        return bbList.size()!=0? bbList:null;
    }
    public static Set<List<Statement>> getPaths(SDG<? extends InstanceKey> G, EndpointFinder sources, EndpointFinder sinks) {
        Set<List<Statement>> result = HashSetFactory.make();
        Set<List<BasicBlock>> resultBlocks = HashSetFactory.make();

        for(Statement src : G) {
            if (sources.endpoint(src)) {
                for(Statement dst : G) {
                    if (sinks.endpoint(dst)) {
//                        System.out.println("src: "+src.toString());
//                        System.out.println("dest: "+dst.toString());

//                        BFSPathFinder<Statement> paths = new BFSPathFinder<Statement>(G, src, dst);
//                        List<Statement> path = paths.find();
                        Set<List<Statement>> paths = new SDGSearchManager(G).getPathsBacktracking(src, dst);

                        if (paths != null) {
                            System.out.println("Statement path size: "+paths.size());
                            for(List<Statement> path: paths) {
                                printPath(path);
                                result.add(path);

                                List<BasicBlock> bbPath = getBasicBlocksFromPath(path);
                                //printBasicBlockPath(bbPath);
                                resultBlocks.add(bbPath);
                            }

                        }else {
                            System.out.println("No path found");
                        }
                        System.out.println();
                        System.out.println();
                    }
                }
            }
        }
        System.out.println("BB path size: "+resultBlocks.size());
        for(List<BasicBlock> bbpath: resultBlocks) {
            printBasicBlockPath(bbpath);
        }
        return result;
    }
}
package com.oauth.oauthguard;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.HashSetFactory;
import com.oauth.oauthguard.TaintAnalysis.EndpointFinder;
import com.oauth.property.InvokeReturnProperty;
import com.oauth.util.BasicBlockUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CheckInvokeReturnProperty {

    SDG<InstanceKey> sdg;
    InvokeReturnProperty property;

    // basic blocks or instructions (from the blocks) need to be executed for this property
    Set<List<BasicBlock>> basicBlockPaths;
    Set<List<SSAInstruction>> instructionPaths;

    Map<BasicBlock, CGNode> blockCGNodeMap = new HashMap<>();
    Map<SSAInstruction, BasicBlock> instructionBasicBlockMap = new HashMap<>();

    public CheckInvokeReturnProperty(SDG<InstanceKey> sdg, InvokeReturnProperty p) {
        this.sdg = sdg;
        this.property = p;
    }


    public boolean getResult() {
        // query 1
        System.out.println("SDG search:: source -> "+property.getFieldName());
        System.out.println("SDG search:: sink -> "+property.getInvokeMethodName());
        Set<List<Statement>> paths = new SDGSearchManager(sdg).searchPathsSDG(sourceField, sinkInvokeMethodCall);
        if (paths.size() == 0) {
            System.out.println("SDG search:: no path found!");
            return false;
        }
        for (List<Statement> path: paths) {
            BasicBlockUtil.printStatementPath(path);
            Set<BasicBlock> bbPath = BasicBlockUtil.getBasicBlocksFromPathOfStatements(path);
            List<SSAInstruction> instPath = BasicBlockUtil.getInstructionsPathFromBasicBlockPath(bbPath);
            BasicBlockUtil.printInstructionPaths(instPath);
        }

        //query 2
        System.out.println("SDG search:: source -> "+property.getInvokeMethodName());
        System.out.println("SDG search:: sink -> "+"conditional branch");
        paths = new SDGSearchManager(sdg).searchPathsSDG(sourceInvokeMethodReturn, sinkConditionalBranch);
        if (paths.size() == 0) {
            System.out.println();
            return false;
        }
        for (List<Statement> path: paths) {
            BasicBlockUtil.printStatementPath(path);
            Set<BasicBlock> bbPath = BasicBlockUtil.getBasicBlocksFromPathOfStatements(path);
            List<SSAInstruction> instPath = BasicBlockUtil.getInstructionsPathFromBasicBlockPath(bbPath);
            BasicBlockUtil.printInstructionPaths(instPath);
        }

        return true;
    }

//    public boolean getResult() {
//        basicBlockPaths = doSearch();
//
//        // no paths found. property instructions do not exist in the program.
//        if (basicBlockPaths.size() == 0) {
//            return false;
//        }
//        instructionPaths = getInstructionsPath(basicBlockPaths);
//        //printInstructionPaths();
//        System.out.println("bb paths: "+basicBlockPaths.size());
//
//        return false;
//
////        boolean isAccepted = isAccepted();
////
////        if (isAccepted) {
////            System.out.println("Accepted");
////            return true;
////        }else {
////            System.out.println("Not Accepted");
////            return false;
////        }
//    }

    private void printInstructionPaths() {
        assert instructionPaths != null: "Instruction paths null in CheckProperty.";
        for(List<SSAInstruction> instList: instructionPaths) {
            System.out.println("Path (Instruction): [ ");
            for(SSAInstruction inst: instList) {
                System.out.println("  -->"+inst.toString()+" Type: "+inst.getClass());
            }
            System.out.println("]");
        }
    }
    private void printStatementPath(List<Statement> path) {
        System.out.println("Path (Statements): [ ");
        for (Statement s: path) {
            if (s instanceof NormalStatement) {
                NormalStatement ns = (NormalStatement) s;
                System.out.println("STMT:: -> "+ns.getInstruction().toString());
            }
            else {
                System.out.println("STMT:: => "+s.toString());
            }
        }
        System.out.println("]");
    }

    private Set<List<BasicBlock>> doSearch() {
        Set<List<BasicBlock>> finalBlockPaths = HashSetFactory.make();

        // search step 1 (from field to invoke method call)
        System.out.println("SDG search:: source -> "+property.getFieldName());
        System.out.println("SDG search:: sink -> "+property.getInvokeMethodName());
        Set<List<BasicBlock>> bBlocksSearch1 = searchSDG(sdg, sourceField, sinkInvokeMethodCall);

        if (bBlocksSearch1.size() == 0) {
            System.out.println("SDG search returned no path!");
            return finalBlockPaths;
        }

        //search step 2 (from invokeMethodReturn to conditional branch)
        System.out.println("SDG search:: source -> "+property.getInvokeMethodName());
        System.out.println("SDG search:: sink -> "+"conditional branch");
        Set<List<BasicBlock>> bBlocksSearch2 = searchSDG(sdg, sourceInvokeMethodReturn, sinkConditionalBranch);

        if (bBlocksSearch2.size() == 0) {
            System.out.println("method was invoked for the field, but returned value didn't get to any conditional branch");
            return finalBlockPaths;
        }

        // at this point, there exist path(s) from both (field --> invoke method call)
        // and (invokeMethodReturn --> conditional branch).
        // So, merge the paths from search1 and search 2
        for (List<BasicBlock> blocks1: bBlocksSearch1) {
            for(List<BasicBlock> blocks2: bBlocksSearch2) {
                List<BasicBlock> mergedPath = new ArrayList<>();
                mergedPath.addAll(blocks1);
                mergedPath.addAll(blocks2);

                finalBlockPaths.add(mergedPath);
            }
        }
        return finalBlockPaths;
    }

    private Set<List<SSAInstruction>> getInstructionsPath(Set<List<BasicBlock>> basicBlockPaths) {
        Set<List<SSAInstruction>> instPaths = HashSetFactory.make();
        for(List<BasicBlock> blocks: basicBlockPaths) {
            List<SSAInstruction> instructions = new ArrayList<>();
            for(BasicBlock block: blocks) {

                for(SSAInstruction inst: block.getAllInstructions()) {
                    instructions.add(inst);
                    instructionBasicBlockMap.put(inst, block);
                }
            }
            instPaths.add(instructions);
        }
        return instPaths;
    }

    private EndpointFinder sourceField =
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

    public EndpointFinder sinkInvokeMethodCall = new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
            if (s.getKind()==Kind.PARAM_CALLER) {
                ParamCaller pCaller = (ParamCaller)s;
                SSAInstruction inst = pCaller.getInstruction();

                // get String value of dispatch instruction's first arg -> which contains invoke methodName
                String val0 = s.getNode().getIR().getSymbolTable().getValueString(inst.getUse(0));

                if (val0.endsWith(property.getInvokeMethodName())) {
                    return true;
                }
            }

            return false;
        }
    };

    public EndpointFinder sourceInvokeMethodReturn = new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
            if (s.getKind()==Kind.NORMAL_RET_CALLER) {

                NormalReturnCaller nretCaller = (NormalReturnCaller)s;
                SSAInstruction inst = nretCaller.getInstruction();

                // get String value of dispatch instruction's first arg -> which is methodName
                String val0 = s.getNode().getIR().getSymbolTable().getValueString(inst.getUse(0));

                if (val0.endsWith(property.getInvokeMethodName())) {
                    return true;
                }
            }

            return false;
        }
    };

    private EndpointFinder sinkConditionalBranch = new EndpointFinder() {
        @Override
        public boolean endpoint(Statement s) {
            // ctor (new) functions contain a conditional branch -> no use for this sink.
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

    // finds the basic blocks traversed during SDG search.
    private List<BasicBlock> getBasicBlocksFromStatementPath(List<Statement> path) {
        List<BasicBlock> bbList = new ArrayList<>();
        BasicBlock prevBlockinPath = null;

        for(Statement stmt: path) {
            SSACFG cfg = stmt.getNode().getIR().getControlFlowGraph();
            SSAInstruction inst = null;

            if (stmt instanceof NormalStatement) {
                inst = ((NormalStatement)stmt).getInstruction();
            }
            else if (stmt instanceof ParamCaller) {
                inst = ((ParamCaller)stmt).getInstruction();
            }
            else if (stmt instanceof NormalReturnCaller) {
                inst = ((NormalReturnCaller)stmt).getInstruction();
            } else {
                System.out.println("Statement is instance of :"+stmt.getClass()+":: Block not added to path.");
            }


            BasicBlock bb = stmt.getNode().getIR().getControlFlowGraph()
                    .getBlockForInstruction(inst.iIndex());
            // consequtive statements in the same block are added as one single block in the list.
            if (prevBlockinPath != null && bb.equals(prevBlockinPath)) {
                // block is already added to the path.
                continue;
            }
            bbList.add(bb);
            prevBlockinPath = bb;

            blockCGNodeMap.put(bb, stmt.getNode());

        }
        return bbList.size()!=0? bbList:null;
    }

    // returns the set of basic block paths found during SDG search from source -> sink
    private Set<List<BasicBlock>> searchSDG(SDG<? extends InstanceKey> G, EndpointFinder source, EndpointFinder sink) {
        Set<List<BasicBlock>> resultBlockPaths = HashSetFactory.make();

        for(Statement src : G) {
            if (source.endpoint(src)) {
                for(Statement dst : G) {
                    if (sink.endpoint(dst)) {
//                        System.out.println("src: "+src.toString());
//                        System.out.println("dest: "+dst.toString());
                        // Since SDG uses data dependance between statements, this search returns many redundant paths.
                        // So, instead of statements, we keep track of Basic Blocks during search in SDG.
                        Set<List<Statement>> paths = new SDGSearchManager(G).getPathsBacktracking(src, dst);

                        if (paths != null) {
                            //System.out.println("Statement path size: "+paths.size());
                            for(List<Statement> path: paths) {
                                printStatementPath(path);
                                List<BasicBlock> bbPath = getBasicBlocksFromStatementPath(path);
                                // add it to a 'set' to remove duplicates -> because of statements in the same block can have data dependency.
                                resultBlockPaths.add(bbPath);
                            }

                        }else {
                            //System.out.println("No path found");
                        }
                    }
                }
            }
        }
        return resultBlockPaths;
    }


}

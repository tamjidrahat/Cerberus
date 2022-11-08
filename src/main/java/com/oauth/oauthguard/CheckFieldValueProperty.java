package com.oauth.oauthguard;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.oauth.oauthguard.TaintAnalysis.EndpointFinder;
import com.oauth.property.FieldValueProperty;
import com.oauth.util.BasicBlockUtil;
import java.util.List;
import java.util.Set;

public class CheckFieldValueProperty {
    SDG<InstanceKey> sdgData;
    SDG<InstanceKey> sdgControl;
    FieldValueProperty property;

//    // basic blocks or instructions (from the blocks) need to be executed for this property
//    Set<List<BasicBlock>> basicBlockPaths;
//    Set<List<SSAInstruction>> instructionPaths;
//
//    Map<BasicBlock, CGNode> blockCGNodeMap = new HashMap<>();
//    Map<SSAInstruction, BasicBlock> instructionBasicBlockMap = new HashMap<>();



    CheckFieldValueProperty(SDG<InstanceKey> sdgData,SDG<InstanceKey> sdgControl, FieldValueProperty p) {
        this.sdgData = sdgData;
        this.sdgControl = sdgControl;
        this.property = p;
    }

    public boolean getResult() {
        if (property.getFields().size() == 1) {
            System.out.println("SDG search:: source -> "+property.getFields().get(0));
            System.out.println("SDG search:: sink -> "+"conditional branch");
            Set<List<Statement>> paths = new SDGSearchManager(sdgData)
                .searchPathsSDG(sourceField1, sinkConditionalBranch);

            return BasicBlockUtil.printPaths(paths);

        } else if(property.getFields().size() == 2) {
            System.out.println("SDG search:: source -> "+
                property.getFields().get(0)+"."+
                property.getFields().get(1));
            System.out.println("SDG search:: sink -> "+"conditional branch");

            Set<List<Statement>> paths = new SDGSearchManager(sdgControl)
                .searchPathsSDG(sourceField1, sourceField2);

            // if no path exists for field1, then ignore search for field2
            if (BasicBlockUtil.printPaths(paths) == false) {
                return false;
            }
            // use each dest statement from last query as src statement for next query
            for(List<Statement> path: Iterator2Iterable.make(paths.iterator())) {
                Statement sourceField2Stmt = path.get(path.size()-1);
                paths = new SDGSearchManager(sdgData)
                    .searchPathsSDG(sourceField2Stmt, sinkConditionalBranch);
                BasicBlockUtil.printPaths(paths);
            }
            return false;
        } else {
            System.out.println("ERROR:: Check number of fileds in CheckFieldValueProperty class!");
            return false;
        }
    }

//    public boolean getResult() {
//        basicBlockPaths = searchSDG(sdg, sourceField, sinkConditionalBranch);
//
//        // no paths found. property instructions do not exist in the program.
//        if (basicBlockPaths.size() == 0) {
//            return false;
//        }
//        instructionPaths = getInstructionsPath(basicBlockPaths);
//
//        boolean isAccepted = isAccepted();
//
//        if (isAccepted) {
//            System.out.println("Accepted");
//            return true;
//        }else {
//            System.out.println("Not Accepted");
//            return false;
//        }
//    }

//    private AutoState getNextAutoState(SSAInstruction inputInst, AutoState curState) {
//        boolean existDotEdge = false;
//        AutoState nextState = null;
//
//        for(AutoEdge edge: curState.getOutgoingStatesInvKeySet()) {
//            if(edge.isDotEdge()) {
//                existDotEdge = true;
//            }
//            else{
//                PropertyAutoEdge edgeID = (PropertyAutoEdge)edge.getId();
//                if (edgeID.instName == PropertyAutoEdge.TYPE_GETFIELD && inputInst instanceof SSAGetInstruction) {
//                    if (((SSAGetInstruction) inputInst)
//                        .getDeclaredField()
//                        .getName()
//                        .toString()
//                        .equals(edgeID.getField("fieldname"))) {
//                            nextState = curState.getOutgoingStatesInv().get(edge).iterator().next();
//                    }
//                }
//                else if (edgeID.instName == "binaryop" && inputInst instanceof SSABinaryOpInstruction) {
//                    SSABinaryOpInstruction instbinary = (SSABinaryOpInstruction) inputInst;
//                    //pass the symbol table to get the val2 (RHS contant of the binary op)
//                    String instVal2 = instbinary.getVal2String(blockCGNodeMap.get(instructionBasicBlockMap.get(inputInst)).getIR().getSymbolTable());
//
//                    // check if [instruction's operator == edge.operator] and [instruction's val2(constant) == edge.val2]
//                    // instructions val2 is returned as v10:#CONSTANT, so we use endswith.
//                    if (instbinary.getOperator().toString().equals(edgeID.getField("operator"))
//                        && instVal2.endsWith(edgeID.getUse("val2"))) {
//                        nextState = curState.getOutgoingStatesInv().get(edge).iterator().next();//each edge points to only one state
//                    }
//
//                }
//                else if (edgeID.instName == "conditional branch" && inputInst instanceof SSAConditionalBranchInstruction) {
//                    nextState = curState.getOutgoingStatesInv().get(edge).iterator().next();
//                }
//                else {
////                    System.out.println("Unrecognized edge: "+edgeID);
////                    System.out.println(inputInst);
//                }
//            }
//        }
//        if (nextState == null && existDotEdge == true) {
//            nextState = curState;
//        }
//        else if(nextState == null && existDotEdge == false) {
//            System.out.println("next state could not be determined. Check property automaton!");
//            System.out.println("Input inst: "+inputInst.toString());
//            System.out.println("Cur Automaton state: "+curState.toString());
//        }
//
//        return nextState;
//    }

    // checks if the automaton accepts any instruction path found during SDG search
//    public boolean isAccepted() {
//        RegAutomaton propAuto = buildPropertyAutomaton();
//        assert propAuto!=null: "Prop Automaton null";
//        assert basicBlockPaths!= null: "Basic Blocks path null";
//
//        for(List<BasicBlock> blockpath: basicBlockPaths){
//            AutoState curState = propAuto.getInitStates().iterator().next();// initial state
//
//            for(BasicBlock block: blockpath) {
//                for(SSAInstruction inst: block.getAllInstructions()) {
//                    curState = getNextAutoState(inst, curState);
//                }
//            }
//
//            if (curState != null && curState.isFinalState()) {
//                printInstructionPaths();
//                return true;
//            }
//        }
//        return false;
//    }

//    private RegAutomaton buildPropertyAutomaton() {
//        RegAutomaton propAuto = new RegAutomaton();
//
//        RegAutoState state_1 = new RegAutoState("1", true, false);
//        RegAutoState state_2 = new RegAutoState("2", false, false);
//        RegAutoState state_3 = new RegAutoState("3", false, false);
//        RegAutoState state_4 = new RegAutoState("4", false, true);
//
//        PropertyAutoEdge pe = new PropertyAutoEdge(PropertyAutoEdge.TYPE_GETFIELD);
//        pe.setField("fieldname", property.getFieldName());
//        AutoEdge edge_getfield = new AutoEdge(pe);
//
//        pe = new PropertyAutoEdge(PropertyAutoEdge.TYPE_BINARYOP);
//        pe.setField("operator", property.getOperator());
//        pe.setUse("val2", property.getConstVal());
//        AutoEdge edge_binaryop = new AutoEdge(pe);
//
//        pe = new PropertyAutoEdge(PropertyAutoEdge.TYPE_CONDITIONAL_BRANCH);
//        pe.setUse("val2", "0");
//        AutoEdge edge_conditional = new AutoEdge(pe);
//
//        AutoEdge edge_dot = new AutoEdge(new PropertyAutoEdge("."), true);
//
//
//        propAuto.addStates(state_1);
//        propAuto.addStates(state_2);
//        propAuto.addStates(state_3);
//        propAuto.addStates(state_4);
//
//        propAuto.addEdge(state_1, state_1, edge_dot);
//        propAuto.addEdge(state_1, state_2, edge_getfield);
//        propAuto.addEdge(state_2, state_2, edge_dot);
//        propAuto.addEdge(state_2, state_3, edge_binaryop);
//        propAuto.addEdge(state_3, state_3, edge_dot);
//        propAuto.addEdge(state_3, state_4, edge_conditional);
//
//        return propAuto;
//
//    }

//    public void printInstructionPaths() {
//        assert instructionPaths != null: "Instruction paths null in CheckProperty.";
//        for(List<SSAInstruction> instList: instructionPaths) {
//            System.out.println("Path (Instruction): [ ");
//            for(SSAInstruction inst: instList) {
//                System.out.println("  -->"+inst.toString()+" Type: "+inst.getClass());
//            }
//            System.out.println("]");
//        }
//    }

    private EndpointFinder sourceField1 =
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
                            .equals(property.getFields().get(0))) {
                            return true;
                        }
                    }
                }

                return false;
            }
        };

    private EndpointFinder sourceField2 =
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
                            .equals(property.getFields().get(1))) {
                            return true;
                        }
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

//    private Set<List<SSAInstruction>> getInstructionsPath(Set<List<BasicBlock>> basicBlockPaths) {
//        Set<List<SSAInstruction>> instPaths = HashSetFactory.make();
//        for(List<BasicBlock> blocks: basicBlockPaths) {
//            List<SSAInstruction> instructions = new ArrayList<>();
//            for(BasicBlock block: blocks) {
//
//                for(SSAInstruction inst: block.getAllInstructions()) {
//                    instructions.add(inst);
//                    instructionBasicBlockMap.put(inst, block);
//                }
//            }
//            instPaths.add(instructions);
//        }
//        return instPaths;
//    }


    // finds the basic blocks traversed during SDG search.
//    private List<BasicBlock> getBasicBlocksFromStatementPath(List<Statement> path) {
//        List<BasicBlock> bbList = new ArrayList<>();
//        BasicBlock prevBlockinPath = null;
//
//        for(Statement stmt: path) {
//            if (stmt instanceof NormalStatement) {
//                SSAInstruction inst = ((NormalStatement)stmt).getInstruction();
//                BasicBlock bb = stmt.getNode().getIR().getControlFlowGraph()
//                    .getBlockForInstruction(inst.iIndex());
//                // consequtive statements in the same block are added as one single block in the list.
//                if (prevBlockinPath != null && bb.equals(prevBlockinPath)) {
//                    continue;
//                }
//                bbList.add(bb);
//                prevBlockinPath = bb;
//
//                blockCGNodeMap.put(bb, stmt.getNode());
//
//            }
//        }
//        return bbList.size()!=0? bbList:null;
//    }

    // prints the statement paths found in SDG search from src to dest.
//    private void printStatementPath(List<Statement> path) {
//        System.out.println("Path (Instructions): [ ");
//        for (Statement s: path) {
//            if (s instanceof NormalStatement) {
//                NormalStatement ns = (NormalStatement) s;
//                System.out.println(" -> "+ns.getInstruction().toString());
//            }
//        }
//        System.out.println("]");
//    }
    // returns the set of basic block paths found during SDG search from source -> sink
//    private Set<List<BasicBlock>> searchSDG(SDG<? extends InstanceKey> G, EndpointFinder source, EndpointFinder sink) {
//        Set<List<BasicBlock>> resultBlockPaths = HashSetFactory.make();
//
//        for(Statement src : G) {
//            if (source.endpoint(src)) {
//                for(Statement dst : G) {
//                    if (sink.endpoint(dst)) {
////                        System.out.println("src: "+src.toString());
////                        System.out.println("dest: "+dst.toString());
//                        // Since SDG uses data dependance between statements, this search returns many redundant paths.
//                        // So, instead of statements, we keep track of Basic Blocks during search in SDG.
//                        Set<List<Statement>> paths = new SDGPathSearch(G).getPathsBacktracking(src, dst);
//
//                        if (paths != null) {
//                            //System.out.println("Statement path size: "+paths.size());
//                            for(List<Statement> path: paths) {
//                                //printStatementPath(path);
//                                List<BasicBlock> bbPath = getBasicBlocksFromStatementPath(path);
//                                // add it to a set to remove duplicates -> because of statements in the same block can have data dependency.
//                                resultBlockPaths.add(bbPath);
//                            }
//
//                        }else {
//                            //System.out.println("No path found");
//                        }
//                    }
//                }
//            }
//        }
//        return resultBlockPaths;
//    }
}

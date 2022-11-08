package com.oauth.util;

import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.PhiStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BasicBlockUtil {

    public static Set<BasicBlock> getBasicBlocksFromPathOfStatements(List<Statement> path) {
        Set<BasicBlock> bbSet = new LinkedHashSet<>();

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
                continue;
            }

            BasicBlock bb = stmt.getNode().getIR().getControlFlowGraph()
                .getBlockForInstruction(inst.iIndex());
            bbSet.add(bb);

        }
        return bbSet.size()!=0? bbSet:null;
    }


    public static List<SSAInstruction> getInstructionsPathFromBasicBlockPath(Set<BasicBlock> basicBlockPaths) {
        List<SSAInstruction> instPath = new LinkedList<>();

        for(BasicBlock block: basicBlockPaths) {
            for(SSAInstruction inst: block.getAllInstructions()) {
                instPath.add(inst);
            }
        }
        return instPath.size() != 0? instPath : null;
    }

    public static void printStatementPath(List<Statement> path) {
        System.out.println("Path (Statements): [ ");
        for (Statement s: path) {
            if (s instanceof NormalStatement) {
                NormalStatement ns = (NormalStatement) s;
                System.out.println("STMT::"+
                    getFunctionName(s.getNode().getContext().toString())+
                    " -> "+ns.getInstruction().toString());
            } else if ( s instanceof PhiStatement) {
                System.out.println("STMT:: -> "+((PhiStatement) s).getPhi().toString());
            }
            else {
                System.out.println("STMT:: => "+s.toString());
            }
        }
        System.out.println("]");
    }

    public static boolean printPaths(Set<List<Statement>> paths) {
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
        return true;
    }

    public static void printInstructionPaths(List<SSAInstruction> instPath) {
        System.out.println("Path (Instruction): [ ");
        for(SSAInstruction inst: instPath) {
            String className = inst.getClass().toString();
            System.out.println("INST:: ->"+inst.toString()+"(Type: "
                +className.substring(className.lastIndexOf('.')+1)+")");
        }
        System.out.println("]");
    }

    private static String getFunctionName(String context) {
        String result = "<No do()>";
        int max_length = 30;
        if (context.contains("do()")) {
            result = context.substring(context.lastIndexOf("do()")-max_length
                ,context.lastIndexOf("]"));
        }
        return result;
    }


}

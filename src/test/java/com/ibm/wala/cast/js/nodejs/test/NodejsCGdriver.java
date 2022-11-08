package com.ibm.wala.cast.js.nodejs.test;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.nodejs.NodejsCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.traverse.DFS;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Set;


public class NodejsCGdriver {

    public static void main(String[] args) throws Exception {

  //    File file = new File("../resources/NodejsRequireJsonTest/index.js");
        File file = new NodejsCGdriver().getFileFromResources();

        PropagationCallGraphBuilder builder = NodejsCallGraphBuilderUtil.makeCGBuilder(file);

        //print IR
        //printIRs(file);

        CallGraph CG = builder.makeCallGraph(builder.getOptions());
//        String cgString = CG.toString();
//        System.out.println(cgString);
        myDumpCG(CG);
        System.out.println(CallGraphStats.getStats(CG));

//        System.err.println("pointer analysis");
//        PointerAnalysis<InstanceKey> PA = builder.getPointerAnalysis();
//        for (PointerKey n : PA.getPointerKeys()) {
//            try {
//                System.out.println(("Pointer key: "+n + " --> " + PA.getPointsToSet(n)));
//                System.out.println();
//            } catch (Throwable e) {
//                System.err.println(("error computing set for " + n));
//            }
//        }
    }

    private static void myDumpCG(CallGraph CG) {


        // get successor nodes of fake node
//        CGNode rootnode = CG.getFakeRootNode();
//        for(CGNode succNode: Iterator2Iterable.make(CG.getSuccNodes(rootnode))) {
//            if (succNode.getMethod().getSignature().startsWith("prologue.js") ||
//                succNode.getMethod().getSignature().startsWith("extended-prologue.js")) {
//                System.out.println("remove edge: ");
//                System.out.println(rootnode.getGraphNodeId()+"=>"+succNode.getGraphNodeId()+": "
//                    +succNode.getMethod().getSignature());
//
//                CG.removeEdge(rootnode, succNode);
//                System.out.println(CG.getSuccNodeNumbers(rootnode));
//            }
//        }


        //get all reachable nodes from root node in callgraph
        Set<CGNode> reachableNodes =
            DFS.getReachableNodes(CG, Collections.singleton(CG.getFakeRootNode()));
        int nNodes = 0;
        int nEdges = 0;
        for (CGNode n : reachableNodes) {
            nNodes++;
            nEdges += CG.getSuccNodeCount(n);
            //printNode(CG, n);
            //printNodeParamInfo(CG, n);
            if (n.getGraphNodeId() == 3) {
                printCallsiteInfo(CG, n);
            }
//            if (n.getGraphNodeId() == 128 || n.getGraphNodeId() == 131 || n.getGraphNodeId() == 3) {
//                printCFGInfo(CG, n);
//            }
        }

//        for (CGNode N: CG) {
//            printNode(CG, N);
//        }
    }

    private File getFileFromResources() throws Exception {
        URL fileUrl = getClass().getClassLoader().getResource("NodejsRequireJsonTest/nodeserverDB.js");
        return new File(fileUrl.toURI());

    }



    public static void printIRs(File file) {
        String filename = file.getPath();

        JSCallGraphUtil.setTranslatorFactory(
        new CAstRhinoTranslatorFactory());
        // build a class hierarchy, for access to code info
        IClassHierarchy cha = null;
        try {
            cha = JSCallGraphUtil.makeHierarchyForScripts(filename);
        } catch (ClassHierarchyException e) {
            e.printStackTrace();
        }
        // for constructing IRs
        IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory(); for (IClass klass : cha) {
            // ignore models of built-in JavaScript methods
            if (!klass.getName().toString().startsWith("Lprologue.js")) { // get the IMethod representing the code (the ‘do’ method)
                IMethod m = klass.getMethod(AstMethodReference.fnSelector);
                if (m != null) {
                    IR ir = factory.makeIR(m, Everywhere.EVERYWHERE, new SSAOptions());
                    System.out.println(ir);
                }
        } }
    }

    public static void printCFGInfo(CallGraph CG, CGNode N) {
        System.out.println("node id: "+N.getGraphNodeId());
        System.out.println(N.getIR());
        //SSACFG cfg = N.getIR().getControlFlowGraph();
    }

    public static void printCallsiteInfo(CallGraph CG, CGNode N) {
        //System.out.println(N.getIR());
        for (CGNode n : Iterator2Iterable.make(CG.getSuccNodes(N))) {
            System.out.println(N.getGraphNodeId()+"--->"+n.getGraphNodeId());
            for (CallSiteReference callsite: Iterator2Iterable.make(CG.getPossibleSites(N, n))) {
                SSAAbstractInvokeInstruction instr = N.getIR().getCalls(callsite)[0];

                System.out.println(instr.toString());
//                N.getIR().getSymbolTable();
                System.out.println(instr.toString(N.getIR().getSymbolTable()));
        // get the params.
        // starts from 1 to instr.getNumberOfPositionalParameters()-1
                System.out.println("total params: "+instr.getNumberOfPositionalParameters());
                System.out.println("val 0: "+N.getIR().getSymbolTable().getValueString(instr.getUse(0)));
                if(1 < instr.getNumberOfPositionalParameters()) {
                    System.out.println("val 1: "+N.getIR().getSymbolTable().getValueString(instr.getUse(1)));
                }
                if(2 < instr.getNumberOfPositionalParameters()) {
                    System.out.println("val 2: "+N.getIR().getSymbolTable().getValueString(instr.getUse(2)));
                }
                // return value number
                //System.out.println(instr.getDef());

                // get all return values in the IR
//                for(SSAInstruction ssa: N.getIR().getInstructions()) {
//
//                    if (ssa != null && ssa.hasDef()) {
//                        //System.out.println(ssa.getClass());
//                        System.out.println(ssa.getDef());
//                    }
//                }
            }
        }
    }

    public static void printNodeParamInfo(CallGraph CG, CGNode N) {

        if (N.getGraphNodeId() != 3 && N.getGraphNodeId() != 128)
            return;
        System.out.println();
        IMethod m = N.getMethod();
        System.out.println(N.getIR());
    System.out.println();
    System.out.println();
        //System.out.println(m.getParameterType(0));
//        System.out.println(m.getParameterType(1));
//        System.out.println(m.getParameterType(2));
//        System.out.println(m.getParameterType(3));
    }
    public static void printNode(CallGraph CG,CGNode N) {


//      SSACFG cfg = N.getIR().getControlFlowGraph();
        IMethod method = N.getMethod();
        System.out.println("Node id: "+N.getGraphNodeId());
        System.out.println("Method Signature: "+ method.getSignature());
        System.out.println("Shortname: "+getShortName(N.getMethod()));
        //System.out.println("Descriptor: "+method.getDescriptor());
        //System.out.println("Name: "+ method.getName().toString());
        //System.out.println("Reference: "+ method.toString());
        //System.out.println("Parameters: "+method.getNumberOfParameters());
        System.out.println("Declaring class: "+ method.getDeclaringClass().getName().toString());
        //System.out.println(N.getGraphNodeId()+"["+N.getMethod().getSignature()+"]"+" -> "+CG.getSuccNodeNumbers(N).toString());
        System.out.println("Succ nodes count: "+ N.getGraphNodeId() +" -> "+ CG.getSuccNodeNumbers(N).toString());
        //System.out.println("Node IR: "+N.getIR());

//        for (IntIterator it = CG.getSuccNodeNumbers(N).intIterator(); it.hasNext(); ) {
//            int I = it.next();
//        }

        for(CallSiteReference callsite: Iterator2Iterable.make(N.iterateCallSites())) {
            System.out.println("------------"+callsite.getDeclaredTarget().toString());
        }

//        System.out.println("Callees of node " + method.getName() + " : [");
        boolean fst = true;
        for (CGNode n : Iterator2Iterable.make(CG.getSuccNodes(N))) {
//            if (fst) fst = false;
//            else System.out.print(", ");
            //System.out.print(n.getGraphNodeId());

//            for (CallSiteReference callsite: Iterator2Iterable.make(CG.getPossibleSites(N, n))) {
//                if (N.getGraphNodeId()==3 && n.getGraphNodeId()==128) {
//                    System.out.println(N.getMethod().getSignature()+"->"+n.getMethod().getSignature());
//                    System.out.println(""+N.getGraphNodeId()+"->"+n.getGraphNodeId()+
//                        ", callsite: "+N.getIR().getCalls(callsite)[0]+",,"+callsite);
//                }
//
//            }
        }
//        System.out.println("]");
        System.out.println();
    }

    public static String getShortName(IMethod method) {
        String origName = method.getName().toString();
        String result = origName;
        if (origName.equals("do") || origName.equals("ctor")) {
            result = method.getDeclaringClass().getName().toString();
            result = result.substring(result.lastIndexOf('/') + 1);
            if (origName.equals("ctor")) {
                if (result.equals("LFunction")) {
                    String s = method.toString();
                    if (s.indexOf('(') != -1) {
                        String functionName = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
                        functionName = functionName.substring(functionName.lastIndexOf('/') + 1);
                        result += ' ' + functionName;
                    }
                }
                result = "ctor of " + result;
            }
        }
        return result;
    }
}

package com.oauth.oauthguard;

import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.cast.js.nodejs.NodejsCallGraphBuilderUtil;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.viz.DotUtil;
import com.oauth.pdg.SDGPartial;
import com.oauth.util.CGNodeDecorator;
import com.oauth.util.CGPruning;
import com.oauth.util.SDGStatementDecorator;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CerberusDriverJS {
    String mainFile;
    CallGraph cg;
    PointerAnalysis<InstanceKey> pa;
    SDG<InstanceKey> sdgData;
    SDG<InstanceKey> sdgControl;

    public CerberusDriverJS(String mainfile) {
        this.mainFile = mainfile;
        init();
    }
    public CerberusDriverJS() {

    }

    private void init() {
        File mFile = new CerberusDriverJS().getFileFromResources(mainFile);
        try {
            PropagationCallGraphBuilder builder = NodejsCallGraphBuilderUtil.makeCGBuilder(mFile);
            cg = builder.makeCallGraph(builder.getOptions());
            pa = builder.getPointerAnalysis();

//            sdgData = new JsSDGBuilder(cg, pa).build(true, false);
//            sdgControl = new JsSDGBuilder(cg, pa).build(false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        long startTime = System.currentTimeMillis();

        //OauthguardDriverJS oAuthdriver = new OauthguardDriverJS("oauthjs/test/unit/server_test.js");
//        OauthguardDriverJS oAuthdriver = new OauthguardDriverJS("oauth2orize/test/server_test.js");
//        OauthguardDriverJS oAuthdriver = new OauthguardDriverJS("oauth-server/examples/server.js");
        //OauthguardDriverJS oAuthdriver = new OauthguardDriverJS("oauth20Provider/test/authorizationCode.js");

        CerberusDriverJS oAuthdriver = new CerberusDriverJS("AuthReqHandlerTest.js");
        //OauthguardDriverJS oAuthdriver = new OauthguardDriverJS("nodeserverDB.js");
//        OauthguardDriverJS oAuthdriver = new OauthguardDriverJS("nodeserver.js");
        //OauthguardDriverJS oAuthdriver = new OauthguardDriverJS("test.js");


        //oAuthdriver.doTaintAnalyses();
        oAuthdriver.tmpInspectCG();
        //oAuthdriver.doIntersect();
        //oAuthdriver.searchEntryPointCG("nodeserver.nodejsModule.moduleSource.processor.do()LRoot;");
        //oAuthdriver.searchEntryPointCG("oauthjs_lib_handlers_authorize__handler.nodejsModule.moduleSource.AuthorizeHandler.do()LRoot;");

        //oAuthdriver.viewCG();
        //oAuthdriver.viewSDG(oAuthdriver.sdgData);
        //oAuthdriver.inspectSDG(oAuthdriver.sdgData);

        //oAuthdriver.buildSDGPruned("oauthjs_lib_handlers_authorize__handler.nodejsModule.moduleSource.AuthorizeHandler.do()LRoot;");
        //oAuthdriver.buildSDG();

        oAuthdriver.doPropertyCheck(oAuthdriver.sdgData, oAuthdriver.sdgControl);

        //oAuthdriver.tmpInspectSDG(oAuthdriver.sdgData, oAuthdriver.sdgControl);
        //new SDGIntersectManager(oAuthdriver.cg, oAuthdriver.pa);

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Time::::: "+ TimeUnit.MILLISECONDS.toSeconds(duration));
    }

    private void doPropertyCheck(SDG<InstanceKey> sdgData, SDG<InstanceKey> sdgControl) {
        new PropertyCheckManager(sdgData, sdgControl);
        //new PropertyCheckManagerOauth20Provider(sdgData, sdgControl);
    }

    private Process viewCG() {
        if (cg == null) {
            System.out.println("CG is null. Cannot process CG viewer.");
        }
        try{
            System.out.println(DotUtil.dotOutput(cg, new CGNodeDecorator(), "CG"));
            return null;
        } catch (WalaException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Process viewSDG(SDG<InstanceKey> sdg) {
        if (sdg == null) {
            System.out.println("SDG is null. Cannot process SDG viewer.");
        }
        try{
            System.out.println(DotUtil.dotOutput(sdg, new SDGStatementDecorator(), "SDG"));
            return null;
        } catch (WalaException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void inspectSDG(SDG<InstanceKey> sdg) {
        // eagerConstruction in SDG is called whenever we iterate over the nodes
        for(Statement stmt: sdg) {
            System.out.println(stmt.getNode());
        }
    }

    private void tmpInspectSDGNode(SDG<InstanceKey> sdg, String pattern) {
        for(Statement stmt: sdg) {
            if (stmt.getNode().getMethod().getName().toString().equals("ctor")) {
                continue;
            }
            if (stmt.toString().contains(pattern)) {
                System.out.println("*-> Node StMt: "+stmt.toString());

                for(Statement child: Iterator2Iterable.make(sdg.getSuccNodes(stmt))) {
                    if (child instanceof NormalStatement) {
                        SSAInstruction inst = ((NormalStatement)child).getInstruction();

                        BasicBlock bb = child.getNode().getIR().getControlFlowGraph().getBlockForInstruction(inst.iIndex());
                        //System.out.println(bb.getGraphNodeId()+"::"+bb.getNumber());
                        System.out.println("-->child: "+((NormalStatement)child).getInstruction().toString());
                    }
                    else {
                        System.out.println("-->child: "+child.toString());
                    }
                }
                for(Statement pred: Iterator2Iterable.make(sdg.getPredNodes(stmt))) {
                    if (pred instanceof NormalStatement) {
                        SSAInstruction inst = ((NormalStatement)pred).getInstruction();

                        BasicBlock bb = pred.getNode().getIR().getControlFlowGraph().getBlockForInstruction(inst.iIndex());
                        //System.out.println(bb.getGraphNodeId()+"::"+bb.getNumber());
                        System.out.println("<-- predecessor: "+((NormalStatement)pred).getInstruction().toString());
                    }
                    else {
                        System.out.println("<-- predecessor-->"+pred.toString());
                    }
                }
                System.out.println();
                System.out.println();
            }


        }
    }

    //first look for entry point based on signature/signature suffix, then prune the CG
    private void searchEntryPointCG(String entryPointSignature) {
        assert cg != null: "Callgraph could not be built for "+mainFile;

        CGNode entryPoint = null;
        for (CGNode node: cg) {
            if (node.getMethod().getSignature().equals(entryPointSignature)) {
                entryPoint = node;
                //we found our entry point, so break here
                break;
            } else if(node.getMethod().getSignature().endsWith(entryPointSignature)) {
                entryPoint = node;
                //we found our entry point, so break here
                break;
            }
        }
        if(entryPoint == null) {
            System.out.println("NO ENTRY POINTS FOUND with name: "+entryPointSignature);
            return;
        }

        Set<CGNode> reachableNodes = DFS
            .getReachableNodes(cg, Collections.singleton(entryPoint));

        System.out.println("Size of Callgraph: "+ cg.getNumberOfNodes());
        System.out.println("Size of Reachable Nodes (CGPruning): "+ reachableNodes.size());

        SDGPartial<InstanceKey> sdgpartial = new SDGPartial<InstanceKey>(cg, pa, reachableNodes
            , new JavaScriptModRef<InstanceKey>()
            , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
            , ControlDependenceOptions.FULL);

        SDG<InstanceKey> sdg = new SDG<InstanceKey>(cg, pa
            , new JavaScriptModRef<InstanceKey>()
            , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
            , ControlDependenceOptions.FULL);

        System.out.println("SDG(partial) nodes: "+sdgpartial.getNumberOfNodes());
        System.out.println("SDG nodes: "+sdg.getNumberOfNodes());


    }

    private void buildSDG() {
        sdgData = new OAuthSDGBuilder(cg, pa).buildSDGForJS(true, false);
        sdgControl = new OAuthSDGBuilder(cg, pa).buildSDGForJS(false, true);
    }

    private void buildSDGPruned(String entryPointSignature) {
        CGNode entryPoint = null;
        for (CGNode node: cg) {
            if (node.getMethod().getSignature().equals(entryPointSignature)) {
                entryPoint = node;
                //we found our entry point, so break here
                break;
            } else if(node.getMethod().getSignature().endsWith(entryPointSignature)) {
                entryPoint = node;
                //we found our entry point, so break here
                break;
            }
        }
        if(entryPoint == null) {
            System.out.println("NO ENTRY POINTS FOUND with name: "+entryPointSignature);
            return;
        }

        Set<CGNode> reachableNodes = DFS
            .getReachableNodes(cg, Collections.singleton(entryPoint));

        System.out.println("Size of Callgraph: "+ cg.getNumberOfNodes());
        System.out.println("Size of Reachable Nodes (CGPruning): "+ reachableNodes.size());

        sdgData = new SDG<InstanceKey>(cg, pa, reachableNodes
            , new JavaScriptModRef<InstanceKey>()
            , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
            , ControlDependenceOptions.NONE
            , null);
        sdgControl = new SDG<InstanceKey>(cg, pa, reachableNodes
            , new JavaScriptModRef<InstanceKey>()
            , DataDependenceOptions.NONE
            , ControlDependenceOptions.FULL
            , null);
    }


    private void doIntersect() {
        assert cg != null: "Callgraph could not be built for "+mainFile;
        IntersectManager im = new IntersectManager(cg);
        im.buildIntersect();
        Set<CGNode> entryNodes = im.getEntryNodesInCG();
        Set<CGNode> endNodes = im.getEndNodesInCG();

        if (endNodes.size() > 0 && entryNodes.size() >0) {
            //just consider one entry and end point for now
            System.out.println("entry: "+entryNodes.iterator().next());
            System.out.println("end: "+endNodes.iterator().next());

            Set<CGNode> reachableNodes = new CGPruning(this.cg).findNodes(entryNodes.iterator().next()
                , endNodes.iterator().next());

            System.out.println("Size of Callgraph: "+DFS
                .getReachableNodes(cg, Collections.singleton(cg.getFakeRootNode())).size());
            System.out.println("Size of Reachable Nodes (entry:end): "+reachableNodes.size());
            System.out.println("Size of Reachable Nodes (entry: ): "+DFS
                .getReachableNodes(cg, Collections.singleton(entryNodes.iterator().next())).size());

            SDGPartial<InstanceKey> sdgpartial = new SDGPartial<InstanceKey>(cg, pa, reachableNodes
                , new JavaScriptModRef<InstanceKey>()
                , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
                , ControlDependenceOptions.FULL);

            SDG<InstanceKey> sdg = new SDG<InstanceKey>(cg, pa
                , new JavaScriptModRef<InstanceKey>()
                , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
                , ControlDependenceOptions.FULL);

            for(Statement stmt: sdgpartial) {
                System.out.println(stmt.getKind());
            }
            System.out.println("SDG(partial) nodes: "+sdgpartial.getNumberOfNodes());
            System.out.println("SDG nodes: "+sdg.getNumberOfNodes());
        }

    }

    private Collection<Statement> getBackwaredSliceOfStatement(Statement s) {
        try {
            return Slicer.computeBackwardSlice(s, cg, pa,
                DataDependenceOptions.NO_EXCEPTIONS,
                ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
        } catch (CancelException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void doTaintAnalyses(SDG<InstanceKey> sdg) {
        assert sdg != null: "SDG build returned null for "+mainFile;
        Set<List<Statement>> paths = TaintAnalysis.getPaths(sdg, TaintAnalysis.getFieldRedirectUriSource, TaintAnalysis.conditionalBranchSink);

//        for (List<Statement> path : paths) {
//            System.out.println("Path: ");
//            System.out.println(path);
//            System.out.println();
//
//            // this code performs backward slicing for the dest statement of the path
//            Statement dstStmt = path.get(1);
//            Collection<Statement> slice = getBackwaredSliceOfStatement(dstStmt);
//            System.out.println("Slice of: "+dstStmt);
//            for(Statement s: slice) {
//                if (s.getNode().getMethod().toString().equals(dstStmt.getNode().getMethod().toString())) {
//                    System.out.println(s+"\n");
//                }
//
//            }
//        }
    }

    private void tmpInspectSDG(SDG<InstanceKey> sdgData, SDG<InstanceKey> sdgControl) {
        HashMap<Statement, Set<Statement>> sdgDataSuccessors = new HashMap<>();
        HashMap<Statement, Set<Statement>> sdgControlSuccessors = new HashMap<>();

        for(Statement st: sdgData) {
            System.out.println(st);
            sdgDataSuccessors.put(st, Iterator2Collection.toSet(sdgData.getSuccNodes(st)));
        }

        for(Statement st: sdgControl) {
            sdgControlSuccessors.put(st, Iterator2Collection.toSet(sdgControl.getSuccNodes(st)));
        }

//        System.out.println(sdgDataSuccessors.keySet().size());
//        System.out.println(sdgControlSuccessors.keySet().size());

    }

    private void tmpInspectCG() {
        Set<CGNode> reachableNodes =
            DFS.getReachableNodes(cg, Collections.singleton(cg.getFakeRootNode()));
        for (CGNode n : reachableNodes) {
            //System.out.println("iindex=30 :"+n.getIR().getInstructions()[29]);
            IMethod method = n.getMethod();
            System.out.println("Node id: "+n.getGraphNodeId());
            System.out.println("Method Signature: "+ method.getSignature());
            System.out.println("Method Name: "+ method);
            System.out.println("Succ nodes count: "+ n.getGraphNodeId() +" -> "+ cg.getSuccNodeNumbers(n).toString());
            System.out.println("Node IR: "+n.getIR().getInstructions());
            System.out.println();
        }
    }

    private File getFileFromResources(String filename) {
        File file = null;
        try {
            URL fileUrl = getClass().getClassLoader().getResource(filename);
            file = new File(fileUrl.toURI());

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return file;
    }
}

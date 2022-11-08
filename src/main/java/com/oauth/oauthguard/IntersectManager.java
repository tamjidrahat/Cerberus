package com.oauth.oauthguard;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.rta.CallSite;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.traverse.DFS;
import com.oauth.automaton.AutoEdge;
import com.oauth.automaton.AutoState;
import com.oauth.automaton.CGAutoState;
import com.oauth.automaton.CGAutomaton;
import com.oauth.automaton.InterAutoOpts;
import com.oauth.automaton.InterAutomaton;
import com.oauth.automaton.RegAutoState;
import com.oauth.automaton.RegAutomaton;
import com.oauth.automaton.SrcTgtEdgeWrapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


// this class manages building:
// 1. CG-autonaton from WALA-callgraph
// 2. Reg-automaton from query
// 3. Intersection between CG-autonaton & Reg-automaton
public class IntersectManager {
    // callgraph of the input program
    CallGraph cg;

    CGNode rootNode;

    // set of all reachable nodes in the callgraph
    Set<CGNode> reachableNodes;

    private Set<IMethod> reachableMethSet = new HashSet<IMethod>();

    // automaton for call graph.
    CGAutomaton cgAuto;

    // automaton for regular expression.
    RegAutomaton regAuto;

    // Reg Auto edge representing the entry method (request method)
    AutoEdge entryEdge;

    // Reg Auto edge representing the end method (response method)
    AutoEdge endEdge;

    // automaton for intersect graph.
    InterAutomaton interAuto;

    // Original final state
    //private InterAutoState orgInterFinal;

    // each CGNode will be represented by the unicode of its number.
    public static Map<String, CGNode> uidToCGnodeMap = new HashMap<String, CGNode>();
    Map<CGNode, String> cgNodeToUidMap = new HashMap<CGNode, String>();

    Map<CGNode, CGAutoState> cgNodeToStateMap = new HashMap<CGNode, CGAutoState>();
    Map<CGAutoState, CGNode> stateToCGNodeMap = new HashMap<CGAutoState, CGNode>();

    Map<CallSite, AutoEdge> invkToEdgeMap = new HashMap<CallSite, AutoEdge>();
    Map<String, AutoEdge> signatureToEdgeMap = new HashMap<String, AutoEdge>();

    // (States from automaton.jar library to our RegAutomaton)
    //Map<State, RegAutoState> jsaToAutostate = new HashMap<State, RegAutoState>();

    private boolean debug = false;

    public IntersectManager(CallGraph cg) {
        this.cg = cg;
        this.rootNode = cg.getFakeRootNode();

        cgAuto = new CGAutomaton();
        regAuto = new RegAutomaton();
        buildMasterAndSlaveAutomaton();
    }

    // author: tamjid
    private void buildMasterAndSlaveAutomaton() {
        int offset = 100;

        assert rootNode != null : "root node null";

        // build state for each reachable nodes in call graph
        for(CGNode n: getReachableNodes()) {
            // for now, let's ignore unique id. rather use nodeID as uid
            //String uid = "\\u" + String.format("%04x", n.getGraphNodeId() + offset);
            String uid = String.valueOf(n.getGraphNodeId());
            uidToCGnodeMap.put(uid, n);
            cgNodeToUidMap.put(n, uid);

            CGAutoState st = null;
            // if node is fakerootnode -> make it initial state
            if (n.getGraphNodeId() == rootNode.getGraphNodeId()) {
                st = new CGAutoState(uid, true, false);
            } else {
                st = new CGAutoState(uid, false, true);
            }
            assert st != null: "st is null in IntersectManager.init()";
            st.setDesc(n.getMethod().getSignature());

            cgNodeToStateMap.put(n, st);
            stateToCGNodeMap.put(st, n);
        }
        //master
        buildSimulatedRegAutomaton();

        //slave
        buildCGAutomaton();

    }
    // author: tamjid
    private void buildCGAutomaton() {
        assert rootNode != null : "root node is null";

        // do DFS from rootnode and add states+edges to cgAuto for each mode
        Stack<CGNode> stack = new Stack<CGNode>();
        stack.push(rootNode);

        Set<CGAutoState> reachableStates = new HashSet<CGAutoState>();
        Set<CGNode> visited = new HashSet<CGNode>();

        while (!stack.empty()) {
            CGNode srcNode = stack.pop();

            if (visited.contains(srcNode))
                continue;
            visited.add(srcNode);

            CGAutoState srcState = cgNodeToStateMap.get(srcNode);
            reachableStates.add(srcState);

            // cgnode -> outgoing nodes (immediate successors)
            for (CGNode tgtNode : Iterator2Iterable.make(cg.getSuccNodes(srcNode))) {
                if (!reachableNodes.contains(tgtNode)) {
                    System.err.println("unreachable target Node: " + tgtNode.getMethod().getSignature());
                    continue;
                }
                // return the call sites in src node that might dispatch to the target node.
                // ISSUE: allow multiple callsites for a single target node?
                for (CallSiteReference refCallsite: Iterator2Iterable.make(cg.getPossibleSites(srcNode, tgtNode))) {

                    //AutoEdge outEdge = new AutoEdge(refCallsite);

                    // each edge is represented by the tgt node's signature
                    String tgtNodeSignature = tgtNode.getMethod().getSignature();

                    // make sure only one edge for each unique signature
                    if (signatureToEdgeMap.get(tgtNodeSignature) == null) {
                        AutoEdge edge = new AutoEdge(tgtNodeSignature);
                        signatureToEdgeMap.put(tgtNodeSignature, edge);
                    }
                    AutoEdge outEdge = signatureToEdgeMap.get(tgtNodeSignature);

                    if (tgtNode.equals(srcNode)) { //recursive call. add self loop
                        // add incoming-outgoing connection between same state (self loop)
                        cgAuto.addEdge(srcState, srcState, outEdge);
                        invkToEdgeMap.put(new CallSite(refCallsite, srcNode), outEdge);
                    }
                    else {
                        stack.push(tgtNode);
                        CGAutoState tgtState = cgNodeToStateMap.get(tgtNode);
                        assert tgtState != null: "tgt state null in Intersectmanager.buildcgautomaton";
                        // add incoming-outgoing connection between src and tgt state
                        cgAuto.addEdge(srcState, tgtState, outEdge);
                        invkToEdgeMap.put(new CallSite(refCallsite, srcNode), outEdge);
                    }
                }
            }
        }

        for (CGAutoState rs : reachableStates)
            cgAuto.addStates(rs);

        //cgAuto.validate();
        //cgAuto.dump();
    }

    // author: tamjid
    // this returns all reachable nodes of callgraph from the fake root node
    private Set<CGNode> getReachableNodes() {
        if (reachableNodes == null) {
            reachableNodes = DFS.getReachableNodes(cg, Collections.singleton(rootNode));
        }
        return this.reachableNodes;
    }

//    private void buildRegAutomaton(String regx) {
//        //regx = StringEscapeUtils.unescapeJava(regx);
//        // step 1. Constructing a reg without .*
//        RegExp r = new RegExp(regx);
//        Automaton auto = r.toAutomaton();
//        regAuto = new RegAutomaton();
//        // Set<RegAutoState> finiteStates = new HashSet<RegAutoState>();
//        Set<State> states = auto.getStates();
//        int number = 1;
//
//        for (State s : states) {
//            // Map<State, Edge>
//            // Map<AutoEdge, Set<AutoState>> incomeStates = new
//            // HashMap<AutoEdge, Set<AutoState>>();
//            RegAutoState mystate = new RegAutoState(number, false, false);
//
//            // mystate.setOutgoingStatesInv(incomeStates);
//
//            // use number to represent state id.
//            jsaToAutostate.put(s, mystate);
//            number++;
//        }
//
//        for (State s : states) {
//            RegAutoState fsmState = jsaToAutostate.get(s);
//            if (s.isAccept()) {
//                fsmState.setFinalState();
//                regAuto.addFinalState(fsmState);
//            }
//
//            if (s.equals(auto.getInitialState())) {
//                fsmState.setInitState();
//                regAuto.addInitState(fsmState);
//            }
//
//            for (Transition t : s.getTransitions()) {
//                RegAutoState tgtState = jsaToAutostate.get(t.getDest());
//                // Map tgtIncome = tgtState.getOutgoingStatesInv();
//                // using edge label as id.
//                String unicode = StringUtil.appendChar(t.getMin(),
//                    new StringBuilder(""));
//                String shortName = "undefined";
//                AutoEdge outEdge = new AutoEdge(unicode);
//
//                if (uidToCGnodeMap.get(unicode) != null) {
//                    shortName = uidToCGnodeMap.get(unicode).getMethod().getSignature();
//                } else if(t.getDest().equals(s)){
//                    outEdge.setDotEdge();
//                    shortName = ".";
//                }
//
//                outEdge.setShortName(shortName);
//                fsmState.addOutgoingStates(tgtState, outEdge);
//                tgtState.addIncomingStates(fsmState, outEdge);
//            }
//
//            regAuto.addStates(fsmState);
//        }
//        // dump current result.
//        // System.out.println("dump regular graph.");
//        // regAuto.dump();
//    }

    private void buildSimulatedRegAutomaton() {

        AutoEdge edge_req = new AutoEdge("nodeserver.nodejsModule.moduleSource.processor.do()LRoot;");
        AutoEdge edge_res = new AutoEdge("nodeserver.nodejsModule.moduleSource.Response.nodeserver@575.do()LRoot;");
        AutoEdge edge_dot = new AutoEdge(".", true);

        this.entryEdge = edge_req;
        this.endEdge = edge_res;

        RegAutoState state_1 = new RegAutoState("a", true, false);
        RegAutoState state_2 = new RegAutoState("b", false, false);
        RegAutoState state_3 = new RegAutoState("c", false, true);

        regAuto.addStates(state_1);
        regAuto.addStates(state_2);
        regAuto.addStates(state_3);

        regAuto.addEdge(state_1, state_1, edge_dot);
        regAuto.addEdge(state_1, state_2, edge_req);

        regAuto.addEdge(state_2, state_2, edge_dot);
        regAuto.addEdge(state_2, state_3, edge_res);



    }

    public void buildIntersect() {
        assert cgAuto!=null: "CG Automaton is null. Build Intersect failed";
        assert regAuto!=null: "Reg Automaton is null. Build Intersect failed";
        buildInterAutomaton(cgAuto, regAuto);
    }



    private void buildInterAutomaton(CGAutomaton cgAuto, RegAutomaton regAuto) {
        // options are unnecessary for now
        Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
        InterAutoOpts myopts = new InterAutoOpts(myoptions);
        interAuto = new InterAutomaton(myopts, regAuto, cgAuto);
        interAuto.build();

        System.out.println("Intersect Automaton finished");

        //interAuto.dump();
        System.out.println("Final states: "+interAuto.getFinalStates());

//        // find all non-dot edges of regAuto
//        List<AutoEdge> regAutoEdges = new LinkedList<>();
//
//        Stack<AutoState> stack = new Stack<>();
//        for(AutoState initstate: regAuto.getInitStates()) {
//            stack.push(initstate);
//        }
//        while (!stack.empty()) {
//            AutoState srcState = stack.pop();
//            Map<AutoState, Set<AutoEdge>> outgoingStates = srcState.getOutgoingStates();
//            for(AutoState tgtState: srcState.getOutgoingStatesKeySet()) {
//                //ignore self loop
//                if (!tgtState.equals(srcState)) {
//                    stack.push(tgtState);
//
//                    for (AutoEdge outEdge: outgoingStates.get(tgtState)) {
//                        if (!outEdge.isDotEdge()) {
//                            regAutoEdges.add(outEdge);
//                        }
//                    }
//                }
//
//            }
//        }
//
//        //assume we have two callsite at regAuto -> 1) request(entry) 2) response(end)
//        CGNode entryNode = null;
//        CGNode endNode = null;
//
//        boolean first = true;
//        for (AutoEdge regEdge: regAutoEdges) {
//            System.out.println("Request/Response Signature: "+ regEdge.getId());
//            Set<SrcTgtEdgeWrapper> interEntry = inter.lookupByMasterEdge(regEdge);
//            if (interEntry != null) {
//                for(SrcTgtEdgeWrapper entry: Iterator2Iterable.make(inter.lookupByMasterEdge(regEdge).iterator())) {
////                    System.out.println("src CGNode: "+stateToCGNodeMap.get(entry.getSrcState().getSlaveState()));
////                    System.out.println("tgt CGNode: "+stateToCGNodeMap.get(entry.getTgtState().getSlaveState()));
//                    if (first) {
//                        entryNode = stateToCGNodeMap.get(entry.getTgtState().getSlaveState());
//                        first = false;
//                    }
//                    else {
//                        endNode = stateToCGNodeMap.get(entry.getTgtState().getSlaveState());
//                    }
//
//                }
//            }
//
//        }
//
//        if (entryNode != null && endNode != null) {
//            System.out.println("entry Node: "+ entryNode);
//            System.out.println("end Node: "+ endNode);
//        }
//
//        Set<CGNode> reachableNodes = new CGPruning(this.cg).findNodes(entryNode, endNode);
//        System.out.println("Size of Callgraph: "+DFS
//            .getReachableNodes(cg, Collections.singleton(cg.getFakeRootNode())).size());
//        System.out.println("Size of Reachable Nodes (entry:end): "+reachableNodes.size());
//        System.out.println("Size of Reachable Nodes (entry: ): "+DFS
//            .getReachableNodes(cg, Collections.singleton(entryNode)).size());


//        for(AutoState initstate: regAuto.getInitStates()) {
//            for(AutoEdge edge: initstate.getOutgoingStatesInvKeySet()) {
//                if (!edge.isDotEdge()) {
//                    System.out.println("Master Edge: "+edge.getId());
//                    System.out.println("Inter Src Tgt Edge: ");
//                    System.out.println(inter.lookupByMasterEdge(edge));
//                }
//
//            }
//        }
    }

    public Set<AutoState> getInterectFinalStates() {
        return interAuto.getFinalStates();
    }

    public Set<CGNode> getEntryNodesInCG() {
        Set<CGNode> entryNodes = new HashSet<>();
        Set<SrcTgtEdgeWrapper> interEntrySet = interAuto.lookupByMasterEdge(entryEdge);
        if (interEntrySet != null) {
            for(SrcTgtEdgeWrapper entry: Iterator2Iterable.make(interEntrySet.iterator())) {
                entryNodes.add(stateToCGNodeMap.get(entry.getTgtState().getSlaveState()));
            }
        }
        return entryNodes;
    }

    public Set<CGNode> getEndNodesInCG() {
        Set<CGNode> endNodes = new HashSet<>();
        Set<SrcTgtEdgeWrapper> interEndSet = interAuto.lookupByMasterEdge(endEdge);
        if (interEndSet != null) {
            for(SrcTgtEdgeWrapper entry: Iterator2Iterable.make(interEndSet.iterator())) {
                endNodes.add(stateToCGNodeMap.get(entry.getTgtState().getSlaveState()));
            }
        }
        return endNodes;
    }

//

//    private boolean buildInterAutomaton(CGAutomaton cgAuto,
//        RegAutomaton regAuto, boolean annot, boolean stepTwo, boolean exhaust) {
//        Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
//        myoptions.put("annot", annot);
//        myoptions.put("two", stepTwo);
//        InterAutoOpts myopts = new InterAutoOpts(myoptions);
//
//        interAuto = new InterAutomaton(myopts, regAuto, cgAuto);
//        interAuto.build();
//        // interAuto.validate();
//        // interAuto.dumpFile();
//
//        // before we do the mincut, we need to exclude some trivial cases
//        // such as special invoke, static invoke and certain virtual invoke.
//        if (interAuto.getFinalStates().size() == 0) {
//            // eager version much also be false in this case.
//            return false;
//        }
//        orgInterFinal = (InterAutoState) interAuto.getFinalStates().iterator()
//            .next();
//        if (debug)
//            GraphUtil.checkValidInterAuto(interAuto);
//
//        // need to append a super final state, otherwise the result is wrong.
//        createSuperNode(interAuto);
//
//        boolean answer = true;
//        // exhaustive checking.
////        if (exhaust) {
////            answer = checkExhaust();
////        } else {
////            answer = checkByMincut();
////        }
//        return answer;
//    }

    // replace the final states with a super final states.
    private void createSuperNode(InterAutomaton auto) {
        CGAutoState superFinalSt = new CGAutoState("SuperFinal", false, true);
        superFinalSt.setDesc("Final");

        //from all current final states, add an edge to the super final state.
        for(AutoState autoSt : auto.getFinalStates()) {
            AutoEdge superEdge = new AutoEdge(autoSt.getId() + "@superFinal");
            superEdge.setInfinityWeight();

            autoSt.addOutgoingStates(superFinalSt, superEdge);

            // add incoming state.
            superFinalSt.addIncomingStates(autoSt, superEdge);
            autoSt.resetFinalState(); // autoSt is no longer final state
        }
        auto.clearFinalState();
        auto.addFinalState(superFinalSt);
        auto.addStates(superFinalSt);
    }

    // checking every edge along the path eagerly.
//    private boolean checkExhaust() {
//        long startExhau = System.nanoTime();
//        HashMap<AutoEdge, Boolean> edgeMap = new HashMap<AutoEdge, Boolean>();
//        // step 1: check all edges
//        LinkedList<AutoState> worklist = new LinkedList<AutoState>();
//        worklist.addAll(interAuto.getInitStates());
//        Set<AutoState> visited = new HashSet<AutoState>();
//        while (!worklist.isEmpty()) {
//            AutoState worker = worklist.pollFirst();
//            if (visited.contains(worker))
//                continue;
//
//            visited.add(worker);
//            worklist.addAll(worker.getOutgoingStatesKeySet());
//            if (interAuto.getInitStates().contains(worker))
//                continue;
//
//            for (AutoEdge e : worker.getOutgoingStatesInvKeySet()) {
//                // check each edge.
//                Set<AutoState> sts = worker.outgoingStatesInvLookup(e);
//                assert sts.size() == 1 : e;
//                AutoState tgt = sts.iterator().next();
//                if (tgt.isFinalState())
//                    continue;
//
//                CGNode callee = uidToCGnodeMap.get(((InterAutoEdge) e)
//                    .getTgtCGAutoStateId());
//                assert callee != null : tgt;
//                edgeMap.put(e, isValidEdge(e, worker));
//            }
//        }
//        // step 2: perform dfs
//        worklist.clear();
//        worklist.addAll(interAuto.getInitStates());
//        visited.clear();
//        while (!worklist.isEmpty()) {
//            AutoState worker = worklist.pollFirst();
//            if (visited.contains(worker))
//                continue;
//
//            visited.add(worker);
//            for (AutoEdge e : worker.getOutgoingStatesInvKeySet()) {
//                Set<AutoState> sts = worker.outgoingStatesInvLookup(e);
//                assert sts.size() == 1 : e;
//                AutoState tgt = sts.iterator().next();
//                if (!edgeMap.containsKey(e) || edgeMap.get(e)) {
//                    worklist.add(tgt);
//                }
//            }
//        }
//
//        visited.retainAll(interAuto.getFinalStates());
//        boolean answer = !visited.isEmpty();
//        long stopExhau = System.nanoTime();
//        //StringUtil.reportSec("exhau: " + answer, startExhau, stopExhau);
//        return answer;
//    }

    // checking validity using mincut.
//    private boolean checkByMincut() {
////        ptTime = 0;
////        cutTime = 0;
//        // Stop conditions:
//        // 1. Refute all edges in current cut set;(Yes)
//        // 2. Can not find a mincut without infinity anymore.(No)
//        Set<CutEntity> cutset = GraphUtil.minCut(interAuto);
//        // System.out.println("cutset:" + cutset);
//
//        boolean answer = true;
//
//        // try the final edge first?
//        boolean shortCut = true;
//        for (AutoEdge lastEdge : orgInterFinal.getIncomingStatesInvKeySet()) {
//            if (lastEdge.isInvEdge())
//                continue;
//            AutoState inState = orgInterFinal.incomingStatesInvLookup(lastEdge)
//                .iterator().next();
//            if (isValidEdge(lastEdge, inState)) {
//                shortCut = false;
//                lastEdge.setInfinityWeight();
//                break;
//            }
//        }
//
//        // contains infinity edge?
//        if (!shortCut) {
//            while (!hasInfinityEdges(cutset)) {
//                boolean refuteAll = true;
//
//                for (CutEntity e : cutset) {
//                    if (isValidEdge(e.edge, e.getSrc())) {
//                        refuteAll = false;
//                        e.edge.setInfinityWeight();
//                        break;
//                    } else {
//                        // TODO:e is a false positive.
//                    }
//                }
//                // all edges are refute, stop.
//                if (refuteAll) {
//                    answer = false;
//                    break;
//                }
//                // modify visited edges and continue.
//                cutset = GraphUtil.minCut(interAuto);
//                // System.out.println("cutset:" + cutset);
//            }
//        } else {
//            answer = false;
//        }
////        StringUtil.reportDiff("Time on PT: ", ptTime);
////        StringUtil.reportDiff("Cut time:" + answer, cutTime);
//        return answer;
//    }

//    private boolean isValidEdge(AutoEdge e, AutoState src) {
//        assert !e.isInvEdge();
//        long start = System.nanoTime();
//        CallSiteReference st = e.getSrcStmt();
//        if (st == null)
//            return true;
//        CGNode calleeNode = uidToCGnodeMap.get(((InterAutoEdge) e)
//            .getTgtCGAutoStateId());
//        String calleeSig = calleeNode.getMethod().getSignature();
//        IClass calleeClz = calleeNode.getMethod().getDeclaringClass();
//        CGNode caller = uidToCGnodeMap.get(((InterAutoState) src)
//            .getSlaveState().getId());
//
//        Set<AutoEdge> inEdges = src.getIncomingStatesInvKeySet();
//        assert (calleeMeth != null);
//        // main method is always reachable.
//        if (calleeMeth.isMain() || calleeMeth.isStatic()
//            || calleeMeth.isPrivate() || calleeMeth.isPhantom())
//            return true;
//
//        List<Type> typeSet = SootUtils
//            .compatibleTypeList(calleeClz, calleeMeth);
//        if (st.getInvokeExpr() instanceof SpecialInvokeExpr) {
//            // handle super.foo();
//            for (SootClass sub : SootUtils.subTypesOf(calleeClz)) {
//                typeSet.add(sub.getType());
//            }
//        }
//
//        Set<Type> ptTypeSet = new HashSet<Type>();
//        assert st != null : calleeMeth;
//        Local l = getReceiver(st);
//        // get the context of l. This could be optimized later.
//        for (AutoEdge in : inEdges) {
//            if (in.isInvEdge())
//                continue;
//            Stmt stack = in.getSrcStmt();
//
//            if (stack != null
//                && stack.containsInvokeExpr()
//                && ((stack.getInvokeExpr() instanceof VirtualInvokeExpr) || (stack
//                .getInvokeExpr() instanceof InterfaceInvokeExpr))) {
//                CgContext ctxt = new CgContext(stack.getInvokeExpr());
//                Set<Type> types = ptsDemand.reachingObjects(ctxt, l)
//                    .possibleTypes();
//                if ((SootUtils.isObserver(calleeSig)) && caller != null
//                    && caller.getName().startsWith("fire")) {
//                    Set<SootClass> subObs = SootUtils.subTypesOf(caller
//                        .getDeclaringClass());
//                    Set<Type> valids = new HashSet<Type>();
//                    for (SootClass subO : subObs) {
//                        Set<Type> mapTypes = ptsDemand.resolveObservers().get(
//                            subO.getType());
//                        if (mapTypes == null)
//                            continue;
//                        valids.addAll(mapTypes);
//
//                    }
//                    // System.out.println("actual: " + types);
//                    // System.out.println("valids: " + valids);
//                    types.retainAll(valids);
//                    // System.out.println("retain: " + types);
//                }
//
//                // System.out.println("stmt: " + st + " ctxt: " + ctxt + " var:"
//                // + l + " types:" + types);
//                ptTypeSet.addAll(types);
//            } else {
//                // Since we limit k=1, if the context is static, we ignore
//                ptTypeSet.addAll(ptsDemand.reachingObjects(l).possibleTypes());
//            }
//        }
//
//
//
//        long end = System.nanoTime();
//        ptTime = ptTime + (end - start);
//
//        if (ptTypeSet.size() == 0)
//            return false;
//
//        // super.<init> always true;
//        if (calleeMeth.isConstructor())
//            return true;
//
//        if (includeAnyType && hasAnyType(ptTypeSet))
//            return true;
//
//        ptTypeSet.retainAll(typeSet);
//        return !ptTypeSet.isEmpty();
//    }


}

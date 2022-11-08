package com.oauth.oauthguard;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.traverse.DFS;
import com.oauth.automaton.AutoEdge;
import com.oauth.automaton.CGAutoState;
import com.oauth.automaton.CGAutomaton;
import com.oauth.oauthguard.TaintAnalysis.EndpointFinder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class SDGIntersectManager {
    CallGraph cg;
    PointerAnalysis<InstanceKey> pa;

    SDG<InstanceKey> sdgData;
    SDG<InstanceKey> sdgControl;

    HashMap<Statement, Set<Statement>> sdgDataSuccessors = new HashMap<>();
    HashMap<Statement, Set<Statement>> sdgControlSuccessors = new HashMap<>();

    Map<Statement, CGAutoState> sdgStmtToStateMap = new HashMap<Statement, CGAutoState>();
    Map<CGAutoState, Statement> sdgStateToStmtMap = new HashMap<CGAutoState, Statement>();

    Set<Statement> reachableStatements;

    public SDGIntersectManager(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
        this.cg = cg;
        this.pa = pa;

        init();
    }

    private void init() {
        sdgData = new OAuthSDGBuilder(cg, pa).buildSDGForJS(true, false);
        sdgControl = new OAuthSDGBuilder(cg, pa).buildSDGForJS(false, true);

        for(Statement st: sdgData) {
            sdgDataSuccessors.put(st, Iterator2Collection.toSet(sdgData.getSuccNodes(st)));
        }

        for(Statement st: sdgControl) {
            sdgControlSuccessors.put(st, Iterator2Collection.toSet(sdgControl.getSuccNodes(st)));
        }
        System.out.println(sdgDataSuccessors.keySet().size());
        System.out.println(sdgControlSuccessors.keySet().size());

        //testing
        Statement root = getRootStatement(sdgData);
        if (root != null) {
            buildSDGAutomaton(sdgData, root);
        }

    }

    EndpointFinder rootFinder =
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

    private Statement getRootStatement(SDG<InstanceKey> sdg) {

        for(Statement src : sdg) {
            if (rootFinder.endpoint(src)) {
                return src;
            }
        }
        // root was not found
        return null;
    }

    private void buildSDGAutomaton(SDG<InstanceKey> sdg, Statement root) {
        assert root != null: "Root statement cannot be null";
        CGAutomaton SDGAutomaton = new CGAutomaton();

        for(Statement stmt: getReachableStatements(sdg, root)) {
            // each state is identified by a statement object (id)
            CGAutoState state = null;
            if (stmt.equals(root)) {
                state = new CGAutoState(root, true, false);
            } else {
                state = new CGAutoState(stmt, false, true);
            }
            assert state != null: "State st is null in SDGIntersectManager::buildSDGAutomaton";
            sdgStmtToStateMap.put(stmt, state);
            sdgStateToStmtMap.put(state, stmt);

            SDGAutomaton.addStates(state);
        }

        Stack<Statement> stack = new Stack<>();
        stack.push(root);

        Set<CGAutoState> reachableStates = new HashSet<CGAutoState>();
        Set<Statement> visited = new HashSet<>();

        while (!stack.isEmpty()) {
            Statement srcNode = stack.pop();
            if (visited.contains(srcNode))
                continue;
            visited.add(srcNode);

            System.out.println("traverse: "+srcNode);

            CGAutoState srcState = sdgStmtToStateMap.get(srcNode);
            reachableStates.add(srcState);

            for(Statement tgtNode: Iterator2Iterable.make(sdg.getSuccNodes(srcNode))) {
                if (!reachableStatements.contains(tgtNode)) {
                    System.err.println("unreachable target Stmt: " + tgtNode);
                }
                // each edge is identified by the target statement
                AutoEdge outEdge = new AutoEdge(tgtNode);

                stack.push(tgtNode);
                CGAutoState tgtState = sdgStmtToStateMap.get(tgtNode);
                assert tgtState != null: "tgtState is null in in SDGIntersectManager::buildSDGAutomaton";

                SDGAutomaton.addEdge(srcState, tgtState, outEdge);
            }
        }

        System.out.println("Rechable states: "+reachableStates.size());
        System.out.println("Reachable statements: "+reachableStatements.size());
    }

    public Set<Statement> getReachableStatements(SDG<InstanceKey> sdg, Statement rootStmt) {
        if (reachableStatements == null) {
            reachableStatements = DFS.getReachableNodes(sdg, Collections.singleton(rootStmt));
        }
        return this.reachableStatements;
    }
}

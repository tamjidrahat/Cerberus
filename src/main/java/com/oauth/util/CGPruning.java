package com.oauth.util;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CGPruning {

    private Set<CGNode> visited;
    private CallGraph cg;

    public CGPruning(CallGraph cg) {
        this.cg = cg;
    }

    public Set<CGNode> findNodes(final CGNode entryPoint, final CGNode endPoint) {
        this.visited = new HashSet<>();

        dfs(entryPoint, endPoint);

        return visited;
    }

    public Set<CGNode> findNodes(final CGNode entryPoint) {
        this.visited = new HashSet<>();

        dfs(entryPoint);

        return visited;
    }

    //search nodes between entry and end
    private void dfs(CGNode entry, CGNode end) {
        visited.add(entry);

        // do not traverse beyond the end node
        if(entry.equals(end)) {
            return;
        }

        Iterator<CGNode> it = cg.getSuccNodes(entry);

        while (it.hasNext()) {
            CGNode next = it.next();
            if (!visited.contains(next)) {
                dfs(next, end);
            }
        }
    }

    private void dfs(CGNode entry) {
        visited.add(entry);

        Iterator<CGNode> it = cg.getSuccNodes(entry);

        while (it.hasNext()) {
            CGNode next = it.next();
            if (!visited.contains(next)) {
                dfs(next);
            }
        }
    }




}

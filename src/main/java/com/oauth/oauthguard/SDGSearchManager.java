package com.oauth.oauthguard;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.oauth.oauthguard.TaintAnalysis.EndpointFinder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SDGSearchManager {
    SDG<? extends InstanceKey> sdg;

    Set<List<Statement>> paths;

    private boolean DEBUG = false;

    public SDGSearchManager(SDG<? extends InstanceKey> G) {
        this.sdg = G;
        paths = HashSetFactory.make();
    }

    public Set<List<Statement>> searchPathsSDG(Statement srcStmt, EndpointFinder sink) {
        Set<List<Statement>> resultPaths = HashSetFactory.make();
        for(Statement dest : this.sdg) {
            if (sink.endpoint(dest)) {
//                System.out.println("src: "+srcStmt.toString());
//                System.out.println("dest: "+dest.toString());

                Set<List<Statement>> paths = getPathsBacktracking(srcStmt, dest);

                if (paths != null) {
                    resultPaths.addAll(paths);
                }
            }
        }
        return resultPaths;
    }

    public Set<List<Statement>> searchPathsSDG(EndpointFinder source, EndpointFinder sink) {
        Set<List<Statement>> resultPaths = HashSetFactory.make();

        for(Statement src : this.sdg) {
            if (source.endpoint(src)) {
                for(Statement dest : this.sdg) {
                    if (sink.endpoint(dest)) {
//                        System.out.println("src: "+src.toString());
//                        System.out.println("dest: "+dest.toString());
                        // Since SDG uses data dependance between statements, this search returns many redundant paths.
                        // So, instead of statements, we keep track of Basic Blocks during search in SDG.
                        Set<List<Statement>> paths = getPathsBacktracking(src, dest);

                        if (paths != null) {
                            //System.out.println("Statement path size: "+paths.size());
//                            for(List<Statement> path: paths) {
//                                printStatementPath(path);
//                                List<BasicBlock> bbPath = getBasicBlocksFromStatementPath(path);
//                                // add it to a set to remove duplicates -> because of statements in the same block can have data dependency.
//                                resultBlockPaths.add(bbPath);
//                            }
                            resultPaths.addAll(paths);

                        }else {
                            //System.out.println("No path found");
                        }
                    }
                }
            }
        }
        return resultPaths;
    }



    public Set<List<Statement>> getPathsBacktracking(Statement src, Statement tgt) {
        HashMap<Statement, Boolean> visited = HashMapFactory.make();
        List<Statement> currentPath = new ArrayList<Statement>();
        currentPath.add(src);
        find(src, tgt, visited, currentPath);

        return paths.size() == 0? null: paths;
    }


    // this is the DP (faster) version of the iterative find() method below. "Visited" also keeps track if a node can lead to destionation.
    private boolean find(Statement src, Statement tgt, HashMap<Statement, Boolean> visited, List<Statement> currentPath) {
//        if (src instanceof NormalStatement) {
//            System.out.println("-->"+((NormalStatement)src).getInstruction().toString());
//        }
//        else {
//            System.out.println("-->"+src.toString());
//        }
//        System.out.println(sdg.getSuccNodeCount(src));

        visited.put(src, false); // true -> dest is reachable through this node. false -> dest not reachable

        if (src.equals(tgt)) {
            List<Statement> copy = new ArrayList<>();
            copy.addAll(currentPath);
            this.paths.add(copy);
            visited.put(tgt, true);
            //System.out.println("Reached PATH");
            return true;
        } else {
            boolean isDestReachable = false;
            for(Statement child: Iterator2Iterable.make(sdg.getSuccNodes(src))) {
                if (child.getNode().getMethod().getName().toString().equals("ctor")) {
                    continue;
                }
                if (DEBUG == true) {
                    System.out.println("Child:: "+child);
                }
                if (visited.get(child) == null || visited.get(child) == true) { // if not visited or leads to destination (true means dest can be reached from this node)

                    currentPath.add(child);
                    isDestReachable |= find(child, tgt, visited, currentPath);
                    currentPath.remove(child);
                }
            }
            visited.put(src, isDestReachable);
            return isDestReachable;
        }

    }

}


// BFS will give us one path (if exists). We prefer to use backtracking to get all possible paths
//    public Set<List<Statement>> getPathsBFS(Statement src, Statement tgt) {
//        Set<List<Statement>> resultPaths = new HashSet<List<Statement>>();
//
//        Queue<List<Statement>> q = new LinkedList<>();
//
//        List<Statement> initPath = new ArrayList<>();
//        initPath.add(src);
//        q.add(initPath);
//
//        while (!q.isEmpty()) {
//            List<Statement> path = q.poll();
//            Statement lastNode = path.get(path.size()-1);
//
//            if (lastNode.equals(tgt)) {
//                resultPaths.add(path);
//            } else {
//                for(Statement child: Iterator2Iterable.make(sdg.getSuccNodes(lastNode))) {
//                    if (!path.contains(child)) {
//                        List<Statement> newPath = new ArrayList<>();
//                        newPath.addAll(path); // add existing nodes of this path
//                        newPath.add(child); // add new child node
//
//                        q.add(newPath);
//                    }
//                }
//            }
//        }
//        return resultPaths;
//    }


// recursive search function for getPathsBacktracking
//    private void find(Statement src, Statement tgt, HashMap<Statement, Boolean> visited, List<Statement> currentPath) {
//        visited.put(src, true);
//
//        if (src.equals(tgt)) {
//            List<Statement> copy = new ArrayList<>();
//            copy.addAll(currentPath);
//            this.paths.add(copy);
//            return;
//        } else {
//            for(Statement child: Iterator2Iterable.make(sdg.getSuccNodes(src))) {
////                System.out.println("child: "+child);
//                if (visited.get(child) == null || visited.get(child)==false) {
//                    currentPath.add(child);
//                    find(child, tgt, visited, currentPath);
//                    currentPath.remove(child);
//                }
//            }
//        }
//        visited.put(src, false);
//    }

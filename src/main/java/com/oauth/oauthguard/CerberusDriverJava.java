package com.oauth.oauthguard;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.io.FileProvider;
import com.oauth.property.FieldValueProperty;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CerberusDriverJava {

    String jarFilePath;
    String entrypoint;

    CallGraph cg;
    PointerAnalysis<InstanceKey> pa;
    SDG<InstanceKey> sdgData;
    SDG<InstanceKey> sdgControl;

    public CerberusDriverJava(String jarFilePath, String entrypoint) {
        this.jarFilePath = jarFilePath;
        this.entrypoint = entrypoint;

        init(jarFilePath, entrypoint);
    }
    //init builds cg and pa
    private void init(String jarFilePath, String entrypoint) {
        try {
            //**input jar files**
            File jarFile = new FileProvider().getFile(jarFilePath);

            File exFile = new FileProvider().getFile("Java60RegressionExclusions.txt");

            AnalysisScope scope = AnalysisScopeReader
                .makeJavaBinaryAnalysisScope(jarFile.getAbsolutePath(),exFile);

            IClassHierarchy cha = ClassHierarchyFactory.make(scope);

            // **entry points**
            Iterable<Entrypoint> entrypoints =
                com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
                    scope, cha, entrypoint);

            AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

            // CFA callgraph
            CallGraphBuilder<InstanceKey> callGraphBuilder = CFACallGraphBuilder(options, cha, scope);

            // RTA callgraph
            //CallGraphBuilder<InstanceKey> callGraphBuilder = RTACallGraphBuilder(options, cha, scope);

            cg = callGraphBuilder.makeCallGraph(options, null);
            pa = callGraphBuilder.getPointerAnalysis();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

  public static void main(String[] args) {
        //Here: change jar and entrypoint for different libraries

      //lib: spring-auth-server
//      String jarFilePath = "JavaLibraries/spring-auth-server/spring-authorization-server-many.jar";
//      String entrypoint1 = "Lorg/springframework/security/oauth2/server/authorization/authentication/ManyCalls";
//      String entrypoint2 = "Lorg/springframework/security/oauth2/server/authorization/authentication/OAuthEndpointCalls";
//      String oauthEntrypoint = "OAuth2AuthorizationEndpointFilter";

      //lib: oauth2-server
//      String jarFilePath = "JavaLibraries/oauth2-server/oauth2-example-app.jar";
//      String entrypoint = "Lcom/clouway/oauth2/app/AuthBootstrap";
//      String oauthEntrypoint = "ClientAuthorizationActivity";

      //lib: apifest-oauth20
//      String jarFilePath = "JavaLibraries/apifest-oauth20/apifest-oauth20.jar";
//      String entrypoint = "Lcom/apifest/oauth20/OAuthEndpointCalls";
//      String oauthEntrypoint = "issueAuthorizationCode";

      //lib: jobmission/oauth2-server
//      String jarFilePath = "JavaLibraries/jobmission-oauth2-server/oauth2-server.jar";
//      String entrypoint = "Lcom/revengemission/sso/oauth2/server/Oauth2ServerApplication";
//      String oauthEntrypoint = "AuthorizationCodeTokenGranter";

      //lib: glufederation/oxauth
      String jarFilePath = "JavaLibraries/oxauth/oxauth-server.jar";
      String entrypoint = "Lorg/gluu/oxauth/token/ws/rs/OAuthMain";
      String oauthEntrypoint = "requestAccessToken";

      long startTime = System.currentTimeMillis();

      CerberusDriverJava oauthGuard = new CerberusDriverJava(jarFilePath, entrypoint);
      oauthGuard.buildSDG();
      //oauthGuard.tmpInspectCG();
      //oauthGuard.searchEntryPointCG("OAuth2AuthorizationEndpointFilter");
        oauthGuard.testCheckProperty();
      //oauthGuard.runOnFullCallgraph();
      //oauthGuard.runOnPrunedCallgraph(oauthEntrypoint);

      long duration = System.currentTimeMillis() - startTime;
      System.out.println("Time::::: "+ TimeUnit.MILLISECONDS.toSeconds(duration));
  }

    private void tmpInspectCG() {
        Set<MethodReference> allMethods = CallGraphStats.collectMethods(cg);
      System.out.println("Methods reached: " + allMethods.size());
      System.out.println(CallGraphStats.getStats(cg));

      Set<CGNode> reachableNodes =
            DFS.getReachableNodes(cg, Collections.singleton(cg.getFakeRootNode()));
        System.out.println("Reachable Nodes Size (DFS): "+reachableNodes.size());

        for (CGNode n : reachableNodes) {
            IMethod method = n.getMethod();
            System.out.println("Node id: "+n.getGraphNodeId());
            System.out.println("Method Signature: "+ method.getSignature());
            System.out.println("Method Name: "+ method);
            System.out.println("Succ nodes count: "+ n.getGraphNodeId() +" -> "+ cg.getSuccNodeNumbers(n).toString());
            System.out.println("Node IR: "+n.getIR());
            System.out.println();
        }
    }

    private void runOnFullCallgraph() {
        SDG<InstanceKey> sdg = new SDG<InstanceKey>(cg, pa
            , new ModRef<>()
            , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
            , ControlDependenceOptions.FULL
            ,null);

        new CheckFieldValueProperty(sdg, null, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("redirect_uri")),"eq", "http://"))
            .getResult();

    }

    private void testCheckProperty() {
        new CheckTestProperty(sdgData, sdgControl).getResult();
    }

    private void runOnPrunedCallgraph(String entryPointSignature) {
        CGNode entryPoint = null;
        for (CGNode node: cg) {
            if (node.getMethod().getSignature().equals(entryPointSignature)) {
                entryPoint = node;
                //we found our entry point, so break here
                break;
            } else if(node.getMethod().getSignature().contains(entryPointSignature)) {
                entryPoint = node;
                //we found our entry point, so break here
                break;
            }
        }
        if(entryPoint == null) {
            System.err.println("NO ENTRY POINTS FOUND with name: "+entryPointSignature);
            return;
        }
        Set<CGNode> reachableNodes = DFS
            .getReachableNodes(cg, Collections.singleton(entryPoint));

        SDG<InstanceKey> sdgpartial = new SDG<InstanceKey>(cg, pa, reachableNodes
            , new ModRef<>()
            , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
            , ControlDependenceOptions.FULL
            , null);
        new CheckFieldValueProperty(sdgpartial, null, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("redirect_uri")),"eq", "http://"))
            .getResult();
    }

    //first look for entry point based on signature/signature suffix, then prune the CG
    private void searchEntryPointCG(String entryPointSignature) {
        assert cg != null: "Callgraph could not be built for "+jarFilePath;

        CGNode entryPoint = null;
        for (CGNode node: cg) {
            if (node.getMethod().getSignature().equals(entryPointSignature)) {
                entryPoint = node;
                //we found our entry point, so break here
                break;
            } else if(node.getMethod().getSignature().contains(entryPointSignature)) {
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

        SDG<InstanceKey> sdgpartial = new SDG<InstanceKey>(cg, pa, reachableNodes
            , new ModRef<>()
            , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
            , ControlDependenceOptions.FULL
            , null);

        SDG<InstanceKey> sdg = new SDG<InstanceKey>(cg, pa
            , new ModRef<>()
            , DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
            , ControlDependenceOptions.FULL
            ,null);

        System.out.println("SDG(partial) nodes: "+sdgpartial.getNumberOfNodes());
        System.out.println("SDG nodes: "+sdg.getNumberOfNodes());

        long startTime = System.currentTimeMillis();
        new CheckFieldValueProperty(sdg, null, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("redirect_uri")),"eq", "http://"))
            .getResult();
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Time(full SDG)::::: "+ TimeUnit.MILLISECONDS.toSeconds(duration));

        startTime = System.currentTimeMillis();
        new CheckFieldValueProperty(sdgpartial, null, new FieldValueProperty
            (new LinkedList<String>(Arrays.asList("redirect_uri")),"eq", "http://"))
            .getResult();
        duration = System.currentTimeMillis() - startTime;
        System.out.println("Time(partial SDG)::::: "+TimeUnit.MILLISECONDS.toSeconds(duration));

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

    private CallGraphBuilder<InstanceKey> CFACallGraphBuilder(AnalysisOptions options
        , IClassHierarchy cha, AnalysisScope scope) {
        return Util.makeZeroOneCFABuilder(Language.JAVA
            , options
            , new AnalysisCacheImpl()
            , cha
            , scope);
    }
    private CallGraphBuilder<InstanceKey> RTACallGraphBuilder(AnalysisOptions options
        , IClassHierarchy cha, AnalysisScope scope) {
        return Util.makeRTABuilder(options, new AnalysisCacheImpl(), cha, scope);
    }

    private void buildSDG() {
        sdgData = new OAuthSDGBuilder(cg, pa).buildSDGForJava(true, false);
        sdgControl = new OAuthSDGBuilder(cg, pa).buildSDGForJava(false, true);
    }

//  private static void testCG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
//      File jarFile = new FileProvider().getFile("JavaLibraries/spring-authorization-server-oauth.jar");
//      File exFile = new FileProvider().getFile("Java60RegressionExclusions.txt");
////      System.out.println(exFile.getAbsolutePath());
//      AnalysisScope scope = AnalysisScopeReader
//          .makeJavaBinaryAnalysisScope(jarFile.getAbsolutePath(),exFile);
//      IClassHierarchy cha = ClassHierarchyFactory.make(scope);
//
////      Iterable<Entrypoint> entrypoints =
////          com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
////              scope, cha, "Lorg/springframework/security/oauth2/server/authorization/authentication/MyTest");
//
//      Iterable<Entrypoint> entrypoints =
//          com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
//              scope, cha, "Lorg/springframework/security/oauth2/server/authorization/authentication/OAuthEndpointCalls");
//
//      AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
//
//      //doCallGraphs(options, new AnalysisCacheImpl(), cha, scope, useShortProfile());
//
//      //CallGraph cg = CallGraphTestUtil.buildRTA(options, new AnalysisCacheImpl(), cha, scope);
//
//      CallGraphBuilder<InstanceKey> builderCFA01 =
//          Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
//
//      CallGraphBuilder<InstanceKey> builderRTA =
//          Util.makeRTABuilder(options, new AnalysisCacheImpl(), cha, scope);
//
//      //CallGraph cg = builderRTA.makeCallGraph(options, null);
//      CallGraph cg = builderCFA01.makeCallGraph(options, null);
//
//      PointerAnalysis<InstanceKey> pa = builderRTA.getPointerAnalysis();
//
//      SDG<InstanceKey> sdg = new SDG<>(cg, pa, DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS
//          , ControlDependenceOptions.FULL);
//
////    try {
////      GraphIntegrity.check(cg);
////    } catch (UnsoundGraphException e1) {
////      e1.printStackTrace();
////      Assert.assertTrue(e1.getMessage(), false);
////    }
//
//      Set<MethodReference> rtaMethods = CallGraphStats.collectMethods(cg);
//      System.err.println("RTA methods reached: " + rtaMethods.size());
//      System.err.println(CallGraphStats.getStats(cg));
//      System.err.println("RTA warnings:\n");
//
//
////      options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
////      IAnalysisCacheView cache = new AnalysisCacheImpl(options.getSSAOptions());
//
//      for(CGNode node: cg) {
//
//          IMethod m = node.getMethod();
//
//          if (m.getSignature().contains("org.springframework.security.oauth2.server.authorization")) {
//              System.out.println("=============");
//              IR ir = node.getIR();
//              //IR ir = cache.getIR(m, Everywhere.EVERYWHERE);
//              System.out.println(ir);
//          }
//          else if(m.getSignature().contains("org.springframework.security.oauth2.core.endpoint")) {
//              System.out.println("======++++=======");
//              IR ir = node.getIR();
//              //IR ir = cache.getIR(m, Everywhere.EVERYWHERE);
//              System.out.println(ir);
//          }
//
//      }
//
////      for(Statement stmt: sdg) {
////          System.out.println(stmt);
////      }
//
//
//
//
////    for (IClass c : cha) {
////        String cname = c.getName().toString();
////        System.out.println("Class:" + cname);
////
////        for (IMethod m : c.getAllMethods()) {
////            String mname = m.getName().toString();
////            System.out.println("  method:" + mname);
////        }
////        System.out.println();
////    }
//  }
}

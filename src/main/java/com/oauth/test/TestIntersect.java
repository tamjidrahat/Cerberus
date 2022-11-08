package com.oauth.test;

import com.oauth.automaton.AutoEdge;
import com.oauth.automaton.AutoState;
import com.oauth.automaton.CGAutoState;
import com.oauth.automaton.CGAutomaton;
import com.oauth.automaton.InterAutoOpts;
import com.oauth.automaton.InterAutomaton;
import com.oauth.automaton.RegAutoState;
import com.oauth.automaton.RegAutomaton;
import java.util.HashMap;
import java.util.Map;

public class TestIntersect {
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        RegAutomaton reg = mytestReg3();
        CGAutomaton call = mytestCG3();

        reg.dump();
        call.dump();

        Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
//		myoptions.put("annot", true);
//		 myoptions.put("two", true);
//		myoptions.put("one", true);
        InterAutoOpts myopts = new InterAutoOpts(myoptions);
        InterAutomaton inter = new InterAutomaton(myopts, reg, call);
        inter.build();



        inter.dump();
        System.out.println(inter.getFinalStates());
        System.out.println("Finished");

        for(AutoState initstate: reg.getInitStates()) {
            for(AutoEdge edge: initstate.getOutgoingStatesInvKeySet()) {
                System.out.println("Master Edge: "+edge.getId());
                System.out.println("Inter Src Tgt Edge: ");
                System.out.println(inter.lookupByMasterEdge(edge));
            }
        }
    }


    public static RegAutomaton mytestReg3() {
        RegAutomaton expr = new RegAutomaton();


        AutoEdge edge_req = new AutoEdge("Req()");
        AutoEdge edge_res = new AutoEdge("Res()");
        AutoEdge edge_dot = new AutoEdge(".", true);


        RegAutoState state_1 = new RegAutoState(1, true, false);
        RegAutoState state_2 = new RegAutoState(2, false, false);
        RegAutoState state_3 = new RegAutoState(3, false, true);

        expr.addStates(state_1);
        expr.addStates(state_2);
        expr.addStates(state_3);

        expr.addEdge(state_1, state_1, edge_dot);
        expr.addEdge(state_1, state_2, edge_req);

        expr.addEdge(state_2, state_2, edge_dot);
        expr.addEdge(state_2, state_3, edge_res);

        return expr;
    }

    public static CGAutomaton mytestCG3() {
        CGAutomaton call = new CGAutomaton();

        /** construct a simulated call graph fsm */
        AutoEdge edge_a = new AutoEdge("a");
        AutoEdge edge_b = new AutoEdge("b");
        AutoEdge edge_req = new AutoEdge("Req()");
        AutoEdge edge_res = new AutoEdge("Res()");

        CGAutoState state_4 = new CGAutoState(4, true, false);
        CGAutoState state_5 = new CGAutoState(5, false, true);
        CGAutoState state_6 = new CGAutoState(6, false, true);
        CGAutoState state_7 = new CGAutoState(7, false, true);
        CGAutoState state_8 = new CGAutoState(8, false, true);
        CGAutoState state_9 = new CGAutoState(9, false, true);

        call.addStates(state_4);
        call.addStates(state_5);
        call.addStates(state_6);
        call.addStates(state_7);
        call.addStates(state_8);
        call.addStates(state_9);


        call.addEdge(state_4, state_5, edge_a);
        call.addEdge(state_5, state_6, edge_req);
        call.addEdge(state_6, state_7, edge_b);
        call.addEdge(state_7, state_8, edge_res);
        call.addEdge(state_8, state_9, edge_a);

        return call;
    }
}

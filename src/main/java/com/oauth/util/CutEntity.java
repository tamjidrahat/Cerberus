package com.oauth.util;


import com.ibm.wala.classLoader.CallSiteReference;
import com.oauth.automaton.AutoEdge;
import com.oauth.automaton.AutoState;

/**
 * wrapper class for AutoState and AutoEdge
 * @author yufeng
 *
 */
public class CutEntity {
	
	public AutoState state;
	public AutoEdge edge;
	public AutoState endState;
	protected CallSiteReference srcStmt;


	
	public CutEntity(AutoState state, AutoEdge edge, AutoState endState) {
		this.state = state;
		this.edge = edge;
		this.endState = endState;
		srcStmt = edge.getSrcStmt();
	}
	
	public AutoState getSrc() {
		return state;
	}
	
	public AutoState getTgt() {
		return endState;
	}
	
	public CallSiteReference getStmt() {
		return srcStmt;
	}
	
	@Override
	public String toString() {
		return state + "->(" + srcStmt + ")" + endState;
	}

}

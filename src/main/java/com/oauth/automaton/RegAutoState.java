package com.oauth.automaton;

import java.util.*;

public class RegAutoState extends AutoState {

	protected Map<AutoState, Set<AutoEdge>> incomingStates = new HashMap<AutoState, Set<AutoEdge>>();

	protected Map<AutoEdge, Set<AutoState>> incomingStatesInv = new HashMap<AutoEdge, Set<AutoState>>();

	public RegAutoState(Object id, boolean isInitState, boolean isFinalState) {
		super(id, isInitState, isFinalState);
	}

	public RegAutoState(Object id) {
		super(id, false, false);
	}

	@Override
	public Set<AutoState> getIncomingStatesKeySet() {
		return incomingStates.keySet();
	}

	@Override
	public Set<AutoEdge> getIncomingStatesInvKeySet() {
		return incomingStatesInv.keySet();
	}

	// @Override
	public void setIncomingStates(Map<AutoState, Set<AutoEdge>> incomingStates) {
		this.incomingStates = incomingStates;
	}

	/*
	 * @Override public void setIncomingStates(Set<AutoState> incomingStates) {
	 * }
	 */

	// @Override
	public void setIncomingStatesInv(
			Map<AutoEdge, Set<AutoState>> incomingStatesInv) {
		this.incomingStatesInv = incomingStatesInv;
	}

	/*
	 * @Override public void setIncomingStatesInv(AutoEdge incomingEdge) { }
	 */

	@Override
	public Iterator<AutoState> incomingStatesIterator() {
		return incomingStates.keySet().iterator();
	}

	@Override
	public Iterator<AutoEdge> incomingStatesInvIterator() {
		return incomingStatesInv.keySet().iterator();
	}

	// @Override
	public Map<AutoState, Set<AutoEdge>> getIncomingStates() {
		return incomingStates;
	}

	// @Override
	public Map<AutoEdge, Set<AutoState>> getIncomingStatesInv() {
		return incomingStatesInv;
	}

	/*
	 * @Override public AutoEdge getIncomingEdge() { return null; }
	 */

	@Override
	public Set<AutoEdge> incomingStatesLookup(AutoState state) {
		return incomingStates.get(state);
	}

	@Override
	public Set<AutoState> incomingStatesInvLookup(AutoEdge edge) {
		return incomingStatesInv.get(edge);
	}

	@Override
	public boolean addIncomingStates(AutoState state, AutoEdge edge) {
		return addToMap(incomingStates, state, edge)
				| addToInvMap(incomingStatesInv, edge, state);
	}

	@Override
	public boolean deleteOneIncomingState(AutoState state) {
		Set<AutoEdge> edgeList = new HashSet<AutoEdge>();
		if (incomingStates.containsKey(state))
			edgeList.addAll(incomingStates.get(state));
		else
			return false;
		boolean succ = false;
		for (AutoEdge edge : edgeList) {
			succ = succ | deleteFromMap(incomingStates, state, edge)
					| deleteFromMapInv(incomingStatesInv, state, edge);
		}
		return succ;
	}
	
	@Override
	public boolean deleteOneIncomingEdge(AutoEdge edge) {
		Set<AutoState> stateList = new HashSet<AutoState>();
		if (incomingStatesInv.containsKey(edge))
			stateList.addAll(incomingStatesInv.get(edge));
		else
			return false;
		boolean succ = false;
		for (AutoState state : stateList) {
			succ = succ | deleteFromMap(incomingStates, state, edge)
					| deleteFromMapInv(incomingStatesInv, state, edge);
		}
		return succ;
	}

	@Override
	public boolean deleteOneIncomingEdge(AutoState state, AutoEdge edge) {
		return deleteFromMap(incomingStates, state, edge)
				| deleteFromMapInv(incomingStatesInv, state, edge);
	}
	
	@Override
	public boolean isIsolated() {
		return incomingStates.isEmpty() && incomingStatesInv.isEmpty()
				&& outgoingStates.isEmpty() && outgoingStatesInv.isEmpty();
	}
	
	public boolean hasOutgoingDotEdge() {
		for (AutoEdge e : outgoingStatesInv.keySet()) {
			if (e.isDotEdge())
				return true;
		}
		return false;
	}

	public boolean hasOnlyOneDotOutgoingEdge() {
		if (outgoingStatesInv.keySet().size() != 1) {
			return false;
		}
		for (AutoEdge e : outgoingStatesInv.keySet()) {
			if (!e.isDotEdge())
				return false;
		}
		return true;
	}

	@Override
	public boolean hasCycleEdge() {
		return incomingStates != null && incomingStates.keySet().contains(this);
	}

}

package com.oauth.util;

import com.oauth.automaton.Automaton;
import com.oauth.automaton.AutoState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class SCCBuilder {
	protected final List<Set<Object>> componentList = new ArrayList<Set<Object>>();

	protected int index = 0;

	protected Map<Object, Integer> indexForNode, lowlinkForNode;

	protected Stack<Object> s;

	protected Automaton g;

	/**
	 * @param g: a automaton for which we want to compute the strongly connected
	 * components.
	 */
	public SCCBuilder(Automaton g, Set roots) {
		this.g = g;
		s = new Stack<Object>();
		Set<Object> heads = roots;

		indexForNode = new HashMap<Object, Integer>();
		lowlinkForNode = new HashMap<Object, Integer>();

		for (Iterator<Object> headsIt = heads.iterator(); headsIt.hasNext();) {
			Object head = headsIt.next();
			if (!indexForNode.containsKey(head)) {
				recurse((AutoState) head);
			}
		}

		// free memory
		indexForNode = null;
		lowlinkForNode = null;
		s = null;
		g = null;
	}

	protected void recurse(AutoState v) {
		indexForNode.put(v, index);
		lowlinkForNode.put(v, index);
		index++;
		s.push(v);

		for (AutoState succ : v.getOutgoingStatesKeySet()) {
			if (!indexForNode.containsKey(succ)) {
				recurse(succ);
				lowlinkForNode.put(
						v,
						Math.min(lowlinkForNode.get(v),
								lowlinkForNode.get(succ)));
			} else if (s.contains(succ)) {
				lowlinkForNode
						.put(v,
								Math.min(lowlinkForNode.get(v),
										indexForNode.get(succ)));
			}
		}
		if (lowlinkForNode.get(v).intValue() == indexForNode.get(v).intValue()) {
			Set<Object> scc = new HashSet<Object>();
			Object v2;
			do {
				v2 = s.pop();
				scc.add(v2);
			} while (v != v2);
			componentList.add(scc);
		}
	}

	/**
	 * @return the list of the strongly-connected components
	 */
	public List<Set<Object>> getComponents() {
		return componentList;
	}
}

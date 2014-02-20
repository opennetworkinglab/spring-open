package net.onrc.onos.intent.runtime;

import java.util.HashMap;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.onrc.onos.intent.ConstrainedBFSTree;
import net.onrc.onos.intent.ConstrainedShortestPathIntent;
import net.onrc.onos.intent.ErrorIntent;
import net.onrc.onos.intent.ErrorIntent.ErrorType;
import net.onrc.onos.intent.Intent;
import net.onrc.onos.intent.IntentOperation;
import net.onrc.onos.intent.IntentOperation.Operator;
import net.onrc.onos.intent.IntentOperationList;
import net.onrc.onos.intent.PathIntent;
import net.onrc.onos.intent.PathIntentMap;
import net.onrc.onos.intent.ShortestPathIntent;
import net.onrc.onos.ofcontroller.networkgraph.NetworkGraph;
import net.onrc.onos.ofcontroller.networkgraph.Path;
import net.onrc.onos.ofcontroller.networkgraph.Switch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Toshio Koide (t-koide@onlab.us)
 */
public class PathCalcRuntime implements IFloodlightService {
	private NetworkGraph graph;
	private final static Logger log = LoggerFactory.getLogger(PathCalcRuntime.class);
	public PathCalcRuntime(NetworkGraph g) {
		this.graph = g;
	}

	/**
	 * calculate shortest-path and constrained-shortest-path intents into low-level path intents
	 * @param intentOpList IntentOperationList having instances of ShortestPathIntent/ConstrainedShortestPathIntent
	 * @param pathIntents a set of current low-level intents
	 * @return IntentOperationList. PathIntent and/or ErrorIntent instances.
	 */
	public IntentOperationList calcPathIntents(final IntentOperationList intentOpList, final PathIntentMap pathIntents) {
		IntentOperationList pathIntentOpList = new IntentOperationList();
		HashMap<Switch, ConstrainedBFSTree> spfTrees = new HashMap<>();

		for (IntentOperation intentOp: intentOpList) {
			switch (intentOp.operator) {
			case ADD:
				if (!(intentOp.intent instanceof ShortestPathIntent)) {
					log.error("Unsupported intent type: {}", intentOp.intent.getClass().getName());
					pathIntentOpList.add(Operator.ERROR, new ErrorIntent(
							ErrorType.UNSUPPORTED_INTENT,
							"Unsupported intent type.",
							intentOp.intent));
					continue;
				}

				ShortestPathIntent spIntent = (ShortestPathIntent) intentOp.intent;
				Switch srcSwitch = graph.getSwitch(spIntent.getSrcSwitchDpid());
				Switch dstSwitch = graph.getSwitch(spIntent.getDstSwitchDpid());
				if (srcSwitch == null || dstSwitch == null) {
					log.error("Switch not found: {}, {}",
							spIntent.getSrcSwitchDpid(),
							spIntent.getDstSwitchDpid());
					pathIntentOpList.add(Operator.ERROR, new ErrorIntent(
							ErrorType.SWITCH_NOT_FOUND,
							"Switch not found.",
							intentOp.intent));
					continue;
				}

				double bandwidth = 0.0;
				ConstrainedBFSTree tree = null;
				if (intentOp.intent instanceof ConstrainedShortestPathIntent) {
					bandwidth = ((ConstrainedShortestPathIntent) intentOp.intent).getBandwidth();
					tree = new ConstrainedBFSTree(srcSwitch, pathIntents, bandwidth);
				}
				else {
					tree = spfTrees.get(srcSwitch);
					if (tree == null) {
						tree = new ConstrainedBFSTree(srcSwitch);
						spfTrees.put(srcSwitch, tree);
					}
				}
				Path path = tree.getPath(dstSwitch);
				if (path == null) {
					log.error("Path not found: {}", intentOp.intent.toString());
					pathIntentOpList.add(Operator.ERROR, new ErrorIntent(
							ErrorType.PATH_NOT_FOUND,
							"Path not found.",
							intentOp.intent));
					continue;
				}
				PathIntent pathIntent = new PathIntent("pi" + intentOp.intent.getId(), path, bandwidth, intentOp.intent);
				pathIntentOpList.add(Operator.ADD, pathIntent);
				break;
			case REMOVE:
				pathIntentOpList.add(Operator.REMOVE, new Intent("pi" + intentOp.intent.getId()));
				break;
			case ERROR:
				// just ignore
				break;
			}
		}
		return pathIntentOpList;
	}
}
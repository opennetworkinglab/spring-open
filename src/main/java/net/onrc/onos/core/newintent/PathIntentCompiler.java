package net.onrc.onos.core.newintent;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import net.onrc.onos.api.flowmanager.FlowId;
import net.onrc.onos.api.flowmanager.FlowLink;
import net.onrc.onos.api.flowmanager.PacketPathFlow;
import net.onrc.onos.api.flowmanager.Path;
import net.onrc.onos.api.newintent.Intent;
import net.onrc.onos.api.newintent.IntentId;
import net.onrc.onos.api.newintent.PathIntent;
import net.onrc.onos.core.matchaction.match.Match;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.IdGenerator;
import net.onrc.onos.core.util.LinkTuple;

import java.util.Arrays;
import java.util.List;

/**
 * An intent compiler for {@link PathIntent}.
 */
public class PathIntentCompiler
        extends AbstractFlowGeneratingIntentCompiler<PathIntent> {

    /**
     * Construct an {@link net.onrc.onos.api.newintent.IntentCompiler}
     * for {@link PathIntent}.
     *
     * @param intentIdGenerator intent ID generator
     * @param flowIdGenerator flow ID generator
     */
    public PathIntentCompiler(IdGenerator<IntentId> intentIdGenerator,
                              IdGenerator<FlowId> flowIdGenerator) {
        super(intentIdGenerator, flowIdGenerator);
    }

    @Override
    public List<Intent> compile(PathIntent intent) {
        Match match = intent.getMatch();
        if (!(match instanceof PacketMatch)) {
            throw new IntentCompilationException(
                    "intent has unsupported type of match object: " + match
            );
        }

        Path path = convertPath(intent.getPath());
        PacketPathFlow flow = new PacketPathFlow(
                getNextFlowId(),
                (PacketMatch) match,
                intent.getIngressPort().getPortNumber(),
                path,
                packActions(intent, intent.getEgressPort()),
                0, 0
        );
        Intent compiled = new PathFlowIntent(getNextId(), flow);
        return Arrays.asList(compiled);
    }

    /**
     * Converts list of {@link LinkTuple LinkTuples} to a {@link Path}.
     *
     * @param tuples original list of {@link LinkTuple LinkTuples}
     * @return converted {@link Path}
     */
    private Path convertPath(List<LinkTuple> tuples) {
        // would like to use filter and transform, but Findbugs detects
        // inconsistency of use of @Nullable annotation. Then, use of the
        // transform is avoided.
        // Ref: https://code.google.com/p/guava-libraries/issues/detail?id=1812
        // TODO: replace with transform when the above issue is resolved
        ImmutableList<LinkTuple> links = FluentIterable.from(tuples)
                .filter(Predicates.notNull())
                .toList();

        Path path = new Path();
        for (LinkTuple link : links) {
            path.add(new FlowLink(link.getSrc(), link.getDst()));
        }
        return path;
    }
}

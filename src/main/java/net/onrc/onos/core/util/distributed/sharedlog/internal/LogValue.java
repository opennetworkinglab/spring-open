package net.onrc.onos.core.util.distributed.sharedlog.internal;

import javax.annotation.concurrent.Immutable;

import com.google.common.annotations.Beta;


// LogMap value transition
//
//  (null) -> ByteValue   : log successfully written
//  (null) -> NoOp   : allocated SeqNum abandoned/timed out
//  NoOp -> Recycled : became part of SnapShot

// When log reader encounter null
//  (case 1) wait for writer, if time out invalidate log with NoOp
//  (case 2) too far behind, reader need to reset to snap shot
// XXX How to distinguish above is TBD. For now assuming case 2 will never happen

/**
 * Value stored in LogMap.
 */
@Beta
@Immutable
public interface LogValue {

}

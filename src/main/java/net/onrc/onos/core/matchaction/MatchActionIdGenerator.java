package net.onrc.onos.core.matchaction;
/**
  * A generator of MatchActionId.
  */
public interface MatchActionIdGenerator {
    /**
      * Generates a globally unique MatchActionId instance.
      */
    MatchActionId getNewId();
}

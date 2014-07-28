package net.onrc.onos.core.datagrid;

import com.hazelcast.config.Config;
import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.examples.TestApp;

/**
 * Hazelcast CLI.
 */
public class HazelcastCLI extends TestApp {

    private static final int LOAD_EXECUTORS_COUNT = 16;

    /**
     * hazelcast CLI.
     *
     * @param hazelcast Hazelcast instance.
     */
    public HazelcastCLI(HazelcastInstance hazelcast) {
        super(hazelcast);
    }

    /**
     * {@link TestApp} modified to read conf/hazelcast.xml.
     *
     * @param args none expected
     * @throws Exception exception
     */
    public static void main(String[] args) throws Exception {
        final String configFilename = System.getProperty(
                "net.onrc.onos.core.datagrid.HazelcastDatagrid.datagridConfig",
                "conf/hazelcast.xml");
        Config config = HazelcastDatagrid.loadHazelcastConfig(configFilename);

        for (int k = 1; k <= LOAD_EXECUTORS_COUNT; k++) {
            config.addExecutorConfig(new ExecutorConfig("e" + k).setPoolSize(k));
        }

        HazelcastCLI cli = new HazelcastCLI(Hazelcast.newHazelcastInstance(config));
        cli.start(args);
    }

}

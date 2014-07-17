package net.onrc.onos.core.metrics.web.serializers;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.onrc.onos.core.metrics.web.MetricsObjectResource;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JSON serializer for the Metrics resource.
 */
public class MetricsObjectSerializer extends SerializerBase<MetricsObjectResource> {

    /**
     * Public constructor - just calls its super class constructor.
     */
    public MetricsObjectSerializer() {
        super(MetricsObjectResource.class);
    }

    /**
     * Convenience method to serialize a Metrics field.
     *
     * @param jsonGenerator generator to use for serialization
     * @param fieldName name of the top level field
     * @param serializedObjectJSON JSON representation from the Metrics serializer
     * @param object internal resource for the Metric
     * @throws IOException if JSON generation fails.
     */
    private void serializeItem(final JsonGenerator jsonGenerator,
                               final String fieldName,
                               final String serializedObjectJSON,
                               final MetricsObjectResource.BaseMetricObject object)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("name", object.getName());

        // If you write the JSON for the Metric using a StringField, the
        // generator applies an extra set of double quotes and breaks the
        // syntax.  You have to use the raw JSON output to get it right.
        jsonGenerator.writeRaw(",\"" + fieldName + "\": " + serializedObjectJSON);
        jsonGenerator.writeEndObject();
    }

    /**
     * Serialize a MetricsObjectResource into JSON.  For each kind of Metric,
     * his serializes common ONOS defined fields like name and
     * then calls the Metrics serializer to make the JSON string
     * for the actual Metric.
     *
     * @param metrics resource for all ONOS Metrics
     * @param jsonGenerator generator to use for the JSON output
     * @param serializerProvider unused, needed for Override
     * @throws IOException if any of the JSON serializations fail
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void serialize(final MetricsObjectResource metrics,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider)
            throws IOException {

        final ObjectMapper mapper = new ObjectMapper().registerModule(
                new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false));
        jsonGenerator.writeStartObject();

        //  serialize Timers
        jsonGenerator.writeArrayFieldStart("timers");

        for (final MetricsObjectResource.TimerObjectResource timer :
             metrics.getTimers()) {
            final String timerJSON = mapper.writeValueAsString(timer.getTimer());
            serializeItem(jsonGenerator, "timer", timerJSON, timer);
        }
        jsonGenerator.writeEndArray();

        // Serialize Gauges
        jsonGenerator.writeArrayFieldStart("gauges");

        for (final MetricsObjectResource.GaugeObjectResource gauge :
             metrics.getGauges()) {
            final String gaugeJSON = mapper.writeValueAsString(gauge.getGauge());
            serializeItem(jsonGenerator, "gauge", gaugeJSON, gauge);
        }
        jsonGenerator.writeEndArray();

        // Serialize Counters
        jsonGenerator.writeArrayFieldStart("counters");

        for (final MetricsObjectResource.CounterObjectResource counter :
             metrics.getCounters()) {
            final String counterJSON = mapper.writeValueAsString(counter.getCounter());
            serializeItem(jsonGenerator, "counter", counterJSON, counter);
        }
        jsonGenerator.writeEndArray();

        // Serialize Meters
        jsonGenerator.writeArrayFieldStart("meters");

        for (final MetricsObjectResource.MeterObjectResource meter :
             metrics.getMeters()) {
            final String meterJSON = mapper.writeValueAsString(meter.getMeter());
            serializeItem(jsonGenerator, "meter", meterJSON, meter);
        }
        jsonGenerator.writeEndArray();

        // Serialize Histograms
        jsonGenerator.writeArrayFieldStart("histograms");

        for (final MetricsObjectResource.HistogramObjectResource histogram :
             metrics.getHistograms()) {
            final String histogramJSON = mapper.writeValueAsString(histogram.getHistogram());
            serializeItem(jsonGenerator, "histogram", histogramJSON, histogram);
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
   }

}

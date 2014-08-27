package net.floodlightcontroller.restserver;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;

/**
 * Helper class used to dynamically override the default serializers used for
 * JSON serialization.
 * <p/>
 * Serializers can be added to the helper class at runtime using
 * {@link #addSerializer(Class, JsonSerializer)}. The serializers contained by
 * the helper class can then be added to a JacksonRepresentation prior to
 * serializing it using {@link #applySerializers(JacksonRepresentation)}.
 * <p/>
 * This class enables the use of custom serializers for Java objects without
 * having to hardcode the mapping of class to serializer using
 * annotations on the class. Any serialization annotations on the class will be
 * overridden by the serializers used here, so different serializers can be
 * used on the class in different contexts.
 */
public class CustomSerializerHelper {
    private final SimpleModule customSerializerModule;
    private CustomSerializerFactory sf;

    /**
     * Constructor.
     */
    public CustomSerializerHelper() {
        customSerializerModule = new SimpleModule("custom-serializers", new Version(1, 0, 0, null));
        sf =  new CustomSerializerFactory();
    }

    /**
     * Adds a serializer to the set of serializers that will be used for JSON
     * serialization.
     *
     * @param serializer the serializer to add
     */
    public <T> void addSerializer(Class<T> clazz, JsonSerializer<T> serializer) {
        customSerializerModule.addSerializer(serializer);
        sf.addGenericMapping(clazz, serializer);
    }

    /**
     * Applies the list of serializers to the JacksonRepresentation so they
     * will be used when the object in the representation is serialized
     *
     * @param jacksonRepresentation the representation to apply the serializers
     * to
     * @return a representation with the custom serializers applied
     */
    public Representation applySerializers(JacksonRepresentation<?> jacksonRepresentation) {
        ObjectMapper mapper = jacksonRepresentation.getObjectMapper();

        mapper.registerModule(customSerializerModule);
        mapper.setSerializerFactory(sf);
        jacksonRepresentation.setObjectMapper(mapper);

        return jacksonRepresentation;
    }
}

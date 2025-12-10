package edu.ptithcm.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.ptithcm.security.CryptoUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.PrivateKey;
import java.security.PublicKey;

public class JsonUtils {
    private static class PublicKeyTypeAdapter extends TypeAdapter<PublicKey>{

        @Override
        public void write(JsonWriter jsonWriter, PublicKey publicKey) throws IOException {
            jsonWriter.value(CryptoUtils.publicKeyToString(publicKey));
        }

        @Override
        public PublicKey read(JsonReader jsonReader) throws IOException {
            String publicKeyStr = jsonReader.nextString();
            return CryptoUtils.stringToPublicKey(publicKeyStr);
        }
    }

    private static class PrivateKeyTypeAdapter extends TypeAdapter<PrivateKey>{

        @Override
        public void write(JsonWriter jsonWriter, PrivateKey privateKey) throws IOException {
            jsonWriter.value(CryptoUtils.privateKeyToString(privateKey));
        }

        @Override
        public PrivateKey read(JsonReader jsonReader) throws IOException {
            String privateKeyStr = jsonReader.nextString();
            return CryptoUtils.stringToPrivateKey(privateKeyStr);
        }
    }
    private static final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(PublicKey.class, new PublicKeyTypeAdapter())
            .registerTypeHierarchyAdapter(PrivateKey.class, new PrivateKeyTypeAdapter())
            .create();

    /**
     * Convert object to json
     * @param obj: object to convert
     * @return json as String
     */
    public static String toJson(Object obj){
        return gson.toJson(obj);
    }

    /**
     * Converts a JSON string into an object of the specified class type.<br>
     * Use this when the target type is a non-generic class, for example:
     * <pre><code>
     * User user = JsonUtils.fromJson(json, User.class);
     * </code></pre>
     * @param json the JSON string to parse
     * @param clazz clazz the target class to convert to
     * @return an object of type T created from the JSON string
     */
    public static <T> T fromJson(String json, Class<T> clazz){
        return gson.fromJson(json, clazz);
    }

    /**
     * Converts a JSON string into an object of a generic type.<br>
     * Use this when parsing generic types.<br>
     * Example:
     * <pre><code>
     * Type listType = new TypeToken<List<Data>>(){}.getType();
     * List<Data> list = JsonUtils.fromJson(json, listType);
     * </code></pre>
     *
     * @param json     the JSON string to parse
     * @param typeOfT  the generic type to convert to
     * @return an object of type T created from the JSON string
     */
    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }
}

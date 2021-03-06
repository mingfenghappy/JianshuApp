package retrofit2.converter.gson;

import com.copy.jianshuapp.exception.JsonParserException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * JSON反序列失败时，能返回JsonParserException，以便获取原始JSON
 * @version imkarl 2017-05
 */
public final class CustomGsonConverterFactory extends Converter.Factory {
    public static CustomGsonConverterFactory create() {
        return create(new Gson());
    }
    public static CustomGsonConverterFactory create(Gson gson) {
        return new CustomGsonConverterFactory(gson);
    }

    private final Gson gson;

    private CustomGsonConverterFactory(Gson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        this.gson = gson;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type,
                                                            Annotation[] annotations,
                                                            Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonResponseBodyConverter<>(gson, adapter, TypeToken.get(type));
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,
                                                          Annotation[] parameterAnnotations,
                                                          Annotation[] methodAnnotations,
                                                          Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonRequestBodyConverter<>(gson, adapter);
    }

    private static final class GsonResponseBodyConverter<T>
            implements Converter<ResponseBody, T> {
        private final Gson gson;
        private final TypeAdapter<T> adapter;
        private final TypeToken type;

        GsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter, TypeToken type) {
            this.gson = gson;
            this.adapter = adapter;
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T convert(ResponseBody value) throws IOException {
            if (type.getRawType().equals(String.class)) {
                return (T) value.string();
            }

            JsonElement jsonElement = null;
            T result;
            try {
                // 解析错误时可获取原始JSON
                jsonElement = gson.fromJson(value.charStream(), JsonElement.class);
                if (jsonElement == null) {
                    jsonElement = JsonNull.INSTANCE;
                }
                result = adapter.fromJsonTree(jsonElement);
                if (result == null) {
                    try {
                        result = adapter.fromJson("{}");
                    } catch (Exception ignored) {
                    }
                }
            } catch (JsonSyntaxException e) {
                throw new JsonParserException(jsonElement, e.getMessage());
            } finally {
                value.close();
            }
            return result;
        }
    }

}
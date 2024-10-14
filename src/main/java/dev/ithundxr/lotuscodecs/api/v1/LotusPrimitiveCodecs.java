package dev.ithundxr.lotuscodecs.api.v1;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

@SuppressWarnings("unused")
public interface LotusPrimitiveCodecs {
    PrimitiveCodec<Character> CHAR = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Character> read(final DynamicOps<T> ops, final T input) {
            return ops.getNumberValue(input)
                    .map(n -> (char) n.intValue());
        }

        @Override
        public <T> T write(final DynamicOps<T> ops, final Character value) {
            return ops.createInt(value);
        }

        @Override
        public String toString() {
            return "Char";
        }
    };
}

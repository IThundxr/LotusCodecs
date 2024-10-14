package dev.ithundxr.lotuscodecs.stream.api.v1;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;

public interface LotusStreamCodecBuilders {
    static <T extends Enum<T>> StreamCodec<ByteBuf, T> ofEnum(Class<T> clazz) {
        return new StreamCodec<>() {
            public @NotNull T decode(@NotNull ByteBuf buffer) {
                return clazz.getEnumConstants()[VarInt.read(buffer)];
            }

            public void encode(@NotNull ByteBuf buffer, @NotNull T value) {
                VarInt.write(buffer, value.ordinal());
            }
        };
    }

    static <B extends ByteBuf, L, R> StreamCodec<B, Pair<L, R>> pair(StreamCodec<B, L> codecL, StreamCodec<B, R> codecR) {
        return new StreamCodec<>() {
            @Override
            public @NotNull Pair<L, R> decode(B buffer) {
                L l = codecL.decode(buffer);
                R r = codecR.decode(buffer);
                return Pair.of(l, r);
            }

            @Override
            public void encode(B buffer, Pair<L, R> value) {
                codecL.encode(buffer, value.getFirst());
                codecR.encode(buffer, value.getSecond());
            }
        };
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, Optional<V>> optional() {
        return ByteBufCodecs::optional;
    }

    static <B extends ByteBuf, V> StreamCodec<B, @Nullable V> nullable(StreamCodec<B, V> base) {
        return new StreamCodec<>() {
            @Override
            @SuppressWarnings("NullableProblems")
            public @Nullable V decode(@NotNull B buffer) {
                if (buffer.readBoolean())
                    return base.decode(buffer);
                else
                    return null;
            }

            @Override
            public void encode(@NotNull B buffer, @Nullable V value) {
                if (value != null) {
                    buffer.writeBoolean(true);
                    base.encode(buffer, value);
                } else {
                    buffer.writeBoolean(false);
                }
            }
        };
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, @Nullable V> nullable() {
        return LotusStreamCodecBuilders::nullable;
    }

    static <B extends ByteBuf, V> StreamCodec<B, List<V>> list(StreamCodec<B, V> base) {
        return base.apply(ByteBufCodecs.list());
    }

    static <B extends ByteBuf, V> StreamCodec<B, List<V>> list(StreamCodec<B, V> base, int maxSize) {
        return base.apply(ByteBufCodecs.list(maxSize));
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, NonNullList<V>> nonNullList() {
        return streamCodec -> ByteBufCodecs.collection(NonNullList::createWithCapacity, streamCodec);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, NonNullList<V>> nonNullList(int maxSize) {
        return streamCodec -> ByteBufCodecs.collection(NonNullList::createWithCapacity, streamCodec, maxSize);
    }

    static <B extends ByteBuf, V> StreamCodec<B, NonNullList<V>> nonNullList(StreamCodec<B, V> base) {
        return base.apply(nonNullList());
    }

    static <B extends ByteBuf, V> StreamCodec<B, NonNullList<V>> nonNullList(StreamCodec<B, V> base, int maxSize) {
        return base.apply(nonNullList(maxSize));
    }

    static <B extends FriendlyByteBuf, V> StreamCodec<B, V[]> array(StreamCodec<? super B, V> base, Class<?> clazz) {
        return new StreamCodec<>() {
            @Override
            public V @NotNull [] decode(@NotNull B buffer) {
                int size = buffer.readVarInt();
                @SuppressWarnings("unchecked")
                V[] array = (V[]) Array.newInstance(clazz, size);
                for (int i = 0; i < size; i++) {
                    array[i] = base.decode(buffer);
                }
                return array;
            }

            @Override
            public void encode(@NotNull B buffer, @NotNull V[] value) {
                buffer.writeVarInt(value.length);
                for (V v : value) {
                    base.encode(buffer, v);
                }
            }
        };
    }

    static <B extends FriendlyByteBuf, V> StreamCodec.CodecOperation<B, V, V[]> array(Class<?> clazz) {
        return streamCodec -> array(streamCodec, clazz);
    }
}

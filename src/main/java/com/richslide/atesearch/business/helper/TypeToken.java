package com.richslide.atesearch.business.helper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public class TypeToken<T> {
    private final Type type;
    private Optional<String> name;
    private Optional<T> value;

    public TypeToken(T value) {
        this(null, value);
    }

    public TypeToken(String name, T value) {
        Type stype = getClass().getGenericSuperclass();
        System.out.println(stype);
        if (stype instanceof ParameterizedType)
            this.type = ((ParameterizedType) stype).getActualTypeArguments()[0];
        else throw new RuntimeException("There is no parameterized type or unknown type.");

        this.name = Objects.nonNull(name) ? Optional.of(name) : Optional.empty();
        this.value = Objects.nonNull(value) ? Optional.of(value) : Optional.empty();
    }

    public TypeToken() {
        this(null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass().getSuperclass() != o.getClass().getSuperclass()) return false;
        TypeToken<?> that = (TypeToken<?>) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() { return type.hashCode(); }

    public Type getType() {
        return type;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<T> getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = Objects.nonNull(name) ? Optional.of(name) : Optional.empty();
    }

    public void setValue(T t) {
        this.value = Objects.nonNull(t) ? Optional.of(t) : Optional.empty();
    }

//    public static void main(String[] args) {
//        TypeToken token = new TypeToken<Integer>("String", 13) {};
//        System.out.println(token.type + " / " + token.name + " / " + token.value + " / " + token.value.getClass().getSimpleName());
//    }
}

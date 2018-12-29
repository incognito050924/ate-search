package com.richslide.atesearch.business.domain.model.mapper;

import com.richslide.atesearch.business.domain.model.Equipment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.richslide.atesearch.business.helper.CommonsUtil.convertCamelCase;

public class DocumentMapper {

    /**
     * 자바 빈 객체를 {@link HashMap<String, Object>} 객체로 변환한다.
     *
     * @param t JavaBean spec 구현체.
     * @param <T>
     * @return
     */
    public static <T> Map<String, Object> bean2Map(final T t) {
        final Map<String, Object> map = new HashMap<>();
        final Class[] noArgs = {};
        List<Field> fields = Arrays.asList(t.getClass().getDeclaredFields());
        fields.stream()
            .map(field -> convertCamelCase(field.getName(), "get", null))
            .map(getterName -> {
                try {
                    Method getter = t.getClass().getMethod(getterName, noArgs);
                    String key = convertCamelCase(getterName.substring(3), null, null);
                    return new AbstractMap.SimpleEntry<>(key, getter.invoke(t));
                } catch (NoSuchMethodException | IllegalAccessException |InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .filter(entry -> Objects.nonNull(entry) && Objects.nonNull(entry.getValue()))
            .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        return map;
    }

    /**
     * {@link Map<String, Object>} 객체를 자바 빈 객체로 변환한다.
     * @param map
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> Optional<T> map2Bean(final Map<String, Object> map, final Class<T> clazz) {
        try {
            final T bean = clazz.newInstance();
            for (String key : map.keySet()) {
                final String setterName = convertCamelCase(key, "set", null);
                if (setterName.equals("setVersion")) {
                    System.err.println(map.get(key).getClass());
                    clazz.getMethod(setterName, map.get(key).getClass()).invoke(bean, Long.class.cast(map.get(key)));
                }
                clazz.getMethod(setterName, map.get(key).getClass()).invoke(bean, map.get(key));
            }
            return Optional.of(bean);

        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }



//    public static void main(String[] args) {
//        Equipment e = new Equipment();
//        ArrayList<String> cate = new ArrayList<>(Arrays.asList("cate1", "cate2"));
//
//        e.setCategories(cate);
//        e.setTitle("Equipment 1");
//        e.setId("eq1");
//        e.setModel("ate001");
//        e.setUrl("http://test.com");
//        e.setVintage(2003);
//
//        Map<String, Object> map = bean2Map(e);
//        System.out.println(map.toString());
//        map2Bean(map, Equipment.class).ifPresent(System.out::println);
//    }
}

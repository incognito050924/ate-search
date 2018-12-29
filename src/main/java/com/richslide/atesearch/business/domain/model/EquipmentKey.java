package com.richslide.atesearch.business.domain.model;

public enum EquipmentKey {
    // Commons
    EQUIPMENT_ID("id")
    , VERSION("version")
    , AD_NUMBER("adNum")
    , TITLE("title")
    , URL("url")
    , BASE_URL("website")
    , VINTAGE("vintage")
    , PRICE("price")
    , CATEGORY_PRIMARY(EquipmentKey.CATEGORY_FIELD_NAME + EquipmentKey.SEPARATOR + EquipmentKey.PRIMARY_CATEGORY_SUFFIX)
    , CATEGORY_SECONDARY(EquipmentKey.CATEGORY_FIELD_NAME + EquipmentKey.SEPARATOR + EquipmentKey.SECONDARY_CATEGORY_SUFFIX)
    , AUTHOR("author")
    , UPD_ID("modifier")
    , AUTHOR_ORG("authorOrganization")
    , ADDITIONAL_INFO("additionalInfo")
    , RELATED_ITME_1(EquipmentKey.RELATED_ITEMS_FIELD_NAME + EquipmentKey.SEPARATOR + EquipmentKey.RELATED_ITME_1_SUFFIX)
    , RELATED_ITME_2(EquipmentKey.RELATED_ITEMS_FIELD_NAME + EquipmentKey.SEPARATOR + EquipmentKey.RELATED_ITME_2_SUFFIX)
    , RELATED_ITME_3(EquipmentKey.RELATED_ITEMS_FIELD_NAME + EquipmentKey.SEPARATOR + EquipmentKey.RELATED_ITME_3_SUFFIX)
    , RELATED_ITME_4(EquipmentKey.RELATED_ITEMS_FIELD_NAME + EquipmentKey.SEPARATOR + EquipmentKey.RELATED_ITME_4_SUFFIX)
    , RELATED_ITME_5(EquipmentKey.RELATED_ITEMS_FIELD_NAME + EquipmentKey.SEPARATOR + EquipmentKey.RELATED_ITME_5_SUFFIX)
    // Equipmatching Only
    , LOCATION("location")
    , MANUFACTURER("manufacturer")
    , AD_STATUS("adType")
    , TYPE("type")
    , MODEL("model")
    , CONDITION("condition")
    , QUANTITY("quantity")
    , SELLER_TYPE("sellerType")
    // Surplusglobal Only
    , POST_TYPE("adType")
    , RECOM_YN("recomYN")
    , SERIAL_NO("serialNo")
    , WAFER_SIZE("waferSize")
    , BUY_SELL("adStatus")
    , STATUS("sellingStatus")
    , WRITE_DATE("regDate")
    , UPD_DATE("modDate");

    EquipmentKey(final String key) {
        this.text = key;
    }

    private String text;

    public String getText() {
        return text;
    }

    public static final String INDEX_NAME = "equipments";
    public static final String TYPE_NAME = "equipment";

    private static final String SEPARATOR = ".";

    /**
     * This constant`s literal must be equals to variable name that {@link com.richslide.atesearch.business.domain.model.Equipment#categories}
     */
    public static final String CATEGORY_FIELD_NAME = "categories";
    public static final String PRIMARY_CATEGORY_SUFFIX = "primary";
    public static final String SECONDARY_CATEGORY_SUFFIX = "secondary";

    /**
     * This constant`s literal must be equals to variable name that {@link com.richslide.atesearch.business.domain.model.Equipment#related}
     */
    public static final String RELATED_ITEMS_FIELD_NAME = "related";
    public static final String RELATED_ITME_1_SUFFIX = "related1";
    public static final String RELATED_ITME_2_SUFFIX = "related2";
    public static final String RELATED_ITME_3_SUFFIX = "related3";
    public static final String RELATED_ITME_4_SUFFIX = "related4";
    public static final String RELATED_ITME_5_SUFFIX = "related5";

}

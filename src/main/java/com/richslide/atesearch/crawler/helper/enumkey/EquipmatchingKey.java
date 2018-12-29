package com.richslide.atesearch.crawler.helper.enumkey;

@Deprecated
public enum EquipmatchingKey {
    EQUIPMENT_ID("id"), VERSION("version"), TITLE("title"), AUTHOR("authorInfo.author")
    , LOCATION("location"), AD_STATUS("adType"), MANUFACTURER("manufacturer"), TYPE("type")
    , MODEL("model"), VINTAGE("vintage"), CONDITION("condition"), PRICE("price")
    , QUANTITY("quantity"), SELLER_TYPE("sellerType"), ADDITIONAL_INFO("additionalInfo"), RELATED("related")
    , CATEGORIES("categories"), AD_NUMBER("adNum"), URL("url"), BASE_URL("website");

    EquipmatchingKey(final String text) { this.text = text; }

    private String text;
    public String getText() { return text; }
}

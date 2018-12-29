package com.richslide.atesearch.crawler.helper.enumkey;

@Deprecated
public enum SurplusGlobalKey {
    EQUIPMENT_ID("id"), VERSION("version"), ITEM_NAME("title"), WRITER_ID("author")
    , UPD_ID("author2"), WRITER_COMPANY("authorOrganization"), POST_TYPE("adType"), RECOM_YN("recomYN")
    , SERIAL_NO("serialNo"), VINTAGE("vintage"), ADDITIONAL_INFO("additionalInfo"), PRICE("price")
    , WAFER_SIZE("waferSize"), BUY_SELL("adStatus"),  STATUS("sellingStatus"), RELATED("related")
    , CATEGORIES("categories"), AD_NUMBER("adNum"), URL("url"), BASE_URL("website")
    , WRITE_DATE("regDate"), UPD_DATE("modDate");

    SurplusGlobalKey(final String text) { this.text = text; }

    private String text;
    public String getText() { return text; }
}

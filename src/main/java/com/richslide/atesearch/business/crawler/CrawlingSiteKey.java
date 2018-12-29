package com.richslide.atesearch.business.crawler;

public enum CrawlingSiteKey {
    EQUIPMATCING("https://www.equipmatching.com/")
    , SURPLUSGLOBAL("https://www.surplusglobal.com/")
    ;

    CrawlingSiteKey(final String text) { this.text = text; }

    private String text;
    public String getText() { return text; }
}

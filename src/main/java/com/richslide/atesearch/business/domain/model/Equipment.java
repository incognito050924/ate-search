package com.richslide.atesearch.business.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDate;
import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Setting(settingPath = "/elasticsearch/settings.json")
@Document(indexName = EquipmentKey.INDEX_NAME, type = EquipmentKey.TYPE_NAME)
public class Equipment {
    @Id
    private String id;
    @Version
    private Long version;
    /** surplusglobal: Item_Name(xml) / equipmatching: title */
    @Field(type = FieldType.Text, analyzer = "ko_analyzer", searchAnalyzer = "ko_analyzer")
    private String title;
    /** surplusglobal: WriterID(xml)  / equipmatching: Posted by*/
    @Field(type = FieldType.Text)
    private String author;
    /** surplusglobal: UpdID(xml) / equipmatching: - */
    @Field(type = FieldType.Text)
    private String modifier;
    /** surplusglobal: Writer_Company(xml) / equipmatching: - */
    @Field(type = FieldType.Text)
    private String authorOrganization;
    /** surplusglobal: - / equipmatching: Location */
    @Field(type = FieldType.Text)
    private String location;
    /** surplusglobal: PostType(xml) / equipmatching: Ad Status */
    @Field(type = FieldType.Text)
    private String adType;
    /** surplusglobal: / equipmatching: Manufacturer */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String manufacturer;
    /** surplusglobal: / equipmatching: Type */
    @Field(type = FieldType.Text)
    private String type;
    /** surplusglobal: Serial Number(html) / equipmatching: - */
    @Field(type = FieldType.Text)
    private String serialNo;
    /** surplusglobal: - / equipmatching: Model */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String model;
    /** surplusglobal: Vintage(html) / equipmatching: Year */
    @Field(type = FieldType.Text)
    private String vintage;
    /** surplusglobal: Configuration / equipmatching: Description | Specification */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String additionalInfo;
    /** surplusglobal & equipmatching: Category Information */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = EquipmentKey.PRIMARY_CATEGORY_SUFFIX, type = FieldType.Text, fielddata = true, store = true, analyzer = "standard", searchAnalyzer = "standard")
                    , @InnerField(suffix = EquipmentKey.SECONDARY_CATEGORY_SUFFIX, type = FieldType.Text, store = true, analyzer = "standard", searchAnalyzer = "standard")
            }
    )
    private ArrayList<String> categories;
    /** surplusglobal: Wafer Size(html) / equipmatching: - */
    @Field(type = FieldType.Integer)
    private Integer waferSize;
    /** surplusglobal: Recom_YN(xml) / equipmatching: - */
    @Field(type = FieldType.Text, index = false)
    private String recomYN;
    /** surplusglobal & equipmatching : Equipment Ad url  */
    @Field(type = FieldType.Text, index = false)
    private String url;
    /** surplusglobal: - / equipmatching: Condition */
    @Field(type = FieldType.Text)
    private String condition;
    /** surplusglobal: / equipmatching: Price */
    @Field(type = FieldType.Text)
    private String price;
    /** surplusglobal: / equipmatching: Quantity */
    @Field(type = FieldType.Text)
    private String quantity;
    /** surplusglobal: - / equipmatching: Type Of Seller */
    @Field(type = FieldType.Text)
    private String sellerType;
    /** surplusglobal: BuySell(xml) / equipmatching: - */
    @Field(type = FieldType.Text)
    private String adStatus;
    /** surplusglobal: Status(xml) / equipmatching: - */
    @Field(type = FieldType.Text)
    private String sellingStatus;
    /** surplusglobal: Related Items(html) / equipmatching: Related Machines */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = EquipmentKey.RELATED_ITME_1_SUFFIX, type = FieldType.Text, fielddata = true, store = true, analyzer = "ko_analyzer", searchAnalyzer = "ko_analyzer")
                    , @InnerField(suffix = EquipmentKey.RELATED_ITME_2_SUFFIX, type = FieldType.Text, store = true, analyzer = "ko_analyzer", searchAnalyzer = "ko_analyzer")
                    , @InnerField(suffix = EquipmentKey.RELATED_ITME_3_SUFFIX, type = FieldType.Text, store = true, analyzer = "ko_analyzer", searchAnalyzer = "ko_analyzer")
                    , @InnerField(suffix = EquipmentKey.RELATED_ITME_4_SUFFIX, type = FieldType.Text, store = true, analyzer = "ko_analyzer", searchAnalyzer = "ko_analyzer")
                    , @InnerField(suffix = EquipmentKey.RELATED_ITME_5_SUFFIX, type = FieldType.Text, store = true, analyzer = "ko_analyzer", searchAnalyzer = "ko_analyzer")
            }
    )
    private ArrayList<String> related;
    /** surplusglobal: Item No(html)/Item_No(xml) / equipmatching: Ad Number */
    @Field(type = FieldType.Integer)
    private Integer adNum;
    /** surplusglobal & equipmatching website url */
    @Field(type = FieldType.Text)
    private String website;
    /** surplusglobal: WriteDate(xml) / equipmatching: - */
    @Field(type = FieldType.Date, format = DateFormat.date/*, pattern = "yyyy-MM-dd hh:mm:ss"*/)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd")
    private LocalDate regDate;
    /** surplusglobal: UpdDate(xml) / equipmatching: - */
    @Field(type = FieldType.Date, format = DateFormat.date)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd")
    private LocalDate modDate;
}

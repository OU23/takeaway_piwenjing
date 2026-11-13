package com.xingguang.delivery.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 规格组（同一 groupName 下的规格项 / 加料项）。 */
@Data
public class SpecGroupVO {

    private String groupName;
    /** SPEC 规格（单选） / ADDON 加料（多选） */
    private String type;
    private List<Option> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private Long id;
        private String name;
        /** 加价（分） */
        private Integer priceDelta;
        private Boolean isDefault;
    }
}

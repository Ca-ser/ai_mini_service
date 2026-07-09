package com.waiitz.suji_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档目录树节点视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTreeVO {

    private UUID id;
    private String title;
    /** 节点类型，当前统一为 DOCUMENT */
    private String type;
    /** 子节点列表 */
    private List<DocumentTreeVO> children;

    public DocumentTreeVO(UUID id, String title, String type) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.children = new ArrayList<>();
    }

}

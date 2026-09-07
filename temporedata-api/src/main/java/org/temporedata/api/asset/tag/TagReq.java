package org.temporedata.api.asset.tag;

import lombok.Data;

/**
 * Create / update tag request.
 */
@Data
public class TagReq {

    private String name;

    private String code;

    private String description;

    private String color; // hex color code

    private String classificationId; // optional tag classification

    private Boolean isMutuallyExclusive; // tags in same classification are mutually exclusive

    private String status; // DRAFT, IN_REVIEW, APPROVED, REJECTED

    private String derivedFromTagId; // auto-derived from parent tag when parent is bound

    private String definition; // authoritative definition (term tags)

    private String synonyms; // comma separated synonyms (term tags)

    private Boolean isTerm; // true = glossary term with definition/synonyms
}
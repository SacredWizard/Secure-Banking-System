package edu.asu.sbs.util;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RichQuery {
    private static final long serialVersionUID = 1L;

    private Map<String, Object> selector;
    private String limit;
    private String skip;
    private List<Map<String, String>> sort;
    private Map<String, String> fields;
    //Use two strings: one for the ddoc and one for the index name
    private List<String> useIndex;
}

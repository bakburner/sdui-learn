
package com.nba.sdui.models.generated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


/**
 * Sortable, paginated table of season statistical leaders (league-wide)
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "title",
    "subtitle",
    "columns",
    "players",
    "totalRows",
    "page",
    "pageSize",
    "sortColumn",
    "sortDirection",
    "emptyMessage"
})
@Generated("jsonschema2pojo")
public class SeasonLeadersTable {

    /**
     * Table heading, e.g. 'Season Leaders'
     * 
     */
    @JsonProperty("title")
    @JsonPropertyDescription("Table heading, e.g. 'Season Leaders'")
    private String title;
    /**
     * Secondary text, e.g. '2025-26 Regular Season – Per Game'
     * 
     */
    @JsonProperty("subtitle")
    @JsonPropertyDescription("Secondary text, e.g. '2025-26 Regular Season \u2013 Per Game'")
    private String subtitle;
    /**
     * Ordered column definitions; clients render left-to-right
     * (Required)
     * 
     */
    @JsonProperty("columns")
    @JsonPropertyDescription("Ordered column definitions; clients render left-to-right")
    @Valid
    @NotNull
    private List<BoxscoreColumnDefinition> columns = new ArrayList<BoxscoreColumnDefinition>();
    /**
     * Player rows, pre-sorted by the server
     * (Required)
     * 
     */
    @JsonProperty("players")
    @JsonPropertyDescription("Player rows, pre-sorted by the server")
    @Valid
    @NotNull
    private List<LeadersPlayerRow> players = new ArrayList<LeadersPlayerRow>();
    /**
     * Total number of rows available server-side (for pagination display)
     * 
     */
    @JsonProperty("totalRows")
    @JsonPropertyDescription("Total number of rows available server-side (for pagination display)")
    private Integer totalRows;
    /**
     * Current page (1-based)
     * 
     */
    @JsonProperty("page")
    @JsonPropertyDescription("Current page (1-based)")
    private Integer page = 1;
    /**
     * Number of rows per page
     * 
     */
    @JsonProperty("pageSize")
    @JsonPropertyDescription("Number of rows per page")
    private Integer pageSize = 25;
    /**
     * Key of the column the table is currently sorted by
     * 
     */
    @JsonProperty("sortColumn")
    @JsonPropertyDescription("Key of the column the table is currently sorted by")
    private String sortColumn;
    @JsonProperty("sortDirection")
    private SeasonLeadersTable.SortDirection sortDirection = SeasonLeadersTable.SortDirection.fromValue("desc");
    /**
     * Text shown when no player rows are available
     * 
     */
    @JsonProperty("emptyMessage")
    @JsonPropertyDescription("Text shown when no player rows are available")
    private String emptyMessage;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Table heading, e.g. 'Season Leaders'
     * 
     */
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    /**
     * Table heading, e.g. 'Season Leaders'
     * 
     */
    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public SeasonLeadersTable withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Secondary text, e.g. '2025-26 Regular Season – Per Game'
     * 
     */
    @JsonProperty("subtitle")
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Secondary text, e.g. '2025-26 Regular Season – Per Game'
     * 
     */
    @JsonProperty("subtitle")
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public SeasonLeadersTable withSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /**
     * Ordered column definitions; clients render left-to-right
     * (Required)
     * 
     */
    @JsonProperty("columns")
    public List<BoxscoreColumnDefinition> getColumns() {
        return columns;
    }

    /**
     * Ordered column definitions; clients render left-to-right
     * (Required)
     * 
     */
    @JsonProperty("columns")
    public void setColumns(List<BoxscoreColumnDefinition> columns) {
        this.columns = columns;
    }

    public SeasonLeadersTable withColumns(List<BoxscoreColumnDefinition> columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Player rows, pre-sorted by the server
     * (Required)
     * 
     */
    @JsonProperty("players")
    public List<LeadersPlayerRow> getPlayers() {
        return players;
    }

    /**
     * Player rows, pre-sorted by the server
     * (Required)
     * 
     */
    @JsonProperty("players")
    public void setPlayers(List<LeadersPlayerRow> players) {
        this.players = players;
    }

    public SeasonLeadersTable withPlayers(List<LeadersPlayerRow> players) {
        this.players = players;
        return this;
    }

    /**
     * Total number of rows available server-side (for pagination display)
     * 
     */
    @JsonProperty("totalRows")
    public Integer getTotalRows() {
        return totalRows;
    }

    /**
     * Total number of rows available server-side (for pagination display)
     * 
     */
    @JsonProperty("totalRows")
    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public SeasonLeadersTable withTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
        return this;
    }

    /**
     * Current page (1-based)
     * 
     */
    @JsonProperty("page")
    public Integer getPage() {
        return page;
    }

    /**
     * Current page (1-based)
     * 
     */
    @JsonProperty("page")
    public void setPage(Integer page) {
        this.page = page;
    }

    public SeasonLeadersTable withPage(Integer page) {
        this.page = page;
        return this;
    }

    /**
     * Number of rows per page
     * 
     */
    @JsonProperty("pageSize")
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Number of rows per page
     * 
     */
    @JsonProperty("pageSize")
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public SeasonLeadersTable withPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Key of the column the table is currently sorted by
     * 
     */
    @JsonProperty("sortColumn")
    public String getSortColumn() {
        return sortColumn;
    }

    /**
     * Key of the column the table is currently sorted by
     * 
     */
    @JsonProperty("sortColumn")
    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    public SeasonLeadersTable withSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
        return this;
    }

    @JsonProperty("sortDirection")
    public SeasonLeadersTable.SortDirection getSortDirection() {
        return sortDirection;
    }

    @JsonProperty("sortDirection")
    public void setSortDirection(SeasonLeadersTable.SortDirection sortDirection) {
        this.sortDirection = sortDirection;
    }

    public SeasonLeadersTable withSortDirection(SeasonLeadersTable.SortDirection sortDirection) {
        this.sortDirection = sortDirection;
        return this;
    }

    /**
     * Text shown when no player rows are available
     * 
     */
    @JsonProperty("emptyMessage")
    public String getEmptyMessage() {
        return emptyMessage;
    }

    /**
     * Text shown when no player rows are available
     * 
     */
    @JsonProperty("emptyMessage")
    public void setEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage;
    }

    public SeasonLeadersTable withEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public SeasonLeadersTable withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SeasonLeadersTable.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("title");
        sb.append('=');
        sb.append(((this.title == null)?"<null>":this.title));
        sb.append(',');
        sb.append("subtitle");
        sb.append('=');
        sb.append(((this.subtitle == null)?"<null>":this.subtitle));
        sb.append(',');
        sb.append("columns");
        sb.append('=');
        sb.append(((this.columns == null)?"<null>":this.columns));
        sb.append(',');
        sb.append("players");
        sb.append('=');
        sb.append(((this.players == null)?"<null>":this.players));
        sb.append(',');
        sb.append("totalRows");
        sb.append('=');
        sb.append(((this.totalRows == null)?"<null>":this.totalRows));
        sb.append(',');
        sb.append("page");
        sb.append('=');
        sb.append(((this.page == null)?"<null>":this.page));
        sb.append(',');
        sb.append("pageSize");
        sb.append('=');
        sb.append(((this.pageSize == null)?"<null>":this.pageSize));
        sb.append(',');
        sb.append("sortColumn");
        sb.append('=');
        sb.append(((this.sortColumn == null)?"<null>":this.sortColumn));
        sb.append(',');
        sb.append("sortDirection");
        sb.append('=');
        sb.append(((this.sortDirection == null)?"<null>":this.sortDirection));
        sb.append(',');
        sb.append("emptyMessage");
        sb.append('=');
        sb.append(((this.emptyMessage == null)?"<null>":this.emptyMessage));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.emptyMessage == null)? 0 :this.emptyMessage.hashCode()));
        result = ((result* 31)+((this.sortColumn == null)? 0 :this.sortColumn.hashCode()));
        result = ((result* 31)+((this.sortDirection == null)? 0 :this.sortDirection.hashCode()));
        result = ((result* 31)+((this.columns == null)? 0 :this.columns.hashCode()));
        result = ((result* 31)+((this.players == null)? 0 :this.players.hashCode()));
        result = ((result* 31)+((this.subtitle == null)? 0 :this.subtitle.hashCode()));
        result = ((result* 31)+((this.pageSize == null)? 0 :this.pageSize.hashCode()));
        result = ((result* 31)+((this.totalRows == null)? 0 :this.totalRows.hashCode()));
        result = ((result* 31)+((this.page == null)? 0 :this.page.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.title == null)? 0 :this.title.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SeasonLeadersTable) == false) {
            return false;
        }
        SeasonLeadersTable rhs = ((SeasonLeadersTable) other);
        return ((((((((((((this.emptyMessage == rhs.emptyMessage)||((this.emptyMessage!= null)&&this.emptyMessage.equals(rhs.emptyMessage)))&&((this.sortColumn == rhs.sortColumn)||((this.sortColumn!= null)&&this.sortColumn.equals(rhs.sortColumn))))&&((this.sortDirection == rhs.sortDirection)||((this.sortDirection!= null)&&this.sortDirection.equals(rhs.sortDirection))))&&((this.columns == rhs.columns)||((this.columns!= null)&&this.columns.equals(rhs.columns))))&&((this.players == rhs.players)||((this.players!= null)&&this.players.equals(rhs.players))))&&((this.subtitle == rhs.subtitle)||((this.subtitle!= null)&&this.subtitle.equals(rhs.subtitle))))&&((this.pageSize == rhs.pageSize)||((this.pageSize!= null)&&this.pageSize.equals(rhs.pageSize))))&&((this.totalRows == rhs.totalRows)||((this.totalRows!= null)&&this.totalRows.equals(rhs.totalRows))))&&((this.page == rhs.page)||((this.page!= null)&&this.page.equals(rhs.page))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.title == rhs.title)||((this.title!= null)&&this.title.equals(rhs.title))));
    }

    @Generated("jsonschema2pojo")
    public enum SortDirection {

        ASC("asc"),
        DESC("desc");
        private final String value;
        private final static Map<String, SeasonLeadersTable.SortDirection> CONSTANTS = new HashMap<String, SeasonLeadersTable.SortDirection>();

        static {
            for (SeasonLeadersTable.SortDirection c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        SortDirection(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static SeasonLeadersTable.SortDirection fromValue(String value) {
            SeasonLeadersTable.SortDirection constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

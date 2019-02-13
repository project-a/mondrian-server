package com.projecta.mondrianserver.sql;

/**
 * Bean class for a parsed fragment of an SQL query
 */
public class SqlFragment {

    private FragmentType type;
    private String       text;
    private String       newText;

    private String       tableAlias;
    private String       columnName;
    private String       resultAlias;
    private String       aggFunction;
    private String       schemaName;
    private String       tableName;
    private String       joinedTableAlias;
    private String       joinedColumnName;
    private boolean      hasNext;


    public enum FragmentType {
        SELECT_KEWORD,
        SELECT_EXPRESSION,
        SELECT_AGGREGATION,
        FROM_KEWORD,
        TABLE,
        WHERE_KEWORD,
        AND_KEWORD,
        JOIN_CONDITION,
        CONDITION,
        GROUP_BY_KEWORD,
        ORDER_BY_KEWORD,
        UNPARSED_EXPRESSION
    }


    @Override
    public String toString() {
        return "SqlFragment [type=" + type + ", text=" + text + ", newText=" + newText + ", tableAlias=" + tableAlias
                + ", columnName=" + columnName + ", resultAlias=" + resultAlias + ", aggFunction=" + aggFunction
                + ", schemaName=" + schemaName + ", tableName=" + tableName + ", joinedTableAlias=" + joinedTableAlias
                + ", joinedColumnName=" + joinedColumnName + ", hasNext=" + hasNext + "]";
    }


    public SqlFragment(String text) {
        this.text = text;
        this.type = FragmentType.UNPARSED_EXPRESSION;
    }

    public FragmentType getType() {
        return type;
    }

    public void setType(FragmentType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getNewText() {
        return newText;
    }

    public void setNewText(String newText) {
        this.newText = newText;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getResultAlias() {
        return resultAlias;
    }

    public void setResultAlias(String resultAlias) {
        this.resultAlias = resultAlias;
    }

    public String getAggFunction() {
        return aggFunction;
    }

    public void setAggFunction(String aggFunction) {
        this.aggFunction = aggFunction;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public void setTableAlias(String tableAlias) {
        this.tableAlias = tableAlias;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public String getJoinedTableAlias() {
        return joinedTableAlias;
    }

    public void setJoinedTableAlias(String joinedTableAlias) {
        this.joinedTableAlias = joinedTableAlias;
    }

    public String getJoinedColumnName() {
        return joinedColumnName;
    }

    public void setJoinedColumnName(String joinedColumnName) {
        this.joinedColumnName = joinedColumnName;
    }

}

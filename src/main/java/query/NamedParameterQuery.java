package query;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NamedParameterQuery {

    // :paramName 패턴 (영문자, 숫자, 언더스코어 허용)
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":(\\w+)");

    private final String originalQuery;
    private final String parsedQuery;
    private final Map<String, List<Integer>> parameterIndexMap;

    public NamedParameterQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL cannot be null or empty");
        }

        this.originalQuery = sql;
        this.parameterIndexMap = new LinkedHashMap<>();  // 순서 보장

        // SQL 파싱
        this.parsedQuery = parse(sql);
    }

    private String parse(String sql) {
        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        int index = 1;

        while (matcher.find()) {
            String paramName = matcher.group(1);
            parameterIndexMap
                    .computeIfAbsent(paramName, k -> new ArrayList<>())
                    .add(index);

            index++;

            matcher.appendReplacement(sb, "?");
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    public String getParsedQuery() {
        return parsedQuery;
    }


    public String getOriginalQuery() {
        return originalQuery;
    }

    public int getParameterIndex(String paramName) {
        // :를 제거
        String cleanName = paramName.startsWith(":") ? paramName.substring(1) : paramName;

        List<Integer> indexes = parameterIndexMap.get(cleanName);

        if (indexes == null || indexes.isEmpty()) {
            throw new IllegalArgumentException("Parameter not found: " + paramName);
        }

        return indexes.get(0);
    }

    public List<Integer> getParameterIndexes(String paramName) {
        String cleanName = paramName.startsWith(":") ? paramName.substring(1) : paramName;

        List<Integer> indexes = parameterIndexMap.get(cleanName);

        if (indexes == null) {
            throw new IllegalArgumentException("Parameter not found: " + paramName);
        }

        return new ArrayList<>(indexes);
    }

    public Set<String> getParameterNames() {
        return new LinkedHashSet<>(parameterIndexMap.keySet());
    }

    public int getParameterCount() {
        return parameterIndexMap.size();
    }

    public int getTotalPlaceholderCount() {
        return parameterIndexMap.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public boolean hasParameter(String paramName) {
        String cleanName = paramName.startsWith(":") ? paramName.substring(1) : paramName;
        return parameterIndexMap.containsKey(cleanName);
    }
}

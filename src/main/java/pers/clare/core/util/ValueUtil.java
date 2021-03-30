package pers.clare.core.util;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ValueUtil {

    /**
     * 如果 value 是 null 則是 then
     *
     * @param value
     * @param then
     * @return
     */
    public static String isNullThen(String value, String then) {
        if (value == null) {
            return then;
        } else {
            return value;
        }
    }

    /**
     * 取得陣列第一個值
     *
     * @param values
     * @return
     */
    public static <T> T first(T[] values) {
        return first(values, null);
    }

    /**
     * 取得陣列第一個值.
     *
     * @param <T>    the generic type
     * @param values the values
     * @param then   the then
     * @return the t
     */
    public static <T> T first(T[] values, T then) {
        if (values == null || values.length == 0) {
            return then;
        } else {
            return values[0];
        }
    }

    /**
     * 將 Map<String,String[]> 轉換成 Map<String,String>
     *
     * @param map
     * @return
     */
    public static Map<String, String> convert(Map<String, String[]> map) {
        if (map == null) {
            return null;
        } else {
            Map<String, String> newMap = new HashMap<String, String>();
            Set<Entry<String, String[]>> entrys = map.entrySet();
            for (Entry<String, String[]> entry : entrys) {
                if (entry.getValue() == null) {
                    newMap.put(entry.getKey(), null);
                } else {
                    newMap.put(entry.getKey(), String.join(",", entry.getValue()));
                }
            }
            return newMap;
        }
    }

    public static String toQueryParams(Map<String, String> params) {
        if (params == null || params.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : params.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue() + "&");
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    /**
     * 解析 queryParams
     *
     * @param queryParams
     * @return
     */
    public static Map<String, String> parseQueryParams(String queryParams) {
        Map<String, String> map = new HashMap<String, String>();
        if (StringUtils.isEmpty(queryParams)) return map;
        char[] cs = queryParams.toCharArray();
        char c;
        String keyStr;
        String oldValue;
        StringBuilder key = new StringBuilder();
        StringBuilder value;
        for (int i = 0, l = cs.length; i < l; i++) {
            c = cs[i];
            switch (c) {
                case '&':
                    if (key.length() == 0) break;
                    i++;
                    for (; i < l; i++) {
                        c = cs[i];
                        if (c == '=') {
                            break;
                        }
                        key.append(c);
                    }
                    break;
                case '=':
                    value = new StringBuilder();
                    i++;
                    for (; i < l; i++) {
                        c = cs[i];
                        if (c == '&') {
                            break;
                        }
                        value.append(c);
                    }
                    keyStr = key.toString();
                    oldValue = map.get(keyStr);
                    if (oldValue == null) {
                        map.put(keyStr, value.toString());
                        key = new StringBuilder();
                    } else {
                        map.put(keyStr, oldValue + "," + value.toString());
                        key = new StringBuilder();
                    }
                    break;
                default:
                    key.append(c);
            }
        }
        if (key.length() > 0) {
            keyStr = key.toString();
            oldValue = map.get(keyStr);
            if (oldValue == null) {
                map.put(keyStr, "");
            } else {
                map.put(keyStr, oldValue + ",");
            }
        }
        return map;
    }


    /**
     * 解析 queryParams
     *
     * @param queryParams
     * @return
     */
    public static Map<String, String> parseQueryParamsN(String queryParams) {
        Map<String, String> map = new HashMap<String, String>();
        if (StringUtils.isEmpty(queryParams)) return map;
        char[] cs = queryParams.toCharArray();
        char c;
        String keyStr;
        String oldValue;
        StringBuilder key = new StringBuilder();
        StringBuilder value;
        for (int i = 0, l = cs.length; i < l; i++) {
            c = cs[i];
            switch (c) {
                case '\n':
                    if (key.length() == 0) break;
                    i++;
                    for (; i < l; i++) {
                        c = cs[i];
                        if (c == '=') {
                            break;
                        }
                        key.append(c);
                    }
                    break;
                case '=':
                    value = new StringBuilder();
                    i++;
                    for (; i < l; i++) {
                        c = cs[i];
                        if (c == '\n') {
                            break;
                        }
                        value.append(c);
                    }
                    keyStr = key.toString();
                    oldValue = map.get(keyStr);
                    if (oldValue == null) {
                        map.put(keyStr, value.toString());
                        key = new StringBuilder();
                    } else {
                        map.put(keyStr, oldValue + "," + value.toString());
                        key = new StringBuilder();
                    }
                    break;
                default:
                    key.append(c);
            }
        }
        if (key.length() > 0) {
            keyStr = key.toString();
            oldValue = map.get(keyStr);
            if (oldValue == null) {
                map.put(keyStr, "");
            } else {
                map.put(keyStr, oldValue + ",");
            }
        }
        return map;
    }
}

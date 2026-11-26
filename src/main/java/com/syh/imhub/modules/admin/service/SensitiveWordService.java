package com.syh.imhub.modules.admin.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SensitiveWordService {

    private final Node root = new Node();


    public record FilterResult(boolean blocked, String text, List<String> hits) {
    }
    public FilterResult filter(String text) {
        // 空文本直接放行，避免后续按位置取字符时越界
        if (text == null || text.isEmpty()) {
            // 返回放行结果：文本原样、无命中
            return new FilterResult(false, text, List.of());
        }
        // 从当前根节点开始匹配（整棵树只读，可安全并发使用）
        Node node = root;
        // 复制一份可变文本，action=2 的替换直接改它，不修改入参
        StringBuilder out = new StringBuilder(text);
        // 命中词 → 命中次数；最终只返回词面集合，重复出现只算一次
        Map<String, Integer> hits = new HashMap<>();
        // 是否出现拦截级词（action=1），一旦为 true 整条消息不可发
        boolean blocked = false;
        // 匹配窗口起始下标，命中后跳到命中词之后
        int i = 0;
        // 外层循环：逐个位置尝试匹配，直到文本末尾
        while (i < text.length()) {
            // 每次尝试都从根节点出发，重新起一个匹配窗口
            Node current = node;
            // 内层探测指针，与窗口起点 i 相同
            int j = i;
            // 命中的结束下标，-1 表示该窗口没有任何词命中
            int matchedEnd = -1;
            // 命中词的处置动作（1／2／3）
            int matchedAction = 0;
            // 命中词的原词面
            String matchedWord = null;
            // 内层循环：沿字符链尽量深地走，取最长匹配
            while (j < text.length()) {
                // 按当前字符取子节点，走不下去就结束本次窗口
                current = current.children.get(text.charAt(j));
                // 路径断了：该窗口不再可能命中更长的词
                if (current == null) {
                    // 跳出内层循环，交由下方判断本窗口是否已命中
                    break;
                }
                // 当前节点是某个词的结尾，先记下来再继续往下找更长的
                if (current.end) {
                    // 暂存命中结束下标（更深节点会覆盖它，从而取最长词）
                    matchedEnd = j;
                    // 暂存该命中词的处置动作
                    matchedAction = current.action;
                    // 暂存该命中词的原词面
                    matchedWord = current.word;
                }
                // 指针右移，继续尝试更长的词
                j++;
            }
            // 该窗口至少命中一个敏感词
            if (matchedEnd >= 0) {
                // 命中计数 +1（只用于去重，返回的是词面集合）
                hits.merge(matchedWord, 1, Integer::sum);
                // action=1：拦截级词，标记整条消息不可发送
                if (matchedAction == 1) {
                    // 置拦截标记；文本继续扫描，hits 要尽量收集全
                    blocked = true;
                    // action=2：替换级词，命中区间逐字符打码
                } else if (matchedAction == 2) {
                    // 遍历命中区间 [i, matchedEnd]
                    for (int k = i; k <= matchedEnd; k++) {
                        // 原地把该位置字符改成星号（长度不变，下标不受影响）
                        out.setCharAt(k, '*');
                    }
                }
                // 跳到命中词之后继续扫描，避免重叠重复命中
                i = matchedEnd + 1;
                // 该窗口没有命中：窗口右移一位，换个起点重试
            } else {
                // 起点右移一个字符
                i++;
            }
        }
        // 返回是否拦截、替换后的文本（无命中／action=3 时与原文相同）、命中词去重列表
        return new FilterResult(blocked, out.toString(), List.copyOf(hits.keySet()));

    }

    // DFA 节点：一个字符对应一个节点，从根到 end=true 的节点构成一个敏感词。
    // 构建期会写 children，构建完成后只读，因此用普通 HashMap 即可。
    private static final class Node {
        // 子节点表：字符 → 下一个节点（构建完成后只读）
        private final Map<Character, Node> children = new HashMap<>();
        // 是否为某个敏感词的结尾
        private boolean end;
        // 该词的处置动作，默认 1（拦截）
        private int action = 1;
        // 该词原词面，命中时回传给上层
        private String word;
    }
}

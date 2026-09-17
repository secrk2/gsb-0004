package cn.gov.sfj.jiaowutong.util;

import com.github.promeg.pinyinhelper.Pinyin;

/**
 * 姓名脱敏：取姓名各字拼音首字母组成缩写，再拼矫正编号尾号。
 * 张伟 + JWT2026-0007 -> ZW·0007
 */
public final class NameMasker {

    private NameMasker() {
    }

    public static String mask(String fullName, String code) {
        StringBuilder initials = new StringBuilder();
        for (char c : fullName.toCharArray()) {
            if (Pinyin.isChinese(c)) {
                String pinyin = Pinyin.toPinyin(c);
                if (pinyin != null && !pinyin.isEmpty()) {
                    initials.append(Character.toUpperCase(pinyin.charAt(0)));
                }
            } else if (Character.isLetterOrDigit(c)) {
                initials.append(Character.toUpperCase(c));
            }
        }
        if (initials.length() == 0) {
            initials.append("**");
        }
        String tail = code;
        int dash = code.lastIndexOf('-');
        if (dash >= 0 && dash < code.length() - 1) {
            tail = code.substring(dash + 1);
        }
        return initials + "·" + tail;
    }
}

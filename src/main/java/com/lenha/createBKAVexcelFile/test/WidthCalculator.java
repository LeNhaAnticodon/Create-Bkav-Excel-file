package com.lenha.createBKAVexcelFile.test;

public class WidthCalculator {
    private WidthCalculator() {}

    // Kiểm tra combining mark bằng type và bằng bảng combining (nếu cần mở rộng)
    private static boolean isCombining(int cp) {
        int type = Character.getType(cp);
        return type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK;
    }

    // Halfwidth Katakana range: U+FF61 .. U+FF9F
    private static boolean isHalfwidthKatakana(int cp) {
        return cp >= 0xFF61 && cp <= 0xFF9F;
    }

    // Fullwidth alphanumeric ranges:
    // Digits: U+FF10..U+FF19
    // Uppercase: U+FF21..U+FF3A
    // Lowercase: U+FF41..U+FF5A
    private static boolean isFullwidthAlnum(int cp) {
        return (cp >= 0xFF10 && cp <= 0xFF19)  // fullwidth digits
                || (cp >= 0xFF21 && cp <= 0xFF3A)  // fullwidth A-Z
                || (cp >= 0xFF41 && cp <= 0xFF5A); // fullwidth a-z
    }

    // Kiểm tra full-width chung (CJK, Hiragana, full Katakana, fullwidth forms, Hangul, ideographic space)
    private static boolean isFullwidthGeneral(int cp) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
        if (block == null) return false;

        // Ideographic space U+3000
        if (cp == 0x3000) return true;

        return block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA       // fullwidth Katakana
                || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS // cần kiểm tra chi tiết bên dưới
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    /**
     * Tính tổng độ rộng theo yêu cầu:
     * - combining marks -> 0
     * - fullwidth alphanumeric -> 2
     * - fullwidth CJK/Hiragana/Katakana/Hangul/ideographic space -> 2
     * - halfwidth katakana -> 1
     * - others -> 1
     */
    public static int computeWidth(String s) {
        if (s == null || s.isEmpty()) return 0;
        int total = 0;
        int i = 0, len = s.length();
        while (i < len) {
            int cp = s.codePointAt(i);

            // combining mark -> 0
            if (isCombining(cp)) {
                // không cộng, chỉ bỏ qua
            }
            // fullwidth alphanumeric explicit -> 2
            else if (isFullwidthAlnum(cp)) {
                total += 2;
            }
            // halfwidth katakana -> 1
            else if (isHalfwidthKatakana(cp)) {
                total += 1;
            }
            // general fullwidth blocks (bao gồm HALFWIDTH_AND_FULLWIDTH_FORMS nhưng đã xử lý alnum & half-kana) -> 2
            else if (isFullwidthGeneral(cp)) {
                // Note: HALFWIDTH_AND_FULLWIDTH_FORMS includes both half- and full-width characters.
                // We already handled halfwidth katakana and fullwidth alnum above.
                // For safety: if block is HALFWIDTH_AND_FULLWIDTH_FORMS and cp is not one of those,
                // treat as fullwidth (2).
                total += 2;
            }
            // default -> 1
            else {
                total += 1;
            }

            i += Character.charCount(cp);
        }
        return total;
    }

    // Demo
    public static void main(String[] args) {
        String[] tests = {
                "abc",                  // 3
                "あいう",               // 6
                "漢字",                 // 4
                "aあb漢c",              // 1+2+1+2+1 = 7
                "e\u0301",              // e + combining -> combining = 0 => 1
                "０１２３ＡＢＣａｂｃ", // fullwidth alnum => each 2
                "ｶＡAA23456789012345678901234567890123456",
                "3 82943ＡＡＡＡＡＡ森ＡＡＡＡＡＡＡ",
                "ｱｲｳ",                 // half-width katakana (if in FFxx range) => each 1
                "👍",                   // emoji -> treated as other => 1
                "🇯🇵",                  // flags are regional indicators -> each RI counted as 1 => total 2
                "?",                   // replacement -> 1
                "　"                    // ideographic space U+3000 -> 2
        };

        for (String t : tests) {
            System.out.printf("%s -> width=%d%n", t, computeWidth(t));
        }
    }
}

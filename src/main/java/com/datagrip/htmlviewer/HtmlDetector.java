package com.datagrip.htmlviewer;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlDetector {

    private static final Pattern HTML_PATTERN = Pattern.compile(
        "(?i)<\\s*(html|head|body|div|p|span|table|tr|td|th|ul|ol|li|h[1-6]|iframe|svg|form|a|header|footer|section|article|style|script|b|i|strong|em|img|blockquote)\\b[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DOCTYPE_PATTERN = Pattern.compile("(?i)<!DOCTYPE\\s+html");
    private static final Pattern HEAD_PATTERN = Pattern.compile("(?i)<head[^>]*>");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?i)<html[^>]*>");

    public static boolean isHtml(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String trimmed = content.trim();

        if (DOCTYPE_PATTERN.matcher(trimmed).find()) {
            return true;
        }

        if (HTML_PATTERN.matcher(trimmed).find()) {
            return true;
        }

        // Check if escaped HTML e.g. &lt;div&gt;
        if (trimmed.contains("&lt;") && trimmed.contains("&gt;")) {
            String unescaped = unescapeHtml(trimmed);
            return HTML_PATTERN.matcher(unescaped).find();
        }

        return false;
    }

    public static String prepareHtmlForRendering(String rawContent) {
        if (rawContent == null) return "";

        String html = rawContent.trim();

        // If escaped HTML, unescape it
        if (html.contains("&lt;") && html.contains("&gt;") && !html.contains("<html>") && !html.contains("<body")) {
            html = unescapeHtml(html);
        }

        // Generate dynamic nonce so only our internal link-prevention script runs while untrusted scripts are blocked
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String cspMeta = "<meta http-equiv=\"Content-Security-Policy\" content=\"script-src 'nonce-" + nonce + "'; default-src * 'unsafe-inline' data: blob:;\">";

        // Disables clicks on links while preserving mouse events and hand cursor on hover
        String linkPreventionScript = "<script nonce=\"" + nonce + "\">\n" +
            "  document.addEventListener('click', function(e) {\n" +
            "    var a = e.target.closest('a');\n" +
            "    if (a) {\n" +
            "      e.preventDefault();\n" +
            "      e.stopPropagation();\n" +
            "    }\n" +
            "  }, true);\n" +
            "</script>";

        String linkCursorCss = "a, a * { cursor: pointer !important; }\n";

        boolean hasDocumentTags = html.toLowerCase().contains("<html") || html.toLowerCase().contains("<body");

        if (!hasDocumentTags) {
            String defaultCss = "body { background-color: #ffffff; color: #222222; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; padding: 16px; margin: 0; }\n" +
                                " a { color: #0969da; }\n" +
                                linkCursorCss +
                                " code, pre { background-color: #f6f8fa; padding: 2px 4px; border-radius: 4px; }\n" +
                                " table { border-collapse: collapse; width: 100%; } th, td { border: 1px solid #ddd; padding: 8px; }";

            html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  " + cspMeta + "\n" +
                "  <style>\n" + defaultCss + "\n</style>\n" +
                "  " + linkPreventionScript + "\n" +
                "</head>\n" +
                "<body>\n" +
                html + "\n" +
                "</body>\n" +
                "</html>";
        } else {
            // Full HTML document: inject CSP meta tag, link hover cursor styling, and click prevention script
            String injection = "\n  " + cspMeta + "\n  <style>\n    " + linkCursorCss + "  </style>\n  " + linkPreventionScript + "\n";
            Matcher headMatcher = HEAD_PATTERN.matcher(html);
            if (headMatcher.find()) {
                int insertPos = headMatcher.end();
                html = html.substring(0, insertPos) + injection + html.substring(insertPos);
            } else {
                Matcher htmlMatcher = HTML_TAG_PATTERN.matcher(html);
                if (htmlMatcher.find()) {
                    int insertPos = htmlMatcher.end();
                    html = html.substring(0, insertPos) + "\n<head>" + injection + "</head>" + html.substring(insertPos);
                } else {
                    html = "<head>" + injection + "</head>\n" + html;
                }
            }
        }

        return html;
    }

    public static String unescapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&nbsp;", " ");
    }
}

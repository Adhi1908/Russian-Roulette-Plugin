package com.example.russianroulette.utils;

import org.bukkit.ChatColor;

/**
 * Utility class for message formatting.
 */
public class MessageUtils {

    /**
     * Translate color codes in a string.
     * Converts '&' color codes to '§' codes.
     * 
     * @param text Text to colorize
     * @return Colorized text
     */
    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Strip all color codes from a string.
     * 
     * @param text Text to strip
     * @return Text without color codes
     */
    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }

    /**
     * Create a centered message for chat.
     * 
     * @param message Message to center
     * @return Centered message
     */
    public static String centerMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        final int CENTER_PX = 154;
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                int charWidth = getCharWidth(c, isBold);
                messagePxSize += charWidth;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = 4;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();

        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }

        return sb.toString() + message;
    }

    /**
     * Get the pixel width of a character.
     */
    private static int getCharWidth(char c, boolean isBold) {
        int width;
        switch (c) {
            case ' ':
                width = 4;
                break;
            case 'i':
            case ':':
            case '.':
            case ',':
            case ';':
            case '!':
            case '\'':
                width = 2;
                break;
            case 'l':
            case '|':
                width = 3;
                break;
            case 't':
            case 'I':
            case '[':
            case ']':
            case '"':
                width = 4;
                break;
            case 'f':
            case 'k':
            case '<':
            case '>':
            case '{':
            case '}':
            case '(':
            case ')':
            case '*':
                width = 5;
                break;
            case '@':
            case '~':
                width = 7;
                break;
            default:
                width = 6;
                break;
        }
        return isBold ? width + 1 : width;
    }

    /**
     * Format a time in seconds to MM:SS format.
     * 
     * @param seconds Total seconds
     * @return Formatted time string
     */
    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * Create a progress bar string.
     * 
     * @param current Current value
     * @param max     Maximum value
     * @param length  Bar length in characters
     * @param filled  Filled character
     * @param empty   Empty character
     * @return Progress bar string
     */
    public static String createProgressBar(int current, int max, int length, char filled, char empty) {
        int filledCount = (int) ((double) current / max * length);
        int emptyCount = length - filledCount;

        StringBuilder bar = new StringBuilder();
        bar.append("§a");
        for (int i = 0; i < filledCount; i++) {
            bar.append(filled);
        }
        bar.append("§7");
        for (int i = 0; i < emptyCount; i++) {
            bar.append(empty);
        }

        return bar.toString();
    }
}

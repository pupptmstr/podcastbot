package com.pupptmstr.podcastbot;


public class AdminMessageParser {

    private String unparsedText;
    private String error, messageType, contentType, messageText;

    AdminMessageParser(String unparsedText) {
        this.unparsedText = unparsedText;
    }

    private void parse() {
        String[] parsedText = unparsedText.split("=");
        error = parsedText[0];
        messageType = parsedText[1].replaceAll("\n", "");
        contentType = parsedText[2].replaceAll("\n", "");
        messageText = parsedText[3];
    }

    void checkMatchingAndParse() {
        String[] parsedText = unparsedText.split("=");
        if (parsedText.length == 4) {
            parse();
        } else error = "error";
    }

    String getMessageText() {
        return messageText;
    }

    String getMessageType() {
        return messageType;
    }

    String getContentType() {
        return contentType;
    }

    String getError() {
        return error;
    }
}
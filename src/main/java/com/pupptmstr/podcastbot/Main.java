package com.pupptmstr.podcastbot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.*;


public class Main {

    private static final ArrayList<String> vol = new ArrayList<>();//массив выпусков подкастов
    private static final ArrayList<String> volNames = new ArrayList<>();//массив названий выпусков
    private static final String key = "715659612:AAEaKyF03jhtE7fcVU3J8661xuZYpwQT1U4";

    private static String PROXY_HOST = "rocketparty.app";//хост прокси
    private static Integer PROXY_PORT = 1080;
    private static String PROXY_USER = "trytoguesstheusername";//юзернейм для авторизации в прокси
    private static String PROXY_PASSWORD = "tipidorda";//пароль для авторизации в прокси

    public static void main(String[] args) {
        try {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(new File("audioIds.txt")));
            int i = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" : ");
                vol.add(i, parts[0]);
                volNames.add(i, parts[1]);
                i++;
            }
            reader.close();
            System.out.println("Выпуски прочитаны и добавлены в массив..");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(PROXY_USER, PROXY_PASSWORD.toCharArray());
                }
            });


            ApiContextInitializer.init();
            TelegramBotsApi botsApi = new TelegramBotsApi();

            DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);

            botOptions.setProxyHost(PROXY_HOST);
            botOptions.setProxyPort(PROXY_PORT);
            botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);

            botsApi.registerBot(new PodcastBot(botOptions));
            System.out.println("Бот зарегестрирован...");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    /**package-private*/
    static int getNumOfEpisodes() {
        return vol.size();
    }

    /**package-private*/
    static String getEpisode(int numOfEpisode) {
        if (numOfEpisode <= getNumOfEpisodes())
            return vol.get(numOfEpisode - 1);

        return "Error";
    }

    /**package-private*/
    static String getKey() {
        return key;
    }

    /**package-private*/
    static String getNameOfEpisode(int numOfEpisode) {
        if (numOfEpisode <= getNumOfEpisodes())
            return volNames.get(numOfEpisode - 1);
        return "Error";
    }

    static void refreshVols() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("audioIds.txt")));
            int i = 0;
            String line;
            volNames.clear();
            vol.clear();
            while ((line = reader.readLine()) != null) {
                if (i+1 > getNumOfEpisodes()) {
                    String[] parts = line.split(" : ");
                    vol.add(i, parts[0]);
                    volNames.add(i, parts[1]);
                    i++;
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
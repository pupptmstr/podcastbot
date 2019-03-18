package com.pupptmstr.podcastbot;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.ArrayList;

public class PodcastBot extends TelegramLongPollingBot {

    PodcastBot(DefaultBotOptions botOptions) {
        super(botOptions);
    }

    private static final String BLACKLIST = "blacklist.txt";
    private static final String ADMINS = "admins.txt";
    private static final String VOLS_SUBS = "volsSubs.txt";
    private static final String NEWS_SUBS = "newsSubs.txt";

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            logUsersMessage(update);
            if (update.getMessage().getText().charAt(0) == '/') {
                commandChecker(update);
            } else if(update.getMessage().getText().charAt(0) == '='){
                wait(update);
                messageFormatChecker(update);
            } else phraseChecker(update);

        } else if (update.hasMessage()
                && (update.getMessage().getAudio() != null)
                && isAdmin(update.getMessage().getChatId().toString())) {
            logEvent("Админ прислал аудиозапись, отправляю ему её id");
            String audioId = update.getMessage().getAudio().getFileId();
            SendAudio msg = new SendAudio()
                    .setChatId(update.getMessage().getChatId())
                    .setCaption(audioId)
                    .setAudio(audioId);
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                logEvent(e.getLocalizedMessage());
            }
        } else
            sorry(update);
    }


    @Override
    public String getBotUsername() {
        return "EnterIntoItBot";
    }

    @Override
    public String getBotToken() {
        return Main.getKey();
    }

    //-------------------------------------Чекеры----------------------------------
    private void commandChecker(Update update) {
        switch(update.getMessage().getText()) {
            case "/start":
                hello(update);
                break;
            case "/help":
                help(update);
                break;
            case "/links":
                links(update);
                break;
            case "/allVolumes":
                wait(update);
                allVolumes(update);
                break;
            case "/volumesList":
                volumesList(update);
                break;
            case "/news":
                wait(update);
                subsAction(update, 1);
                break;
            case "/vols":
                wait(update);
                subsAction(update, 2);
                break;
            case "/sub":
                wait(update);
                subsAction(update, 0);
                break;
            //Блок с админскими командами
            case "/status":
                wait(update);
                if (isAdmin(update.getMessage().getChatId().toString())) {
                    statusAdmin(update);
                }
                else {
                    statusSub(update);
                }
                break;
            case "/admin":
                if (isAdmin(update.getMessage().getChatId().toString()))
                    sendAdminCommands(update);
                break;
            case "/reply":
                if (isAdmin(update.getMessage().getChatId().toString())) {
                    adminReplyMessageFormat(update);
                } else {
                    replyMessageSender(update, "Ты не админ, брат.\nНе используй эту команду");
                }
                break;
            case "/distribution":
                if (isAdmin(update.getMessage().getChatId().toString())) {
                    distributionMessageFormat(update);
                } else {
                    replyMessageSender(update, "Ты не админ, брат.\nНе используй эту команду");
                }
                break;
            case "/ban":
                if (isAdmin(update.getMessage().getChatId().toString()))
                    banMessageFormat(update);
                else
                    replyMessageSender(update, "Ты не админ, брат.\nНе используй эту команду");
                break;
            case "/addVol":
                if (isAdmin(update.getMessage().getChatId().toString())){
                    addVolMessageFormat(update);
                } else
                    replyMessageSender(update, "Ты не админ, брат.\nНе используй эту команду");
                break;
            default:
                matcherChecker(update);
        }
    }

    private void messageFormatChecker(Update update) {
        AdminMessageParser messageParser = new AdminMessageParser(update.getMessage().getText());
        messageParser.checkMatchingAndParse();
        if (!messageParser.getError().equals("error")) {
            switch (messageParser.getMessageType()) {
                case "dis":
                    if(isAdmin(update.getMessage().getChatId().toString())) {
                        switch (messageParser.getContentType()) {
                            case "news":
                                distributionMessageSender(update, 1, messageParser.getMessageText());
                                break;
                            case "episode":
                                distributionMessageSender(update, 2, messageParser.getMessageText());
                                distributionAudioSender(update);
                                break;
                            case "all":
                                distributionMessageSender(update, 1, messageParser.getMessageText());
                                distributionMessageSender(update, 2, messageParser.getMessageText());
                                break;
                            default:
                                replyMessageSender(update, "Неверный тип содержания.");
                        }
                    } else
                        replyMessageSender(update, "Простите, у вас нет прав администратора.");
                    break;
                case "rep":
                    if (isAdmin(update.getMessage().getChatId().toString())) {
                        if (messageParser.getContentType().length() == 9) {
                            adminReplyMessageSender(update,
                                    messageParser.getContentType(),
                                    messageParser.getMessageText());
                        } else
                            replyMessageSender(update, "Неверный chatId");
                    } else
                        replyMessageSender(update, "Простите, у вас нет прав администратора");
                    break;
                case "ban":
                    if (isAdmin(update.getMessage().getChatId().toString())){
                        if (messageParser.getContentType().length() == 9) {
                            if (!isBaned(messageParser.getContentType()))
                                ban(update, messageParser.getContentType());
                            else
                                unBan(update, messageParser.getContentType());
                        } else
                            replyMessageSender(update, "Неверный chatId");
                    }
                    break;
                case "addv":
                    if (isAdmin(update.getMessage().getChatId().toString())) {
                        if (isNewVol(messageParser.getContentType()))
                            addVol(update, messageParser.getContentType(), messageParser.getMessageText());
                        else
                            replyMessageSender(update, "Данный выпуск уже добавлен.");
                    }
                    break;
                default:
                    replyMessageSender(update, "Ошибка в типе сообщения, попробуйте еще раз.");

            }
        } else
            replyMessageSender(update, "Неверный формат сообщения, исправте и попробуйте еще раз.");

    }

    private void matcherChecker(Update update) {
        if (update.getMessage().getText().matches("/vol_\\d+")) {
            vol(update, Integer.parseInt(update.getMessage().getText().substring(5)));
        } else if (update.getMessage().getText().matches("/contact\\s.+")) {
            requestForAdminsMessageSender(update, update.getMessage().getText().split(" ", 2)[1]);
        } else if (update.getMessage().getText().equals("/contact")) {
            replyMessageSender(update, "Чтобы отправить админам сообщение наберите эту команду, " +
                    "и затем ваш текст в одном сообщении.\n" +
                    "Пример: ");
            replyMessageSender(update, "/contact Админы, помогите");
        } else if (update.getMessage().getText().matches("/discussion\\s.+")) {
            themeForAdminMessageSender(update, update.getMessage().getText().split(" ", 2)[1]);
        } else if (update.getMessage().getText().equals("/discussion")) {
            replyMessageSender(update, "Чтобы предложить тему для обсуждения её в выпуске наберите эту команду," +
                    "и затем текст самой темы в одном сообщении.\n" +
                    "Пример:");
            replyMessageSender(update, "/discussion Обсудите в выпуске, что JS - лучший яп");
        } else {
            replyMessageSender(update, "Вау, комада! Сейчас я её ка-а-ак выполню!" +
                    "\n...\nНичего не вышло. Жаль.\n" +
                    "Исправте команду и попробуйте еще раз");
        }

    }

    private void phraseChecker(Update update) {
        switch (update.getMessage().getText().toLowerCase()) {
            case "спасибо":
                replyMessageSender(update, "всегда пожалуйста");
                break;
            case "привет":
                replyMessageSender(update, "Дарова)\nЛадно, шучу");
                hello(update);
                break;
            default:
                missunderstand(update);
        }
    }
    //-----------------------------------------------------------------------------




    //-------------------------------------Сендеры---------------------------------
    private void replyMessageSender(Update update, String text) {
        SendMessage message = new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setText(text);
        try {
            execute(message);
            logBotsMessage(message.getText());
        } catch (TelegramApiException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
    }

    //только текстовые ответы
    private void adminReplyMessageSender(Update update, String destinationChatId, String messageText){
        SendMessage message = new SendMessage()
                .setChatId(destinationChatId)
                .setText("Вам поступил ответ от админа:" +
                        update.getMessage().getFrom().getFirstName() +
                        " (" + update.getMessage().getFrom().getUserName() + ")" +
                        "\n\n" +
                        "Ответ: " + messageText);
        try {
            execute(message);
        }catch (TelegramApiException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
        done(update);
        logEvent("Пользователю отправлен ответ от админа");
    }

    //1 - новость, 2 - новый эпизод
    private void distributionMessageSender(Update update, int typeOfMes, String messageText) {
        String fileName;
        BufferedReader reader;
        ArrayList<String> subs = new ArrayList<>();
        String line;

        if (typeOfMes == 1)
            fileName = NEWS_SUBS;
        else
            fileName = VOLS_SUBS;

        try {
            reader = new BufferedReader(new FileReader(new File(fileName)));
            while ((line = reader.readLine()) != null) {
                subs.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }

        for (String sub : subs) {
            SendMessage message = new SendMessage()
                    .setChatId(sub)
                    .setText(messageText);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                logEvent(e.getLocalizedMessage());
            }
        }
        replyMessageSender(update, "Выполнено...");
        logEvent("Осуществлена рассылка для " + fileName);
    }

    private void distributionAudioSender(Update update) {
        ArrayList<String> subs = new ArrayList<>();
        String line;

        try {
            File file = new File(VOLS_SUBS);
            FileReader fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);
            while ((line = reader.readLine()) != null) {
                subs.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }

        for (String sub : subs) {
            SendAudio message = new SendAudio()
                    .setChatId(sub)
                    .setAudio(Main.getEpisode(Main.getNumOfEpisodes()));
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                logEvent(e.getLocalizedMessage());
            }
        }
    }

    private void themeForAdminMessageSender(Update update, String text) {
        ArrayList<String> admins = new ArrayList<>();
        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(ADMINS)));
            while ((line = reader.readLine()) != null) {
                admins.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }

        for (String adm : admins) {
            SendMessage message = new SendMessage()
                    .setChatId(adm)
                    .setText("Предложение темы для обсуждения в выпуске:\n" +
                            "От "+ update.getMessage().getFrom().getFirstName() +
                            " (" + update.getMessage().getFrom().getUserName() + ")\n\n" +
                            "Предложенная тема: " + text);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                logEvent(e.getLocalizedMessage());
            }
        }
        done(update);
        logEvent("Пользователь " +
                update.getMessage().getFrom().getFirstName() + " - " +  update.getMessage().getFrom().getUserName() +
                " предложил тему: " + text);
    }

    private void requestForAdminsMessageSender(Update update, String text) {
        String line;
        ArrayList<String> admins = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(new File(ADMINS));
            BufferedReader reader = new BufferedReader(fileReader);
            while ((line = reader.readLine()) != null) {
                admins.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }

        for (String adm : admins) {
            SendMessage message = new SendMessage()
                    .setChatId(adm)
                    .setText("Пришел запрос:\n" +
                            text +
                            "\nАвтор:");
            SendMessage sendClientChatId = new SendMessage()
                    .setChatId(adm)
                    .setText(update.getMessage().getChatId().toString()
                            + "(" + update.getMessage().getFrom().getUserName() +
                            update.getMessage().getFrom().getFirstName() + ")");
            try {
                execute(message);
                execute(sendClientChatId);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                logEvent(e.getLocalizedMessage());
            }

        }
        done(update);
        logEvent("Пользователь " +
                update.getMessage().getFrom().getFirstName() + " - " +  update.getMessage().getFrom().getUserName() +
                " отправил сообщение: " + text);
    }
    //-----------------------------------------------------------------------------





    //-----------------------------Пользовательские команды------------------------
    private void help(Update update) {
        replyMessageSender(update,
                "Вот что еще я могу:\n" +
                        "/discussion - предложить тему для обсуждения в выпуске\n" +
                        "/contact - связаться с админами(отправить им сообщение)\n\n" +
                        "/vol_{№} - выслать выпуск c указанным номером\n" +
                        "Например /vol_2\n" +
                        "/volumesList - выслать полный список выпусков\n" +
                        "/allVolumes - выслать все выпуски по порядку\n\n" +
                        "/links - получить ссылки на все основные ресурсы проекта\n" +
                        "/vols - подписаться на рассылку новых выпусков\n" +
                        "/news - подписаться на новостную рассылку проекта\n" +
                        "/sub - полная подписка\n\n" +
                        "/status - проверка статуса своих подписок\n" +
                        "/help - помощь(это самое меню)\n" +
                        "Для отмены подписки используются те же команды, что и для её активации");
    }

    private void vol(Update update, int numOfPodcast) {
        if (numOfPodcast <= Main.getNumOfEpisodes()) {
            SendAudio audio = new SendAudio()
                    .setChatId(update.getMessage().getChatId())
                    .setAudio(Main.getEpisode(numOfPodcast));
            try {
                execute(audio);
                logEvent("Отправлен " + numOfPodcast + "й эпизод подкаста");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                logEvent(e.getLocalizedMessage());
            }
        } else
            replyMessageSender(update, "Данного выпуска нет в моей базе данных");
    }

    private void allVolumes(Update update) {
        for (int i = 0; i < Main.getNumOfEpisodes(); i++) {
            vol(update, i+1);
        }
    }

    private void volumesList(Update update) {
        StringBuilder builder = new StringBuilder();
        builder.append("Список всех эпизодов подкаста:\n\n");
        for (int i = 0; i < Main.getNumOfEpisodes(); i++) {
            builder
                    .append("/vol_")
                    .append(i + 1)
                    .append(" - ")
                    .append(Main.getNameOfEpisode(i + 1))
                    .append("\n");
        }
        String completedString = builder.toString();

        replyMessageSender(update, completedString);
    }

    //0 - полная подписка, 1 - новостная, 2 - эпизодическая
    private void subsAction(Update update, int typeOfSub) {
        switch (typeOfSub) {
            case 1:
                if (isSub(update, NEWS_SUBS))
                    deleteSub(update, NEWS_SUBS);
                else
                    addSub(update, NEWS_SUBS);
                break;
            case 2:
                if (isSub(update, VOLS_SUBS))
                    deleteSub(update, VOLS_SUBS);
                else
                    addSub(update, VOLS_SUBS);
                break;
            case 0:
                if (!isSub(update, VOLS_SUBS)){
                    if (!isSub(update, NEWS_SUBS))
                        addSub(update, NEWS_SUBS);
                    addSub(update, VOLS_SUBS);
                } else if (!isSub(update, NEWS_SUBS)){
                    if (!isSub(update, VOLS_SUBS))
                        addSub(update, VOLS_SUBS);
                    addSub(update, NEWS_SUBS);
                } else {
                    deleteSub(update, VOLS_SUBS);
                    deleteSub(update, NEWS_SUBS);
                }
                break;
        }
    }

    private void addSub(Update update, String fileName) {
        String subType;
        if (fileName.equals(NEWS_SUBS))
            subType = "новостная";
        else
            subType = "эпизодическая";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName), true));
            writer.write(update.getMessage().getChatId().toString());
            writer.newLine();
            writer.close();
            replyMessageSender(update, "Добавлена " + subType + " подписка...");
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }

    }

    private void deleteSub(Update update, String fileName) {
        String subType;
        if (fileName.equals(NEWS_SUBS))
            subType = "новостная";
        else
            subType = "эпизодическая";
        try {
            File file = new File(fileName);
            File tempFile = new File(fileName + ".tmp");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile, true));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.equals(update.getMessage().getChatId().toString())) continue;
                writer.write(line);
                writer.newLine();
            }
            writer.close();
            reader.close();
            if (file.delete()) {
                System.out.println("Удаление успешно...");
                if (tempFile.renameTo(file))
                    System.out.println("Переименование успешно...");
                else
                    System.out.println("Ошибка переименования...");
            } else
                System.out.println("Ошибка удаления...");

            replyMessageSender(update, "Удалена " + subType + " подписка...");
        } catch (Exception e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
    }

    private boolean isSub(Update update, String fileName) {
        boolean res = false;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(update.getMessage().getChatId().toString())){
                    res = true;
                    break;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
        return res;
    }

    private void statusSub(Update update) {
        String newsSub = "off", volsSub = "off", baned = "off";
        if (isSub(update, NEWS_SUBS))
            newsSub = "on";
        if(isSub(update, VOLS_SUBS))
            volsSub = "on";
        if(isBaned(update.getMessage().getChatId().toString()))
            baned = "on";

        replyMessageSender(update,  update.getMessage().getFrom().getFirstName() +
                "(" + update.getMessage().getFrom().getUserName() + ")" +
                "\nНовостная подписка: " + newsSub +
                "\nЭпизодическая подписка: " + volsSub +
                "\nБан общения с админами: " + baned);
    }

    private void links(Update update) {
        replyMessageSender(update,
                "Ссылки на основные ресурсы проекта:\n" +
                        "ВКонтакте - https://vk.com/zaity_v_it\n" +
                        "Anchor - https://anchor.fm/enterintoit\n" +
                        "Apple Podcasts - https://itunes.apple.com/us/podcast/зайдивайти/id1452..\n" +
                        "Google Podcasts - https://www.google.com/podcasts?feed=aHR0cHM6Ly9hbmNo..\n" +
                        "Spotify - https://open.spotify.com/show/1BVFydMftJAQwiZ2iljOih\n" +
                        "Breaker - https://www.breaker.audio/zaidivaiti\n" +
                        "Pocket Casts - https://pca.st/vGmC\n" +
                        "RadioPublic - https://radiopublic.com/-8X4mb1\n" +
                        "Я.Деньги - https://money.yandex.ru/to/410018746499141");
    }
    //-----------------------------------------------------------------------------



    //-----------------------------Админские команды-------------------------------
    private void sendAdminCommands(Update update){
        replyMessageSender(update, "Здравствуйте, господин админ!\n" +
                "Список админских команд:\n" +
                "/admin - получить список админских команд\n" +
                "/status - получить информацию о количестве подписчиков\n" +
                "/distribution - получить формат сообщения для рассылки его подписчикам\n" +
                "/reply - получить формат сообщения для ответа конкретному подписчику\n" +
                "/ban - получить формат сообщения для отправки подписчика в бан\n" +
                "/addVol - получить формат сообщения для добавления нового эпизода подкаста\n\n" +
                "Прошу вас заметить, что формат сообщения строгий и должен соблюдаться\n" +
                "Также у вас есть возмножность отправить мне аудиозапись и получить её файл-ид\n" +
                "Это пригодится вам для добавления новых эпизодов в мою бд");
    }

    private void statusAdmin(Update update) {
        int newsCounter = 0, volsCounter = 0, banedUser = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(NEWS_SUBS)));
            while (reader.readLine() != null)
                newsCounter++;
            reader.close();

            reader = new BufferedReader(new FileReader(new File(VOLS_SUBS)));
            while (reader.readLine() != null)
                volsCounter++;
            reader.close();

            reader = new BufferedReader(new FileReader(new File(BLACKLIST)));
            while (reader.readLine() != null)
                banedUser++;
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
        replyMessageSender(update, "Подписчики новостей: " + newsCounter +
                "\nПодписчики выпусков: " + volsCounter +
                "\nЗабанено юзеров: " + banedUser);
    }

    private void distributionMessageFormat(Update update){
        replyMessageSender(update, "Формат сообщения для рассылки:\n" +
                "=dis(тип сообщения - рассылка)\n" +
                "=news/episode/all(тип содержимого = тип рассылки)\n" +
                "=Текст сообщения самой рассылки\n" +
                "Допустимые типы рассылки:\n" +
                "news - новостная(разослать сообщение подписаным на новости)\n" +
                "episode - эпизодическая(разослать подписанным на новые выпуски)\n" +
                "all - разослать сообщение всем подписчикам\n" +
                "Для самой рассылки просто отправте сообщение нужного формата.\n\n" +
                "Пример:");
        replyMessageSender(update, "=dis\n" +
                "=all\n" +
                "=Внимание друзья! Это рассылка!");
    }

    private void adminReplyMessageFormat(Update update) {
        replyMessageSender(update, "Формат сообщения для ответа подписчику:\n" +
                "=rep(тип сообщения - ответ подписчику)\n" +
                "=000000000(тип содержания - chatId подписчика)\n" +
                "=Текст сообщения ответа\n" +
                "Для ответа просто отправте сообщение нужного формата.\n\n" +
                "Пример:");
        replyMessageSender(update,
                "=rep\n" +
                        "=299233972\n" +
                        "=Спасибо за ваше сообщение! Мой ответ бла бла бля");
    }

    private void banMessageFormat(Update update) {
        replyMessageSender(update, "Формат сообщения для отправки юзера в бан-лист:\n" +
                "=ban(тип сообщения - забанить юзера)\n" +
                "=0000000(тип содержания - chatId юзера)\n" +
                "=(текст - в данном случае не важно какой, " +
                "парсер сообщений настроен так, что тут должно быть хоть что-то)\n" +
                "Для того чтобы разбанить юзера необходимо еще раз отправить запрос на его бан\n\n" +
                "Пример:");
        replyMessageSender(update,
                "=ban\n" +
                        "=0000000\n" +
                        "=что угодно");
    }

    private void addVolMessageFormat(Update update) {
        replyMessageSender(update,"Формат сообщения для добавления нового выпуска подкаста:\n" +
                "=addv(тип сообщения - добавление нового выпуска)\n" +
                "={fileId}(тип содержания - file-id новыго выпуска)\n" +
                "=Название выпуска в формате:\n" +
                "{№}.Название серии(ч.{№} Тема выпуска)" +
                "Пример:");
        replyMessageSender(update, "=addv\n" +
                "=CQADAgAD-gMAAqrh0EpUwBp4EqwZ2QI\n" +
                "=1.Вводный выпуск(ч.1)");

    }

    private void ban(Update update, String chatId) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(BLACKLIST), true));
            writer.write(chatId);
            writer.newLine();
            writer.close();
            replyMessageSender(update, "Юзер " + chatId + " забанен...");
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
    }

    private void unBan(Update update, String chatId) {
        try {
            File file = new File(BLACKLIST);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            File tempFile = new File(BLACKLIST + ".tmp");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile, true));
            String line;
            while((line = reader.readLine()) != null) {
                if (line.equals(chatId)) continue;
                writer.write(line);
                writer.newLine();
            }
            writer.close();
            reader.close();

            if (file.delete()) {
                System.out.println("Удаление успешно.");
                if (tempFile.renameTo(file))
                    System.out.println("Переименование успешно.");
                else
                    System.out.println("Ошибка переименования.");
            } else
                System.out.println("Ошибка удаления.");
            replyMessageSender(update, "Юзер " + chatId + " разбанен");

        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
    }

    private void addVol(Update update, String fileId, String nameOfEpisde) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("audioIds.txt"), true));
            writer.write(fileId + " : " + nameOfEpisde);
            writer.newLine();
            writer.close();
            replyMessageSender(update, "Новый выпуск добавлен.");
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
    }
    //-----------------------------------------------------------------------------



    //----------------------------Вспомогательные мессенджи------------------------
    private void sorry(Update update) {
        replyMessageSender(update, "Извините, мы пока можем отвечать только на текстовые сообщения");
    }

    private void hello(Update update) {
        replyMessageSender(update, "Здравствуй " + update.getMessage().getFrom().getFirstName() +
                "\nРады видеть вас здесь!\n" +
                "Я чат-бот проекта <<ЗайдиВАйТи>>\n" +
                "У нас есть Функция подписки,\n" +
                "благодаря которой вы не пропустите ни единого выпуска или новости\n" +
                "Для активации используйте:\n" +
                "/vols - подписаться на рассылку новых выпусков\n" +
                "/news - подписаться на новостную рассылку проекта\n" +
                "/sub - полная подписка\n" +
                "Для отмены подписки используются те же команды, что и для её активации");
        help(update);
    }

    private void missunderstand(Update update) {
        replyMessageSender(update, "Простите, возможно я вас не понимаю...\n" +
                "Это поможет нам лучше понимать друг друга:");
        help(update);
    }

    private void wait(Update update) {
        replyMessageSender(update, "Подождите, выполняю...");
    }

    private void done(Update update) {
        replyMessageSender(update, "Выполнено...");
    }
    //-----------------------------------------------------------------------------



    //-------------------------------Системные команды-----------------------------
    private void logUsersMessage(Update update) {

        String loggingMessage = "--------------------------------------------\n" +
                update.getMessage().getFrom().getFirstName() +
                "(" + update.getMessage().getFrom().getUserName() + " - " + update.getMessage().getChatId() + ")" +
                ": " + update.getMessage().getText() + "\n";
        System.out.println(loggingMessage);

        try {

            File file = new File("Log.txt");
            FileWriter fileReader = new FileWriter(file, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileReader);

            bufferedWriter.write(loggingMessage + "\n");
            bufferedWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void logBotsMessage(String messageText) {
        String loggingMessage = "--------------------------------------------\n" +
                "Bot: " + messageText + "\n";
        System.out.println(loggingMessage);

        try {

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File("Log.txt"), true));
            bufferedWriter.write(loggingMessage + "\n");
            bufferedWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logEvent(String event) {
        String loggingMessage = "--------------------------------------------\n" +
                event + "\n";
        System.out.println(loggingMessage);

        try {
            FileWriter fileWriter = new FileWriter(new File("Log.txt"), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(loggingMessage + "\n");
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isAdmin(String chatId) {
        boolean res = false;
        try{
            File file = new File(ADMINS);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;

            while((line = bufferedReader.readLine()) != null) {
                if (chatId.equals(line))
                    res = true;
            }
            bufferedReader.close();

        } catch (Exception e){
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
        return res;
    }

    private boolean isBaned(String chatId) {
        boolean res = false;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(BLACKLIST)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(chatId)) {
                    res = true;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }

        return res;
    }

    private boolean isNewVol(String fileId) {
        boolean res = true;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("audioIds.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.split(" : ")[0].equals(fileId))
                    res = false;
            }
            //reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            logEvent(e.getLocalizedMessage());
        }
        return res;
    }
    //-----------------------------------------------------------------------------

}
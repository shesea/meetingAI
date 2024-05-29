package spbu.meetingAI.util;

public enum OperationType {
    NONE("", ""),
    SUMMARY("summary", "Напиши краткое содержание текста от первого лица"),
    KEY_WORDS("key words", "Выдели до 10 ключевых слов и словосочетаний из текста"),
    DESCRIPTION("description", "Опиши полученный текст в одном предложении, ни за что не используй markdown"),
    TITLE("title", "Придумай короткое название для текста без кавычек"),
    QUOTES("quotes", "Выдели из текста от двух до четырех цитат длиной до 30 слов. Между всеми цитатами ставь ровно один перевод строки, нумеруй каждую цитату");

    private final String name;
    private final String command;

    OperationType(String name, String command) {
        this.name = name;
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }
}

package ua.ihromant;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SodStat {
    String url = "http://forum.heroesworld.ru/showthread.php?t=12881";

    @Test
    public void calculateTestStatistics() throws IOException {
        List<GameResult> results = collectGameResults(url);
        Map<Castle, Long> winRate = results.stream()
                .map(GameResult::getWinner)
                .filter(i -> i.getCastle() != null) // filter HotA
                .collect(Collectors.groupingBy(PlayerInfo::getCastle, Collectors.counting()));
        Map<Castle, Long> loseRate = results.stream()
                .map(GameResult::getLoser)
                .filter(i -> i.getCastle() != null) // filter HotA
                .collect(Collectors.groupingBy(PlayerInfo::getCastle, Collectors.counting()));
        new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/HotA.txt")))
                .lines()
                .forEach(line -> {
                    String[] spl = line.split(": ");
                    Castle c = Castle.parse(spl[0]);
                    String[] winLose = spl[1].split("/");
                    winRate.put(c, winRate.get(c) - Long.parseLong(winLose[0]));
                    loseRate.put(c, loseRate.get(c) - Long.parseLong(winLose[1]));
                });
        Stream.of(Castle.values())
                .sorted(Comparator.comparing(winRate::get).reversed())
                .forEach(c -> System.out.println(String.format("%s: %s/%s", c.getRussian(), winRate.get(c), loseRate.get(c))));
    }

    private List<GameResult> collectGameResults(String url) throws IOException {
        Document doc = Jsoup.parse(new URL(url), 10000);
        Elements records = doc.body().getElementById("collapseobj_tournament_reports")
                .child(1).child(0).child(0).child(0).child(1).child(0).children();
        List<GameResult> results = new ArrayList<GameResult>();
        for (Element el : records) {
            results.add(parse(el.child(0).text()));
        }
        return results;
    }

    private static GameResult parse(String text) {
        // TODO actually this is wrong, later needs to be improved by parsing subelements to extract nicknames
        int firstLeftBracket = text.indexOf('(');
        int firstRightBracket = text.indexOf(')');
        int lastLeftBracket = text.lastIndexOf('(');
        int lastRightBracket = text.lastIndexOf(')');
        String[] dateAndFirstPlayer = text.substring(0, firstLeftBracket - 1).split(" ", 2);
        String[] firstPlayerInfo = text.substring(firstLeftBracket + 1, firstRightBracket).split(", ");
        String[] resultAndSecondPlayer = text.substring(firstRightBracket + 2, lastLeftBracket - 1).split(" ", 2);
        String[] secondPlayerInfo = text.substring(lastLeftBracket + 1, lastRightBracket).split(", "); // TODO add template info
        PlayerInfo firstInfo = new PlayerInfo(dateAndFirstPlayer[1], Color.parse(firstPlayerInfo[0]),
                Castle.parse(firstPlayerInfo[1]), Hero.parse(firstPlayerInfo[2]), Hero.parse(firstPlayerInfo[3])); // TODO add random checking
        PlayerInfo secondInfo = new PlayerInfo(resultAndSecondPlayer[1], Color.parse(secondPlayerInfo[0]),
                Castle.parse(secondPlayerInfo[1]), Hero.parse(secondPlayerInfo[2]), Hero.parse(secondPlayerInfo[3]));
        if ("def".equals(resultAndSecondPlayer[0])) {
            return new GameResult(firstInfo, secondInfo);
        } else {
            return new GameResult(secondInfo, firstInfo);
        }
    }
}
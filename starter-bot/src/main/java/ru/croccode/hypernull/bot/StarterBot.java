package ru.croccode.hypernull.bot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

import javafx.util.Pair;
import ru.croccode.hypernull.domain.MatchMode;
import ru.croccode.hypernull.geometry.Offset;
import ru.croccode.hypernull.geometry.Point;
import ru.croccode.hypernull.io.SocketSession;
import ru.croccode.hypernull.message.Hello;
import ru.croccode.hypernull.message.MatchOver;
import ru.croccode.hypernull.message.MatchStarted;
import ru.croccode.hypernull.message.Move;
import ru.croccode.hypernull.message.Register;
import ru.croccode.hypernull.message.Update;
import ru.croccode.hypernull.reader.PropertiesReader;
import ru.croccode.hypernull.wavePropagation.WavePropagation;

import static ru.croccode.hypernull.map.MapGraph.*;

public class StarterBot implements Bot {

    private static final Random rnd = new Random(System.currentTimeMillis());

    private final Map<String, String> properties;

    private int numRounds, mapWidth, mapHeight, id, viewRadius, miningRadius, attackRadius;

    private char[][] myMap;

    public StarterBot(Map<String, String> properties) {
        this.properties = properties;
    }

    private MatchMode getMode() {
        return Objects.equals(properties.get("mode"), "FRIENDLY") ?
                MatchMode.FRIENDLY : MatchMode.DEATHMATCH;
    }

    private int getDirectionToByOneAxis(int a, int aTo) {
        return -Integer.signum(a - aTo);
    }

    private boolean isSecondNearer(Point me, Point first, Point second) { // true if second < first
        return getSquaredDistance(me, second) < getSquaredDistance(me, first);
    }

    private Offset goToTargetStupidOffset(Point me, Point to) {
        return new Offset(
                getDirectionToByOneAxis(me.x(), to.x()),
                getDirectionToByOneAxis(me.y(), to.y())
        );
    }

    private Offset getOptimalGoAwayOffset(Point myAbsolutePoint, Point fromAbsolutePoint) {
        int goFromX, goFromY;

        goFromX = -getDirectionToByOneAxis(myAbsolutePoint.x(), fromAbsolutePoint.x());
        goFromY = -getDirectionToByOneAxis(myAbsolutePoint.y(), fromAbsolutePoint.y());

        int[][] temp = new int[3][3]; // little direction field

        temp[1][1] = 999; // don't stay on one place

        int offsetX = 0, offsetY = 0;

        int min = 999;

        for (int y = -1; y <= 1; y++) { // перебор offsetY
            for (int x = -1; x <= 1; x++) { // перебор offsetX
                Point relatedPoint = getRelatedPoint(myAbsolutePoint, new Point(
                        myAbsolutePoint.x() + x,
                        myAbsolutePoint.y() + y
                ));

                if (isBlocked(relatedPoint)) {
                    temp[1 + x][1 + y] = 999;
                } else {
                    temp[1 + x][1 + y] = Math.abs(x - goFromX) + Math.abs(y - goFromY); // CHECK!
                    if (min > temp[1 + x][1 + y]) {
                        min = temp[1 + x][1 + y];
                        offsetX = x;
                        offsetY = y;
                    }
                }
            }
        }
        return new Offset(offsetX, offsetY);
    }

    @Override
    public Register onHello(Hello hello) {
        Register register = new Register();
        register.setMode(getMode());
        register.setBotName(properties.get("bot.name"));
        register.setBotSecret(properties.get("bot.secret"));
        return register;
    }

    @Override
    public void onMatchStarted(MatchStarted matchStarted) {
        // ничего не понятно
        numRounds = matchStarted.getNumRounds();
        mapWidth = matchStarted.getMapSize().width();
        mapHeight = matchStarted.getMapSize().height();
        id = matchStarted.getYourId();
        viewRadius = matchStarted.getViewRadius();
        miningRadius = matchStarted.getMiningRadius();
        attackRadius = matchStarted.getAttackRadius();
        // все ещё ничего не понятно
        initMap(viewRadius, mapWidth, mapHeight);
    }

    private Pair<Boolean, Offset> handleEnemy(Map<Integer, Point> absoluteBots, Map<Integer, Integer> botCoins,
                                              Point myAbsolutePoint, boolean goWar) { // goWar true -> fight; false -> goAway
        int myCoins = botCoins.get(id);

        Point enemyP = null;

        for (var idI:
                absoluteBots.keySet()) {

            if (id != idI && (enemyP == null || isSecondNearer(myAbsolutePoint, enemyP, absoluteBots.get(idI)))) {
                if ((goWar && botCoins.get(idI) < myCoins) ||
                        (!goWar && botCoins.get(idI) >= myCoins)){
                    enemyP = absoluteBots.get(idI);
                }
            }
        }

        if (enemyP != null) {
            return (goWar) ? new Pair<>(true, goToTargetStupidOffset(myAbsolutePoint, enemyP)) :
                    new Pair<>(true, getOptimalGoAwayOffset(myAbsolutePoint, enemyP));
        }
        else {
            return new Pair<>(false, new Offset(0, 0));
        }

    }

    private Pair<Boolean, Stack<Offset>> ifMoney(Set<Point> absoluteCoins, Point myAbsolutePoint) {
        // волновой алгоритм
        // возвращает true, если можем куда-то добраться
        // offset - первый шаг

        int[][] myMapCopy = createMyMapIntCopyWithoutCoins();

        boolean ansB = false;
        Stack<Offset> ansS = new Stack<>();

        int minLength = 999;

        if (absoluteCoins != null) {
            for (var coinAbsolutePoint :
                    absoluteCoins) {
                Point toRelatedPoint = getRelatedPoint(myAbsolutePoint, coinAbsolutePoint);
                Point myRelatedPoint = getRelatedPoint(myAbsolutePoint, myAbsolutePoint);

                WavePropagation wavePropagation = new WavePropagation(myRelatedPoint, toRelatedPoint, myMapCopy);

                if (wavePropagation.pathFound && wavePropagation.lengthPath < minLength) {
                    ansB = true;
                    minLength = wavePropagation.lengthPath;
                    ansS = wavePropagation.getAllSteps();
                }
            }
        }

        if (ansB) {
            return new Pair<>(true, ansS);
        } else {
            return new Pair<>(false, ansS); // ans0 -> new Stack<>();
        }

    }

    private boolean ifNothing(Map<Integer, Point> bots, Set<Point> coins) {
        return bots.size() <= 1 && coins == null;
    }

    private Point lastAbsolutePoint = new Point(0, 0);
    private int amountOfSleepingSteps = 0;

    private int amountOfStepsInTact = 0;

    private Stack<Offset> stuckList = new Stack<>();
    private Stack<Offset> sbrStack = new Stack<>();

    private int[][] createMyMapIntCopyWithoutCoins() {
        int[][] myMapCopy = new int[myMap.length][myMap[0].length];

        for (int i = 0; i < myMapCopy.length; i++) {
            for (int j = 0; j < myMapCopy[0].length; j++) {
                int temp = 0;
                if (myMap[i][j] == '#') temp = 999;

                myMapCopy[i][j] = temp;
            }
        }

        return myMapCopy;
    }

    private Stack<Offset> createInsaneAntiDancingOffset(Point myAbsolutePoint) {
        int[][] myMapCopy = createMyMapIntCopyWithoutCoins();

        List<Stack<Offset>> ways = new ArrayList<>();

        for(int i = 0; i < myMapCopy.length; i++) {
            for (int j = 0; j < myMapCopy[i].length; j++) {
                if (myMapCopy[i][j] != 999) {
                    Point toRelatedPoint = new Point(j, i);
                    Point myRelatedPoint = getRelatedPoint(myAbsolutePoint, myAbsolutePoint);

                    WavePropagation wavePropagation = new WavePropagation(myRelatedPoint, toRelatedPoint, myMapCopy);

                    if (wavePropagation.pathFound) {
                        ways.add(wavePropagation.getAllSteps());
                    }
                }
            }
        }

        return (!ways.isEmpty()) ? ways.stream().findAny().get() : new Stack<>();
    }

    @Override
    public Move onUpdate(Update update) {
        // умные вещи
        Offset moveOffset = new Offset(0, 0);

        Set<Point> blocks = update.getBlocks();
        Map<Integer, Point> bots = update.getBots();
        Map<Integer, Integer> botCoins = update.getBotCoins();
        Set<Point> coins = update.getCoins();

        Point myAbsolutePoint = bots.get(id);

        // безумные вещи
        updateMap(blocks, coins, bots, myAbsolutePoint);
        myMap = getMap();

        boolean attackAndDefeat;
        boolean attackAndWin;
        boolean stuck = amountOfSleepingSteps >= 3;
        boolean goGoZeppeli;
        boolean nothingElseMatters;

        var valueAttackAndDefeat = handleEnemy(bots, botCoins, myAbsolutePoint, false);
        attackAndDefeat = valueAttackAndDefeat.getKey();

        var valueAttackAndWin = handleEnemy(bots, botCoins, myAbsolutePoint, true);
        attackAndWin = valueAttackAndWin.getKey();

        var steelBallRun = ifMoney(coins, myAbsolutePoint);
        goGoZeppeli = steelBallRun.getKey();
        if (sbrStack.isEmpty()) sbrStack = steelBallRun.getValue();

        nothingElseMatters = ifNothing(bots, coins);

        // TODO:
        // логика
        if (stuck) {
            if (stuckList.isEmpty()) {
                stuckList = createInsaneAntiDancingOffset(myAbsolutePoint);
            }

            if (!stuckList.isEmpty()) {
                moveOffset = stuckList.pop();
            }
            else {
                moveOffset = new Offset(0,0);
            }

            if (rnd.nextInt(1000) > 888) {
                stuckList.clear();
            }
        }
        if (getMode() == MatchMode.DEATHMATCH && attackAndDefeat) {
            moveOffset = valueAttackAndDefeat.getValue();
        } else if (getMode() == MatchMode.DEATHMATCH && attackAndWin) {
            moveOffset = valueAttackAndWin.getValue();
        } else if (!sbrStack.isEmpty()){// || goGoZeppeli) {
            moveOffset = sbrStack.pop();
            if (rnd.nextInt(1000) > 888) {
                sbrStack.clear();
            }
        } else {//if (amountOfStepsInTact >= 7 || nothingElseMatters || !stuckList.isEmpty()){//stuck || nothingElseMatters) {
            // TODO:
            // дрейф

            amountOfStepsInTact = 0;

            if (stuckList.isEmpty()) {
                stuckList = createInsaneAntiDancingOffset(myAbsolutePoint);
            }

            if (!stuckList.isEmpty()) {
                moveOffset = stuckList.pop();
            }
            else {
                moveOffset = new Offset(0,0);
            }

            if (rnd.nextInt(1000) > 888) {
                stuckList.clear();
            }

        }

        if (lastAbsolutePoint.x() == myAbsolutePoint.x() &&
                lastAbsolutePoint.y() == myAbsolutePoint.y()
        ) {
            amountOfSleepingSteps += 1;
        } else {
            amountOfSleepingSteps = 0;
            lastAbsolutePoint = new Point(myAbsolutePoint.x(), myAbsolutePoint.y());
        }

        amountOfStepsInTact += 1;

        Move move = new Move();
        move.setOffset(moveOffset);
        return move;
    }

    @Override
    public void onMatchOver(MatchOver matchOver) {
        resetMap();
    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(300_000);

        Map<String, String> propertiesTmp = PropertiesReader.readProperties(args[0]);

        socket.connect(
                new InetSocketAddress(
                        propertiesTmp.get("server.host"),
                        Integer.parseInt(propertiesTmp.get("server.port"))
                )
        );

        SocketSession session = new SocketSession(socket);

        StarterBot bot = new StarterBot(
                propertiesTmp
        );

        new BotMatchRunner(bot, session).run();
    }
}
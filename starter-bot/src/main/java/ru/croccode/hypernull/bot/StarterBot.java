package ru.croccode.hypernull.bot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

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
import static ru.croccode.hypernull.wavePropagation.WavePropagation.findShortestPathLength;

enum Direction {
    UP(0),
    RIGHT(1),
    DOWN(2),
    LEFT(3);

    public final int value;

    public static Direction getByNum(int num) {
        switch (num) {
            case 0:
                return UP;
            case 1:
                return RIGHT;
            case 2:
                return DOWN;
            case 3:
                return LEFT;
        }

        return null;
    }

    Direction(int value) {
        this.value = value;
    }
}

class SmartDirection {
    private final Direction direction;

    public SmartDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction rotateRight() {
        switch (direction) {
            case UP:
                return Direction.RIGHT;
            case RIGHT:
                return Direction.DOWN;
            case DOWN:
                return Direction.LEFT;
            case LEFT:
                return Direction.UP;
        }

        return null;
    }

    public Direction rotateLeft() {
        switch (direction) {
            case UP:
                return Direction.LEFT;
            case RIGHT:
                return Direction.UP;
            case DOWN:
                return Direction.RIGHT;
            case LEFT:
                return Direction.DOWN;
        }

        return null;
    }

    public Direction getDirection() {
        return direction;
    }
}

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
        System.out.println("HELLO");
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

    private Pair<Boolean, Offset> getOptimalGoToTargetOffsetByWavePropagation(Point myAbsolutePoint,
                                                                              Point targetAbsolutePoint) {
        int[][] myMapCopy = createMyMapIntCopyWithoutCoins();

        Point toRelatedPoint = getRelatedPoint(myAbsolutePoint, targetAbsolutePoint);
        Point myRelatedPoint = getRelatedPoint(myAbsolutePoint, myAbsolutePoint);

        myMapCopy[toRelatedPoint.y()][toRelatedPoint.x()] = 0; // delete war bot from map

        var ans = findShortestPathLength(myMapCopy, myRelatedPoint.y(), myRelatedPoint.x(), toRelatedPoint.y(), toRelatedPoint.x());

        if (ans.getKey() != Integer.MAX_VALUE && ans.getValue().size() > 1) {
            Offset moveOffset = ans.getValue().get(1);
            return new Pair<>(true, moveOffset);
        }
        else {
            return new Pair<>(false, new Offset(0, 0));
        }
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
            return (goWar) ? getOptimalGoToTargetOffsetByWavePropagation(myAbsolutePoint, enemyP) : // new Pair<>(true, goToTargetStupidOffset(myAbsolutePoint, enemyP)) :
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

        boolean ansB = false;
        Stack<Offset> ansS = new Stack<>();

        int minLength = 999;

        if (absoluteCoins != null) {
            for (var coinAbsolutePoint :
                    absoluteCoins) {
                int[][] myMapCopy = createMyMapIntCopyWithoutCoins();

                Point toRelatedPoint = getRelatedPoint(myAbsolutePoint, coinAbsolutePoint);
                Point myRelatedPoint = getRelatedPoint(myAbsolutePoint, myAbsolutePoint);

                //WavePropagation wavePropagation = new WavePropagation(myRelatedPoint, toRelatedPoint, myMapCopy);
                var ans = findShortestPathLength(myMapCopy, myRelatedPoint.y(), myRelatedPoint.x(), toRelatedPoint.y(), toRelatedPoint.x());

                if (ans.getKey() != Integer.MAX_VALUE && ans.getValue().size() < minLength) {
                    ansB = true;
                    minLength = ans.getValue().size();
                    Stack<Offset> tmp = new Stack<>();
                    for(int i = ans.getValue().size() - 1; i >= 0; i--){
                        tmp.push(ans.getValue().get(i));
                    }
                    ansS = tmp;
                }
            }
        }

        if (ansB) {
            return new Pair<>(true, ansS);
        } else {
            return new Pair<>(false, ansS); // ansS -> new Stack<>();
        }

    }

    private boolean ifNothing(Map<Integer, Point> bots, Set<Point> coins) {
        return bots.size() <= 1 && coins == null;
    }

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

/*    private Stack<Offset> createInsaneAntiDancingOffset(Point myAbsolutePoint) {
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
    }*/

    private Point lastAbsolutePoint = new Point(0, 0);
    private int amountOfSleepingSteps = 0;

    private int amountOfStepsInTact = 0;

    private Stack<Offset> stuckList = new Stack<>();
    private Stack<Offset> sbrStack = new Stack<>();

    private SmartDirection smartDirection = new SmartDirection(Direction.UP);

    private Offset getOffsetFromAbsDir(Direction absoluteDirection) {
        switch (absoluteDirection)  {
            case UP:
                return new Offset(0, 1);
            case RIGHT:
                return new Offset(1, 0);
            case DOWN:
                return new Offset(0, -1);
            case LEFT:
                return new Offset(-1, 0);
            default:
                return null;
        }
    }

    private Direction getAbsDirFromRelDir(Direction relatedDirection)   {
        SmartDirection smartDirectionCopy = new SmartDirection(smartDirection.getDirection()); // голова

        Map<Direction, Direction> mapRelatedDirToAbsoluteDir = new HashMap<>();

        for(int i = 1; i <= 4; i++) { // right: 1,
            mapRelatedDirToAbsoluteDir.put(
                    Direction.getByNum(i % 4),
                    smartDirectionCopy.rotateRight()
            );
            smartDirectionCopy = new SmartDirection(smartDirectionCopy.rotateRight());
        }

        return mapRelatedDirToAbsoluteDir.get(relatedDirection);
    }

/*    public static void main(String[] args) {
        StarterBot bot = new StarterBot(null);
        bot.smartDirection = new SmartDirection(Direction.DOWN);
        System.out.println(bot.getAbsDirFromRelDir(Direction.UP)); // LEFT
        System.out.println(bot.getAbsDirFromRelDir(Direction.RIGHT)); // UP
        System.out.println(bot.getAbsDirFromRelDir(Direction.DOWN)); // RIGHT
        System.out.println(bot.getAbsDirFromRelDir(Direction.LEFT)); // DOWN
    }*/

    private boolean isBlockedByRelatedPointAndRelatedDirection(Point relatedPoint, Direction relatedDirection) { // чел; сторона относительно башки
        // smartDirection = left
        // relatedDirection = right
        // return UP
        Direction absoluteDirection = getAbsDirFromRelDir(relatedDirection);

        Point relatedPointFromAbsoluteDirection;
        switch (absoluteDirection)  {
            case UP:
                relatedPointFromAbsoluteDirection = new Point(relatedPoint.x(), relatedPoint.y() - 1);
                return isBlocked(relatedPointFromAbsoluteDirection);
            case RIGHT:
                relatedPointFromAbsoluteDirection = new Point(relatedPoint.x() + 1, relatedPoint.y());
                return isBlocked(relatedPointFromAbsoluteDirection);
            case DOWN:
                relatedPointFromAbsoluteDirection = new Point(relatedPoint.x(), relatedPoint.y() + 1);
                return isBlocked(relatedPointFromAbsoluteDirection);
            case LEFT:
                relatedPointFromAbsoluteDirection = new Point(relatedPoint.x() - 1, relatedPoint.y());
                return isBlocked(relatedPointFromAbsoluteDirection);
        }
        return false;
    }

    private Offset getOffsetFromRelDir(Direction relatedDirection)  {
        Direction absoluteDirection = getAbsDirFromRelDir(relatedDirection);
        return getOffsetFromAbsDir(absoluteDirection);
    }

    private Offset labyrinthOffset(Point myAbsolutePoint) {
        Point myRelatedPoint = getRelatedPoint(myAbsolutePoint, myAbsolutePoint);

        while (true) {
            boolean isNeighbour = false;

            for(int i = 0; i < 4; i++){
                isNeighbour |= isBlockedByRelatedPointAndRelatedDirection(myRelatedPoint, Direction.getByNum(i));
            }

            if (isNeighbour) {
                // полезное
                if (isBlockedByRelatedPointAndRelatedDirection(myRelatedPoint, Direction.RIGHT)) {
                    if (isBlockedByRelatedPointAndRelatedDirection(myRelatedPoint, Direction.UP)) {
                        smartDirection = new SmartDirection(smartDirection.rotateLeft()); // ПОЧЕМУ НЕ РАБОТАЕТ???
                    } else {
                        //return getOffsetFromRelDir(Direction.UP);
                        return getOffsetFromAbsDir(smartDirection.getDirection());
                    }
                } else {
                    smartDirection = new SmartDirection(smartDirection.rotateRight());
                    //return getOffsetFromRelDir(Direction.UP);
                    return getOffsetFromAbsDir(smartDirection.getDirection());
                }
            } else {
                Direction direction = smartDirection.getDirection();
                return getOffsetFromAbsDir(direction);
            }
        }
    }
/*    @Override
    public Move onUpdate(Update update) {
        System.out.println("UPDATE");
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
    }*/

    // есть монетка -> найти полный путь и до конца идти
    // если нет монеток или застряли -> "правая рука"
    @Override
    public Move onUpdate(Update update) {
        System.out.println("UPDATE");

        Offset moveOffset = new Offset(0, 0);

        Set<Point> blocks = update.getBlocks();
        Map<Integer, Point> bots = update.getBots();
        Map<Integer, Integer> botCoins = update.getBotCoins();
        Set<Point> coins = update.getCoins();

        Point myAbsolutePoint = bots.get(id);

        updateMap(blocks, coins, bots, myAbsolutePoint);

        myMap = getMap();

        boolean attackAndDefeat;
        boolean attackAndWin;

        var valueAttackAndDefeat = handleEnemy(bots, botCoins, myAbsolutePoint, false);
        attackAndDefeat = valueAttackAndDefeat.getKey();

        var valueAttackAndWin = handleEnemy(bots, botCoins, myAbsolutePoint, true);
        attackAndWin = valueAttackAndWin.getKey();

        boolean goGoZeppeli;

        if (sbrStack.isEmpty()) {
            var steelBallRun = ifMoney(coins, myAbsolutePoint);
            goGoZeppeli = steelBallRun.getKey();
            sbrStack = steelBallRun.getValue();
        }
        else {
            goGoZeppeli = true;
        }

        // логика
        if (getMode() == MatchMode.DEATHMATCH && attackAndDefeat && true) {  // SWITCH ME PLEASE
            moveOffset = valueAttackAndDefeat.getValue();
        } else if (getMode() == MatchMode.DEATHMATCH && attackAndWin && true) {  // SWITCH ME PLEASE
            moveOffset = valueAttackAndWin.getValue(); // говно переделывай
        } else if (goGoZeppeli && true) {  // SWITCH ME PLEASE
            // монетка
            moveOffset = sbrStack.pop();
            if (moveOffset.dx() == 0 && moveOffset.dy() == 0 && !sbrStack.isEmpty()) moveOffset = sbrStack.pop();
            moveOffset = new Offset(moveOffset.dx(), moveOffset.dy()); // вроде необязательно

            if (rnd.nextInt(1000) > 888) {  // SWITCH ME PLEASE
                sbrStack.clear();
            }
        }
        else if (true){ // SWITCH ME PLEASE
            // лабиринт
            moveOffset = labyrinthOffset(myAbsolutePoint);
        }

        Move move = new Move();
        move.setOffset(moveOffset);
        return move;
    }

    @Override
    public void onMatchOver(MatchOver matchOver) {
        System.out.println("OVER");
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

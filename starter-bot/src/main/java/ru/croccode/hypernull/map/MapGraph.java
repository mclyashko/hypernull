package ru.croccode.hypernull.map;

import ru.croccode.hypernull.geometry.Point;

import java.util.*;

public class MapGraph {
    private static char[][] map = null;
    private static int width = 0,  height = 0, radius = 0;

    public static void initMap(int radius, int width, int height) {
        map = new char[2 * radius + 1][2 * radius + 1];

        clearMap();

        MapGraph.width = width;
        MapGraph.height = height;
        MapGraph.radius = radius;
    }

    public static void resetMap()   {
        width = 0;
        height = 0;
        radius = 0;
        map = null;
    }

    private static void clearMap(char[][] map) {
        for (char[] ints : map) {
            Arrays.fill(ints, '0');
        }
    }

    private static void clearMap() {
        clearMap(map);
    }

    public static Point getRelatedPoint(int myAbsoluteX, int myAbsoluteY, int targetAbsoluteX, int targetAbsoluteY) {
        // 3 2! 4 4? r = 3
        // 4 1
        // 0 0 0 # 0 0 0
        // 0 0 0 # 0 0 0
        // 0 0 # 0 ? 0 0
        // 0 # 0 0 0 # 0
        // # 0 0 ! 0 0 #
        // 0 # 0 0 0 # 0
        // 0 0 # 0 # 0 0

        int xCoordinateTransition;
        int yCoordinateTransition;

        int straightOffsetX = Math.abs(targetAbsoluteX - myAbsoluteX);
        int transitionOffsetX = MapGraph.width - Math.abs(targetAbsoluteX - myAbsoluteX);

        xCoordinateTransition = straightOffsetX < transitionOffsetX ? 1 : -1;

        int offsetX = xCoordinateTransition * Math.min(
                Math.abs(targetAbsoluteX - myAbsoluteX),
                MapGraph.width - Math.abs(targetAbsoluteX - myAbsoluteX)
        );

        int straightOffsetY = Math.abs(targetAbsoluteY - myAbsoluteY);
        int transitionOffsetY = MapGraph.height - Math.abs(targetAbsoluteY - myAbsoluteY);

        yCoordinateTransition = straightOffsetY < transitionOffsetY ? 1 : -1;

        int offsetY = yCoordinateTransition * Math.min(
                Math.abs(targetAbsoluteY - myAbsoluteY),
                MapGraph.height - Math.abs(targetAbsoluteY - myAbsoluteY)
        );

        int newX = targetAbsoluteX > myAbsoluteX ?
                offsetX + MapGraph.radius : -offsetX + MapGraph.radius;
        int newY = targetAbsoluteY > myAbsoluteY ?
                offsetY + MapGraph.radius : -offsetY + MapGraph.radius;

        newY = map.length - newY - 1;

        return new Point(newX, newY);
    }

    public static Point getRelatedPoint(Point myAbsolutePoint, Point targetAbsolutePoint) {
        return getRelatedPoint(myAbsolutePoint.x(), myAbsolutePoint.y(),
                targetAbsolutePoint.x(), targetAbsolutePoint.y());
    }

    private static void addSomething(Set<Point> absoluteTargetPoints, Point myAbsolutePoint, char colour) {
        for (Point absoluteTargetPoint: absoluteTargetPoints)   {

            Point myRelatedPoint = getRelatedPoint(myAbsolutePoint, absoluteTargetPoint);

            map[myRelatedPoint.y()][myRelatedPoint.x()] = colour;
        }
    }

    public static boolean isBlocked(Point relatedPoint) {  // надо протестировать
        return map[relatedPoint.y()][relatedPoint.x()] == '#';
    }

    public static int getSquaredDistance(int meX, int meY, int otherX, int otherY) {
        var x = meX - otherX;
        var y = meY - otherY;

        return x * x + y * y;
    }

    public static int getSquaredDistance(Point me, Point other) {
        return getSquaredDistance(me.x(), me.y(), other.x(), other.y());
    }

    private static boolean inScope(int meX, int meY, int otherX, int otherY) {
        return getSquaredDistance(meX, meY, otherX, otherY) <= radius * radius;
    }

    private static boolean inScope(Point me, Point other) {
        return inScope(me.x(), me.y(), other.y(), other.y());
    }

    private static void scopeMap(char[][] map) {
        // r = 1
        // 0 0 0 0 0
        // 0 0 0 0 0
        // 0 0 0 0 0
        // 0 0 0 0 0
        // 0 0 0 0 0
        // -> to
        // # # # # #
        // # # 0 # #
        // # 0 0 0 #
        // # # 0 # #
        // # # # # #

        int centerX = map.length / 2;
        int centerY = map[0].length / 2;
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (!inScope(j, i, centerX, centerY)) {
                    map[i][j] = '#';
                }
            }
        }
    }

    private static void scopeMap() {
        scopeMap(map);
    }

    public static void updateMap(Set<Point> absoluteBlocks, Set<Point> absoluteCoins, Map<Integer, Point> absoluteBots,
                                 Point myAbsolutePoint) {
        clearMap();

        scopeMap();
        if (absoluteBlocks != null) addSomething(absoluteBlocks, myAbsolutePoint, '#');
        if (absoluteBots != null) addSomething(new HashSet<>(absoluteBots.values()), myAbsolutePoint, '#');
        if (absoluteCoins != null) addSomething(absoluteCoins, myAbsolutePoint, '$');

        //printMap();
    }

    public static char[][] getMap() {
        return map;
    }

    public static void printMap()   {
        System.out.println("Map:");
        for (char[] row : map)  {
            System.out.println(Arrays.toString(row));
        }
    }
}

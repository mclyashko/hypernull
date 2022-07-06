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

    private static void clearMap() {
        for (char[] ints : map) {
            Arrays.fill(ints, '0');
        }
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

        int offsetX = Math.min(
                Math.abs(targetAbsoluteX - myAbsoluteX),
                MapGraph.width - Math.abs(targetAbsoluteX - myAbsoluteX)
        );

        int offsetY = Math.min(
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
        //return map[map.length - y - 1][x] == '#';
        return map[relatedPoint.y()][relatedPoint.x()] == '#';
    }

    public static void updateMap(Set<Point> absoluteBlocks, Set<Point> absoluteCoins, Map<Integer, Point> absoluteBots,
                                 Point myAbsolutePoint) {
        clearMap();

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


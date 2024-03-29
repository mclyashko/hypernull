package ru.croccode.hypernull.wavePropagation;

import javafx.util.Pair;
import ru.croccode.hypernull.geometry.Offset;

import java.util.*;

class Node {
    private int y, x, dist;
    private List<Offset> way;

    Node(int y, int x, int dist, List<Offset> way) {
        this.y = y;
        this.x = x;
        this.dist = dist;
        this.way = way;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getDist() {
        return dist;
    }

    public void setDist(int dist) {
        this.dist = dist;
    }

    public List<Offset> getWay() {
        return way;
    }

    public void setWay(List<Offset> way) {
        this.way = way;
    }
}

public class WavePropagation {
    private static final int[] row = {-1, 0, 0, 1, -1, -1, 1, 1};
    private static final int[] col = {0, -1, 1, 0, -1, 1, -1, 1};

    private static boolean isValid(int[][] map, boolean[][] visited, int row, int col) {
        return (row >= 0) && (row < map.length) && (col >= 0) && (col < map[0].length)
                && map[row][col] == 0 && !visited[row][col];
    }

    public static Pair<Integer, List<Offset>> findShortestPathLength(int[][] map, int startY, int startX, int toY, int toX) {
        map[startY][startX] = 0;

        // Матрица `M × N`
        int M = map.length;
        int N = map[0].length;

        // создаем матрицу для отслеживания посещенных ячеек
        boolean[][] visited = new boolean[M][N];

        // создаем пустую queue
        Queue<Node> q = new ArrayDeque<>();

        // помечаем исходную ячейку как посещенную и ставим исходный узел в queue
        visited[startY][startX] = true;
        List<Offset> init = new ArrayList<>();
        init.add(new Offset(0, 0));
        q.add(new Node(startY, startX, 0, init));

        // сохраняет длину самого длинного пути от источника к месту назначения
        Pair<Integer, List<Offset>> ans = new Pair<>(Integer.MAX_VALUE, null);

        // цикл до тех пор, пока queue не станет пустой
        while (!q.isEmpty()) {
            // удалить передний узел из очереди и обработать его
            Node node = q.poll();

            // (startY, startX) представляет текущую ячейку, а `dist` хранит ее
            // минимальное расстояние от источника
            startY = node.getY();
            startX = node.getX();
            int dist = node.getDist();

            // если пункт назначения найден, обновляем `minDist` и останавливаемся
            if (startY == toY && startX == toX) {
                ans = new Pair<>(dist, node.getWay());
                break;
            }

            // проверяем все четыре возможных перемещения из текущей ячейки
            // и ставим в queue каждое допустимое движение
            for (int k = 0; k < 8; k++) {
                // проверяем, можно ли выйти на позицию
                // (startY + row[k], startX + col[k]) от текущей позиции
                if (isValid(map, visited, startY + row[k], startX + col[k])) {
                    // отметить следующую ячейку как посещенную и поставить ее в queue
                    visited[startY + row[k]][startX + col[k]] = true;
                    var way = new ArrayList<>(node.getWay());  // заставляет задуматься
                    way.add(new Offset(
                            col[k], -row[k]     // заставляет задуматься
                    ));
                    q.add(
                            new Node(
                                    startY + row[k], startX + col[k], dist + 1, way
                            )
                    );
                }
            }
        }

        return ans;
    }
}

//public class WavePropagation {
//    private final int[][] map;
//
//    // в конце - первый шаг
//    private List<Pair<Integer, Integer>> path;
//
//    private final int width, height;
//
//    public boolean pathFound;
//
//    public int lengthPath;
//
//    private int _step;
//    private boolean _finishingCellMarked;
//    private int _finishPointI;
//    private int _finishPointJ;
//
//    private final int startI, startJ;
//
//    public WavePropagation(Point startRelatedPoint, Point toRelatedPoint, int[][] myMap) {
//        map = myMap;
//        width = myMap[0].length;
//        height = myMap.length;
//        startI = startRelatedPoint.y();
//        startJ = startRelatedPoint.x();
//        setStarCell(startI, startJ);
//        pathFound = pathSearch(toRelatedPoint.y(), toRelatedPoint.x());
//    }
//
//    private void setStarCell(int startI, int startJ) {
//        // Пометить стартовую ячейку d := 0
//        _step = 1;
//        map[startI][startJ] = _step;
//    }
//
//    private boolean pathSearch(int toI, int toJ) {
//        if (wavePropagation(toI, toJ))
//            return restorePath();
//
//        return false;
//    }
//
//    // Есть. Ортогонально-диагональный путь
//    // Распространение волны
//    private boolean wavePropagation(int toI, int toJ) {
//        // ЦИКЛ
//        //   ДЛЯ каждой ячейки loc, помеченной числом d
//        //     пометить все соседние свободные непомеченные ячейки числом d + 1
//        // КЦ
//        // d := d + 1
//        // ПОКА(финишная ячейка не помечена) И(есть возможность распространения волны)
//
//        int w = width;
//        int h = height;
//
//        boolean finished = false;
//        do {
//            for (int i = 0; i < h; i++) {
//                for (int j = 0; j < w; j++) {
//                    if (map[i][j] == _step) {
//                        // Пометить все соседние свободные непомеченные ячейки числом d + 1
//                        if (i != h - 1 && map[i + 1][j] == 0) {       // down
//                            map[i + 1][j] = _step + 1;
//                        }
//                        if (j != w - 1 && map[i][j + 1] == 0) {       // right
//                            map[i][j + 1] = _step + 1;
//                        }
//                        if (i != 0 && map[i - 1][j] == 0) {           // up
//                            map[i - 1][j] = _step + 1;
//                        }
//                        if (j != 0 && map[i][j - 1] == 0) {           // left
//                            map[i][j - 1] = _step + 1;
//                        }
//
//                        // диагонали
//                        if (i != h - 1 && j != w - 1 && map[i + 1][j + 1] == 0) {   // down-right
//                            map[i + 1][j + 1] = _step + 1;
//                        }
//                        if (i != h - 1 && j != 0 && map[i + 1][j - 1] == 0) {       // down-left
//                            map[i + 1][j - 1] = _step + 1;
//                        }
//                        if (i != 0 && j != w - 1 && map[i - 1][j + 1] == 0) {       // up-right
//                            map[i - 1][j + 1] = _step + 1;
//                        }
//                        if (i != 0 && j != 0 && map[i - 1][j - 1] == 0) {           // up-left
//                            map[i - 1][j - 1] = _step + 1;
//                        }
//
//                        if (i != h - 1 && i + 1 == toI && j == toJ) {                 // ok down
//                            _finishPointI = i + 1;
//                            _finishPointJ = j;
//                            finished = true;
//                        }
//
//                        if (j != w - 1 && i == toI && j + 1 == toJ) {                 // ok right
//                            _finishPointI = i;
//                            _finishPointJ = j + 1;
//                            finished = true;
//                        }
//
//                        if (i != 0 && i - 1 == toI && j == toJ) {                     // ok up
//                            _finishPointI = i - 1;
//                            _finishPointJ = j;
//                            finished = true;
//                        }
//
//                        if (j != 0 && i == toI && j - 1 == toJ) {                     // ok left
//                            _finishPointI = i;
//                            _finishPointJ = j - 1;
//                            finished = true;
//                        }
//
//                        // диагонали
//                        if (i != h - 1 && j != w - 1 && i + 1 == toI && j + 1 == toJ) {   // ok down-right
//                            _finishPointI = i + 1;
//                            _finishPointJ = j + 1;
//                            finished = true;
//                        }
//
//                        if (i != h - 1 && j != 0 && i + 1 == toI && j - 1 == toJ) {       // ok down-left
//                            _finishPointI = i + 1;
//                            _finishPointJ = j - 1;
//                            finished = true;
//                        }
//
//                        if (i != 0 && j != w - 1 && i - 1 == toI && j + 1 == toJ) {       // ok up-right
//                            _finishPointI = i - 1;
//                            _finishPointJ = j + 1;
//                            finished = true;
//                        }
//
//                        if (i != 0 && j != 0 && i - 1 == toI && j - 1 == toJ) {           // ok up-left
//                            _finishPointI = i - 1;
//                            _finishPointJ = j - 1;
//                            finished = true;
//                        }
//                    }
//
//                }
//            }
//            _step++;
//            // ПОКА(финишная ячейка не помечена) И (есть возможность распространения волны)
//        } while (!finished && _step < w * h + 1); // +1 ???
//        _finishingCellMarked = finished;
//        return finished;
//    }
//
//    //  Восстановление пути
//    private boolean restorePath() {
//        // ЕСЛИ финишная ячейка помечена
//        // ТО
//        //   перейти в финишную ячейку
//        //   ЦИКЛ
//        //     выбрать среди соседних ячейку, помеченную числом на 1 меньше числа в текущей ячейке
//        //     перейти в выбранную ячейку и добавить её к пути
//        //   ПОКА текущая ячейка — не стартовая
//        //   ВОЗВРАТ путь найден
//        // ИНАЧЕ
//        //   ВОЗВРАТ путь не найден
//        if (!_finishingCellMarked)
//            return false;
//
//        int h = height;
//        int w = width;
//
//        int i = _finishPointI;
//        int j = _finishPointJ;
//        path = new ArrayList<>();
//        addToPath(i, j);
//
//        do {
//            if (i != height - 1 && map[i + 1][j] == _step - 1) {   // down
//                i += 1;
//                addToPath(i, j);
//            }
//            if (j != width - 1 && map[i][j + 1] == _step - 1) {   // right
//                j += 1;
//                addToPath(i, j);
//            }
//            if (i != 0 && map[i - 1][j] == _step - 1) {       // up
//                i -= 1;
//                addToPath(i, j);
//            }
//            if (j != 0 && map[i][j - 1] == _step - 1) {       // left
//                j -= 1;
//                addToPath(i, j);
//            }
//
//            // диагонали:
//            if (i != h - 1 && j != w - 1 && map[i + 1][j + 1] == _step - 1) {   // down-right
//                i += 1;
//                j += 1;
//                addToPath(i, j);
//            }
//            if (i != h - 1 && j != 0 && map[i + 1][j - 1] == _step - 1) {       // down-left
//                i += 1;
//                j -= 1;
//                addToPath(i, j);
//            }
//            if (i != 0 && j != w - 1 && map[i - 1][j + 1] == _step - 1) {       // up-right
//                i -= 1;
//                j += 1;
//                addToPath(i, j);
//            }
//            if (i != 0 && j != 0 && map[i - 1][j - 1] == _step - 1) {           // up-left
//                i -= 1;
//                j -= 1;
//                addToPath(i, j);
//            }
//
//            _step--;
//        } while (_step != 1); // ???
//        return true;
//    }
//
//    private void addToPath(int i, int j) {
//        path.add(new Pair<>(i, j));
//    }
//
//    public Offset getFirstStep() {
//        if (pathFound) {
//            var cPair = path.get(path.size() - 2); // ?????
//            int cPairI = cPair.getKey();
//            int cPairJ = cPair.getValue();
//
//            // [0][2] <- p   zeroTwo
//            // [1][1] <- !
//            // NEED: Offset(1, 1)
//            // WHAT WE NEED: (p.xAbs() - !.xAbs(), p.yAbs() - !.yAbs())
//            // HOW TO GET: (p.xRel() - !.xRel(), !.yRel() - p.yRel())
//            // 0 0 p
//            // 0 ! 0
//            // 0 0 0
//            int offsetY = startI - cPairI;
//            int offsetX = cPairJ - startJ;
//
//            return new Offset(offsetX, offsetY);
//        }
//
//        return new Offset(0, 0);
//    }
//
//    // должен вернуть стек со всеми шагами такой, что первый элемент стека - первый шаг
//    public Stack<Offset> getAllSteps() {
//        Stack<Offset> returnStack = new Stack<>();
//
//        var nextPosition = path.get(0);
//        for(int i = 1; i < path.size(); i++) {
//            var prevPosition = path.get(i);
//
//            // offset prevPosition -> nextPosition
//            int offsetY = prevPosition.getKey() - nextPosition.getKey();
//            int offsetX = nextPosition.getValue() - prevPosition.getValue();
//
//            returnStack.push(new Offset(offsetX, offsetY));
//
//            nextPosition = prevPosition;
//        }
//
//        return returnStack;
//    }
//}

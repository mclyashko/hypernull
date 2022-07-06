package ru.croccode.hypernull.wavePropagation;

import javafx.util.Pair;
import ru.croccode.hypernull.geometry.Offset;
import ru.croccode.hypernull.geometry.Point;

import java.util.ArrayList;
import java.util.List;

public class WavePropagation {
    private final int[][] map;

    // в конце - первый шаг
    private List<Pair<Integer, Integer>> path;

    private final int width, height;

    public boolean pathFound;

    public int lengthPath;

    private int _step;
    private boolean _finishingCellMarked;
    private int _finishPointI;
    private int _finishPointJ;

    private final int startI, startJ;

    public WavePropagation(Point startRelatedPoint, Point toRelatedPoint, int[][] myMap) {
        map = myMap;
        width = myMap[0].length;
        height = myMap.length;
        startI = startRelatedPoint.y();
        startJ = startRelatedPoint.x();
        setStarCell(startI, startJ);
        pathFound = pathSearch(toRelatedPoint.y(), toRelatedPoint.x());
    }

    private void setStarCell(int startI, int startJ) {
        // Пометить стартовую ячейку d := 0
        _step = 1;
        map[startI][startJ] = _step;
    }

    private boolean pathSearch(int toI, int toJ) {
        if (wavePropagation(toI, toJ))
            return restorePath();

        return false;
    }

    // Есть. Ортогонально-диагональный путь
    // Распространение волны
    private boolean wavePropagation(int toI, int toJ) {
        // ЦИКЛ
        //   ДЛЯ каждой ячейки loc, помеченной числом d
        //     пометить все соседние свободные непомеченные ячейки числом d + 1
        // КЦ
        // d := d + 1
        // ПОКА(финишная ячейка не помечена) И(есть возможность распространения волны)

        int w = width;
        int h = height;

        boolean finished = false;
        do {
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    if (map[i][j] == _step) {
                        // Пометить все соседние свободные непомеченные ячейки числом d + 1
                        if (i != h - 1 && map[i + 1][j] == 0) {       // down
                            map[i + 1][j] = _step + 1;
                        }
                        if (j != w - 1 && map[i][j + 1] == 0) {       // right
                            map[i][j + 1] = _step + 1;
                        }
                        if (i != 0 && map[i - 1][j] == 0) {           // up
                            map[i - 1][j] = _step + 1;
                        }
                        if (j != 0 && map[i][j - 1] == 0) {           // left
                            map[i][j - 1] = _step + 1;
                        }

                        // TODO:
                        // диагонали
//                        // диагонали
//                        if (i != h - 1 && j != w - 1 && map[i + 1][j + 1] != 999) {   // down-right
//                            map[i + 1][j + 1] = _step + 1;
//                        }
//                        if (i != h - 1 && j != 0 && map[i + 1][j - 1] != 999) {       // down-left
//                            map[i + 1][j - 1] = _step + 1;
//                        }
//                        if (i != 0 && j != w - 1 && map[i - 1][j + 1] != 999) {       // up-right
//                            map[i - 1][j + 1] = _step + 1;
//                        }
//                        if (i != 0 && j != 0 && map[i - 1][j - 1] != 999) {           // up-left
//                            map[i - 1][j - 1] = _step + 1;
//                        }

                        if (i != h - 1 && i + 1 == toI && j == toJ) {                 // ok down
                            _finishPointI = i + 1;
                            _finishPointJ = j;
                            finished = true;
                        }

                        if (j != w - 1 && i == toI && j + 1 == toJ) {                 // ok right
                            _finishPointI = i;
                            _finishPointJ = j + 1;
                            finished = true;
                        }

                        if (i != 0 && i - 1 == toI && j == toJ) {                     // ok up
                            _finishPointI = i - 1;
                            _finishPointJ = j;
                            finished = true;
                        }

                        if (j != 0 && i == toI && j - 1 == toJ) {                     // ok left
                            _finishPointI = i;
                            _finishPointJ = j - 1;
                            finished = true;
                        }
                        // TODO:
                        // диагонали
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
                    }

                }
            }
            _step++;
            // ПОКА(финишная ячейка не помечена) И (есть возможность распространения волны)
        } while (!finished && _step < w * h + 1); // +1 ???
        _finishingCellMarked = finished;
        return finished;
    }

    //  Восстановление пути
    private boolean restorePath() {
        // ЕСЛИ финишная ячейка помечена
        // ТО
        //   перейти в финишную ячейку
        //   ЦИКЛ
        //     выбрать среди соседних ячейку, помеченную числом на 1 меньше числа в текущей ячейке
        //     перейти в выбранную ячейку и добавить её к пути
        //   ПОКА текущая ячейка — не стартовая
        //   ВОЗВРАТ путь найден
        // ИНАЧЕ
        //   ВОЗВРАТ путь не найден
        if (!_finishingCellMarked)
            return false;

        int i = _finishPointI;
        int j = _finishPointJ;
        path = new ArrayList<>();
        addToPath(i, j);

        do {
            if (i != height - 1 && map[i + 1][j] == _step - 1) {   // down
                i += 1;
                addToPath(i, j);
            }
            if (j != width - 1 && map[i][j + 1] == _step - 1) {   // right
                j += 1;
                addToPath(i, j);
            }
            if (i != 0 && map[i - 1][j] == _step - 1) {       // up
                i -= 1;
                addToPath(i, j);
            }
            if (j != 0 && map[i][j - 1] == _step - 1) {       // left
                j -= 1;
                addToPath(i, j);
            }
            // TODO:
            // диагонали
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

            _step--;
        } while (_step != 1); // ???
        return true;
    }

    private void addToPath(int i, int j) {
        path.add(new Pair<>(i, j));
    }

    public Offset getFirstStep() {
        if (pathFound) {
            var cPair = path.get(path.size() - 2); // ?????
            int cPairI = cPair.getKey();
            int cPairJ = cPair.getValue();

            // [0][2] <- p   zeroTwo
            // [1][1] <- !
            // NEED: Offset(1, 1)
            // WHAT WE NEED: (p.xAbs() - !.xAbs(), p.yAbs() - !.yAbs())
            // HOW TO GET: (p.xRel() - !.xRel(), !.yRel() - p.yRel())
            // 0 0 p
            // 0 ! 0
            // 0 0 0
            int offsetY = startI - cPairI;
            int offsetX = cPairJ - startJ;

            for (Pair<Integer, Integer> integerIntegerPair : path) {
                System.out.print(integerIntegerPair);
                System.out.print(" ");
            }
            System.out.println();
            System.out.println("-------------------------------------------------");

            return new Offset(offsetX, offsetY);
        }

        return new Offset(0, 0);
    }
}

//class Program {
//    static void Main(string[] args) {
//        const int heigth = 10;
//        const int width = 18;
//
//        int[,]my = new int[heigth, width];
//        for (int i = 0; i < heigth; i++) {
//            for (int j = 0; j < width; j++) {
//                var rand = new Random(unchecked((int) DateTime.Now.Millisecond));
//                if (rand.Next(100) > 70)
//                    my[i, j] =(int) Figures.Barrier;
//                else
//                my[i, j] =(int) Figures.EmptySpace;
//            }
//        }
//        var random = new Random(unchecked((int) DateTime.Now.Millisecond));
//        my[random.Next(heigth), random.Next(width)] =(int) Figures.StartPosition;
//        my[random.Next(heigth), random.Next(width)] =(int) Figures.Destination;
//        Print(my);
//
//        LeeAlgorithm li = new LeeAlgorithm(my);
//        Console.WriteLine(li.PathFound);
//        if (li.PathFound) {
//            foreach(var item in li.Path)
//            {
//                if (item == li.Path.Last())
//                    my[item.Item1, item.Item2] =(int) Figures.StartPosition;
//                else if (item == li.Path.First())
//                my[item.Item1, item.Item2] =(int) Figures.Destination;
//                else
//                my[item.Item1, item.Item2] =(int) Figures.Path;
//            }
//            Print(li.ArrayGraph);
//            Console.WriteLine("Длина " + li.LengthPath);
//        } else
//            Console.WriteLine("Путь не найден");
//        Console.ReadLine();
//
//    }
//}
